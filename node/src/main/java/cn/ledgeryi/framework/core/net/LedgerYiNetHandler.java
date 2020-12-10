package cn.ledgeryi.framework.core.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.overlay.server.Channel;
import cn.ledgeryi.framework.common.overlay.server.MessageQueue;
import cn.ledgeryi.framework.core.net.message.LedgerYiMessage;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;

@Component
@Scope("prototype")
public class LedgerYiNetHandler extends SimpleChannelInboundHandler<LedgerYiMessage> {

  protected PeerConnection peer;

  private MessageQueue msgQueue;

  @Autowired
  private LedgerYiNetService ledgerYiNetService;

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, LedgerYiMessage msg) throws Exception {
    msgQueue.receivedMessage(msg);
    ledgerYiNetService.onMessage(peer, msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    peer.processException(cause);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.peer = (PeerConnection) channel;
  }

}