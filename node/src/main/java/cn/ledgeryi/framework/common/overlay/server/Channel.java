package cn.ledgeryi.framework.common.overlay.server;

import cn.ledgeryi.common.core.db.ByteArrayWrapper;
import cn.ledgeryi.common.core.exception.P2pException;
import cn.ledgeryi.protos.Protocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeHandler;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;
import cn.ledgeryi.framework.common.overlay.discover.node.statistics.NodeStatistics;
import cn.ledgeryi.framework.common.overlay.message.DisconnectMessage;
import cn.ledgeryi.framework.common.overlay.message.HelloMessage;
import cn.ledgeryi.framework.common.overlay.message.MessageCodec;
import cn.ledgeryi.framework.common.overlay.message.StaticMessages;
import cn.ledgeryi.framework.core.net.LedgerYiNetHandler;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class Channel {

  @Autowired
  protected MessageQueue msgQueue;
  protected NodeStatistics nodeStatistics;
  @Autowired
  private MessageCodec messageCodec;
  @Autowired
  private NodeManager nodeManager;
  @Autowired
  private StaticMessages staticMessages;
  @Autowired
  private WireTrafficStats stats;
  @Autowired
  private HandshakeHandler handshakeHandler;
  @Autowired
  private P2pHandler p2pHandler;
  @Autowired
  private LedgerYiNetHandler ledgerYiNetHandler;
  private ChannelManager channelManager;
  private ChannelHandlerContext ctx;
  private InetSocketAddress inetSocketAddress;
  private Node node;
  private long startTime;
  private LedgerYiState ledgerYiState = LedgerYiState.INIT;
  private boolean isActive;
  private volatile boolean isDisconnect;
  private boolean isTrustPeer;
  private boolean isFastForwardPeer;

  public void init(ChannelPipeline pipeline, String remoteId, ChannelManager channelManager) {
    this.channelManager = channelManager;
    isActive = remoteId != null && !remoteId.isEmpty();
    startTime = System.currentTimeMillis();
    pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(60, TimeUnit.SECONDS));
    pipeline.addLast(stats.tcp);
    pipeline.addLast("protoPender", new ProtobufVarint32LengthFieldPrepender());
    pipeline.addLast("lengthDecode", new LedgerYiProtobufVarint32FrameDecoder(this));
    //handshake first
    pipeline.addLast("handshakeHandler", handshakeHandler);

    messageCodec.setChannel(this);
    msgQueue.setChannel(this);
    handshakeHandler.setChannel(this, remoteId);
    p2pHandler.setChannel(this);
    ledgerYiNetHandler.setChannel(this);
    p2pHandler.setMsgQueue(msgQueue);
    ledgerYiNetHandler.setMsgQueue(msgQueue);
  }

  public void publicHandshakeFinished(ChannelHandlerContext ctx, HelloMessage msg) {
    isTrustPeer = channelManager.getTrustNodes().getIfPresent(getInetAddress()) != null;
    isFastForwardPeer = channelManager.getFastForwardNodes().containsKey(getInetAddress());
    ctx.pipeline().remove(handshakeHandler);
    msgQueue.activate(ctx);
    ctx.pipeline().addLast("messageCodec", messageCodec);
    ctx.pipeline().addLast("p2p", p2pHandler);
    ctx.pipeline().addLast("data", ledgerYiNetHandler);
    setStartTime(msg.getTimestamp());
    setJingCHainState(LedgerYiState.HANDSHAKE_FINISHED);
    getNodeStatistics().p2pHandShake.add();
    log.info("Finish handshake with {}.", ctx.channel().remoteAddress());
  }

  /**
   * Set node and register it in NodeManager if it is not registered yet.
   */
  public void initNode(byte[] nodeId, int remotePort) {
    Node n = new Node(nodeId, inetSocketAddress.getHostString(), remotePort);
    NodeHandler handler = nodeManager.getNodeHandler(n);
    node = handler.getNode();
    nodeStatistics = handler.getNodeStatistics();
    handler.getNode().setId(nodeId);
  }

  public void disconnect(Protocol.ReasonCode reason) {
    this.isDisconnect = true;
    channelManager.processDisconnect(this, reason);
    DisconnectMessage msg = new DisconnectMessage(reason);
    log.info("Send to {} online-time {}s, {}",
        ctx.channel().remoteAddress(),
        (System.currentTimeMillis() - startTime) / 1000,
        msg);
    getNodeStatistics().nodeDisconnectedLocal(reason);
    ctx.writeAndFlush(msg.getSendData()).addListener(future -> close());
  }

  public void processException(Throwable throwable) {
    Throwable baseThrowable = throwable;
    while (baseThrowable.getCause() != null) {
      baseThrowable = baseThrowable.getCause();
    }
    SocketAddress address = ctx.channel().remoteAddress();
    if (throwable instanceof ReadTimeoutException ||
        throwable instanceof IOException) {
      log.warn("Close peer {}, reason: {}", address, throwable.getMessage());
    } else if (baseThrowable instanceof P2pException) {
      log.warn("Close peer {}, type: {}, info: {}",
          address, ((P2pException) baseThrowable).getType(), baseThrowable.getMessage());
    } else {
      log.error("Close peer {}, exception caught", address, throwable);
    }
    close();
  }

  public void close() {
    this.isDisconnect = true;
    p2pHandler.close();
    msgQueue.close();
    ctx.close();
  }

  public Node getNode() {
    return node;
  }

  public byte[] getNodeId() {
    return node == null ? null : node.getId();
  }

  public ByteArrayWrapper getNodeIdWrapper() {
    return node == null ? null : new ByteArrayWrapper(node.getId());
  }

  public String getPeerId() {
    return node == null ? "<null>" : node.getHexId();
  }

  public void setChannelHandlerContext(ChannelHandlerContext ctx) {
    this.ctx = ctx;
    this.inetSocketAddress = ctx == null ? null : (InetSocketAddress) ctx.channel().remoteAddress();
  }

  public InetAddress getInetAddress() {
    return ctx == null ? null : ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
  }

  public NodeStatistics getNodeStatistics() {
    return nodeStatistics;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public void setJingCHainState(LedgerYiState ledgerYiState) {
    this.ledgerYiState = ledgerYiState;
    log.info("Peer {} status change to {}.", inetSocketAddress, ledgerYiState);
  }

  public boolean isActive() {
    return isActive;
  }

  public boolean isDisconnect() {
    return isDisconnect;
  }

  public boolean isTrustPeer() {
    return isTrustPeer;
  }

  public boolean isFastForwardPeer() {
    return isFastForwardPeer;
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Channel channel = (Channel) o;
    if (inetSocketAddress != null ? !inetSocketAddress.equals(channel.inetSocketAddress)
        : channel.inetSocketAddress != null) {
      return false;
    }
    if (node != null ? !node.equals(channel.node) : channel.node != null) {
      return false;
    }
    return this == channel;
  }

  @Override
  public int hashCode() {
    int result = inetSocketAddress != null ? inetSocketAddress.hashCode() : 0;
    result = 31 * result + (node != null ? node.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return String.format("%s | %s", inetSocketAddress, getPeerId());
  }

  public enum LedgerYiState {
    INIT,
    HANDSHAKE_FINISHED,
    START_TO_SYNC,
    SYNCING,
    SYNC_COMPLETED,
    SYNC_FAILED
  }

}

