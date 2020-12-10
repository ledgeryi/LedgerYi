package cn.ledgeryi.framework.common.overlay.server;

import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.protos.Protocol;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import java.util.Queue;
import java.util.concurrent.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.overlay.message.PingMessage;
import cn.ledgeryi.framework.common.overlay.message.PongMessage;
import cn.ledgeryi.framework.core.net.message.InventoryMessage;
import cn.ledgeryi.framework.core.net.message.TransactionsMessage;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class MessageQueue {

  private final static ScheduledExecutorService sendTimer =
          Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "sendTimer"));

  private volatile boolean sendMsgFlag = false;
  private volatile long sendTime;
  private volatile long sendPing;
  private Thread sendMsgThread;
  private Channel channel;
  private ChannelHandlerContext ctx = null;
  private final Queue<MessageRoundtrip> requestQueue = new ConcurrentLinkedQueue<>();
  private final BlockingQueue<Message> msgQueue = new LinkedBlockingQueue<>();
  private ScheduledFuture<?> sendTask;


  public void activate(ChannelHandlerContext ctx) {
    this.ctx = ctx;
    sendMsgFlag = true;
    sendTask = sendTimer.scheduleAtFixedRate(() -> {
      try {
        if (sendMsgFlag) {
          send();
        }
      } catch (Exception e) {
        log.error("[MessageQueue] [sendMsgThread] Unhandled exception", e);
      }
    }, 10, 10, TimeUnit.MILLISECONDS);

    sendMsgThread = new Thread(() -> {
      while (sendMsgFlag) {
        try {
          if (msgQueue.isEmpty()) {
            Thread.sleep(10);
            continue;
          }
          // the type of msg is TX , BLOCK , PING or PONG
          Message msg = msgQueue.take();
          ctx.writeAndFlush(msg.getSendData()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess() && !channel.isDisconnect()) {
              log.error("Fail send to {}, {}", ctx.channel().remoteAddress(), msg);
            }
          });
          log.debug("[MessageQueue] [sendMsgThread] Send to {}, {} ", ctx.channel().remoteAddress(), msg);
        } catch (Exception e) {
          log.error("[MessageQueue] [sendMsgThread] Fail send to {}, error info: {}", ctx.channel().remoteAddress(), e.getMessage());
        }
      }
    });
    sendMsgThread.setName("sendMsgThread-TX-BLOCK-PING-PONG" + ctx.channel().remoteAddress());
    sendMsgThread.start();
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public boolean sendMessage(Message msg) {
    long now = System.currentTimeMillis();
    if (msg instanceof PingMessage) {
      if (now - sendTime < 10_000 && now - sendPing < 60_000) {
        return false;
      }
      sendPing = now;
    }

    log.debug("[MessageQueue] [sendMessage] Send to msgQueue {}, {} ", ctx.channel().remoteAddress(), msg);
    channel.getNodeStatistics().messageStatistics.addTcpOutMessage(msg);
    sendTime = System.currentTimeMillis();
    if (msg.getAnswerMessage() != null) {
      requestQueue.add(new MessageRoundtrip(msg));
    } else {
      msgQueue.offer(msg);
    }
    return true;
  }

  public void receivedMessage(Message msg) {
    log.debug("[MessageQueue] [receivedMessage] Receive from {}, {}, ID:{}", ctx.channel().remoteAddress(), msg, msg.getMessageId().toString());
    channel.getNodeStatistics().messageStatistics.addTcpInMessage(msg);
    MessageRoundtrip rt = requestQueue.peek();
    if (rt != null && rt.getMsg().getAnswerMessage() == msg.getClass()) {
      requestQueue.remove();
      if (rt.getMsg() instanceof PingMessage) {
        channel.getNodeStatistics().pingMessageLatency.add(System.currentTimeMillis() - rt.getTime());
      }
    }
  }

  public void close() {
    sendMsgFlag = false;
    if (sendTask != null && !sendTask.isCancelled()) {
      sendTask.cancel(false);
      sendTask = null;
    }
    if (sendMsgThread != null) {
      try {
        sendMsgThread.join(20);
        sendMsgThread = null;
      } catch (Exception e) {
        log.warn("Join send thread failed, peer {}", ctx.channel().remoteAddress());
      }
    }
  }

  private boolean needToLog(Message msg) {
    if (msg instanceof PingMessage ||
        msg instanceof PongMessage ||
        msg instanceof TransactionsMessage) {
      return false;
    }

    if (msg instanceof InventoryMessage &&
        ((InventoryMessage) msg).getInventoryType().equals(Protocol.Inventory.InventoryType.TX)) {
      return false;
    }

    return true;
  }

  private void send() {
    MessageRoundtrip rt = requestQueue.peek();
    if (!sendMsgFlag || rt == null) {
      return;
    }
    if (rt.getRetryTimes() > 0 && !rt.hasToRetry()) {
      return;
    }
    if (rt.getRetryTimes() > 0) {
      channel.getNodeStatistics().nodeDisconnectedLocal(Protocol.ReasonCode.PING_TIMEOUT);
      log.warn("Wait {} timeout. close channel {}.", rt.getMsg().getAnswerMessage(), ctx.channel().remoteAddress());
      channel.close();
      return;
    }

    Message msg = rt.getMsg();

    ctx.writeAndFlush(msg.getSendData()).addListener((ChannelFutureListener) future -> {
      if (!future.isSuccess()) {
        log.error("Fail send to {}, {}", ctx.channel().remoteAddress(), msg);
      }
    });

    rt.incRetryTimes();
    rt.saveTime();
  }

}
