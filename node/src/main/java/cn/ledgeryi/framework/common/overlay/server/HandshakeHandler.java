package cn.ledgeryi.framework.common.overlay.server;

import cn.ledgeryi.protos.Protocol.ReasonCode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;
import cn.ledgeryi.framework.common.overlay.message.DisconnectMessage;
import cn.ledgeryi.framework.common.overlay.message.HelloMessage;
import cn.ledgeryi.framework.common.overlay.message.P2pMessage;
import cn.ledgeryi.framework.common.overlay.message.P2pMessageFactory;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class HandshakeHandler extends ByteToMessageDecoder {

  @Autowired
  protected ChannelManager channelManager;
  @Autowired
  protected NodeManager nodeManager;
  @Autowired
  protected Manager manager;
  @Autowired
  private SyncPool syncPool;

  private byte[] remoteId;
  protected Channel channel;
  private final P2pMessageFactory messageFactory = new P2pMessageFactory();


  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    log.info("channel active, {}", ctx.channel().remoteAddress());
    channel.setChannelHandlerContext(ctx);
    if (remoteId.length == 64) {
      channel.initNode(remoteId, ((InetSocketAddress) ctx.channel().remoteAddress()).getPort());
      sendHelloMsg(ctx, System.currentTimeMillis());
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
    byte[] encoded = new byte[buffer.readableBytes()];
    buffer.readBytes(encoded);
    P2pMessage msg = messageFactory.create(encoded);

    log.info("Handshake Receive from {}, {}", ctx.channel().remoteAddress(), msg);

    switch (msg.getType()) {
      case P2P_HELLO:
        handleHelloMsg(ctx, (HelloMessage) msg);
        break;
      case P2P_DISCONNECT:
        if (channel.getNodeStatistics() != null) {
          channel.getNodeStatistics().nodeDisconnectedRemote(((DisconnectMessage) msg).getReasonCode());
        }
        channel.close();
        break;
      default:
        channel.close();
        break;
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    channel.processException(cause);
  }

  public void setChannel(Channel channel, String remoteId) {
    this.channel = channel;
    this.remoteId = Hex.decode(remoteId);
  }

  protected void sendHelloMsg(ChannelHandlerContext ctx, long time) {
    HelloMessage message = new HelloMessage(nodeManager.getPublicHomeNode(), time,
        manager.getGenesisBlockId(), manager.getSolidBlockId(), manager.getHeadBlockId());
    ctx.writeAndFlush(message.getSendData());
    channel.getNodeStatistics().messageStatistics.addTcpOutMessage(message);
    log.info("Handshake Send to {}, {} ", ctx.channel().remoteAddress(), message);
  }

  private void handleHelloMsg(ChannelHandlerContext ctx, HelloMessage msg) {
    channel.initNode(msg.getFrom().getId(), msg.getFrom().getPort());
    if (remoteId.length != 64) {
      InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
      if (channelManager.getTrustNodes().getIfPresent(address) == null && !syncPool.isCanConnect()) {
        channel.disconnect(ReasonCode.TOO_MANY_PEERS);
        return;
      }
    }
    if (msg.getVersion() != Args.getInstance().getNodeP2pVersion()) {
      log.info("Peer {} different p2p version, peer->{}, me->{}",
          ctx.channel().remoteAddress(), msg.getVersion(), Args.getInstance().getNodeP2pVersion());
      channel.disconnect(ReasonCode.INCOMPATIBLE_VERSION);
      return;
    }
    if (!Arrays.equals(manager.getGenesisBlockId().getBytes(), msg.getGenesisBlockId().getBytes())) {
      log.info("Peer {} different genesis block, peer->{}, me->{}", ctx.channel().remoteAddress(),
              msg.getGenesisBlockId().getString(), manager.getGenesisBlockId().getString());
      channel.disconnect(ReasonCode.INCOMPATIBLE_CHAIN);
      return;
    }
    if (manager.getSolidBlockId().getNum() >= msg.getSolidBlockId().getNum() && !manager
        .containBlockInMainChain(msg.getSolidBlockId())) {
      log.info("Peer {} different solid block, peer->{}, me->{}", ctx.channel().remoteAddress(),
          msg.getSolidBlockId().getString(), manager.getSolidBlockId().getString());
      channel.disconnect(ReasonCode.FORKED);
      return;
    }
    ((PeerConnection) channel).setHelloMessage(msg);
    channel.getNodeStatistics().messageStatistics.addTcpInMessage(msg);
    channel.publicHandshakeFinished(ctx, msg);
    if (!channelManager.processPeer(channel)) {
      return;
    }
    if (remoteId.length != 64) {
      sendHelloMsg(ctx, msg.getTimestamp());
    }
    syncPool.onConnect(channel);
  }
}
