package cn.ledgeryi.framework.common.net.udp.message.discover;

import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.framework.common.net.udp.message.Message;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.protos.Discover;
import com.google.protobuf.ByteString;

import static cn.ledgeryi.framework.common.net.udp.message.UdpMessageTypeEnum.DISCOVER_PONG;

public class PongMessage extends Message {

  private Discover.PongMessage pongMessage;

  public PongMessage(byte[] data) throws Exception {
    super(DISCOVER_PONG, data);
    this.pongMessage = Discover.PongMessage.parseFrom(data);
  }

  public PongMessage(Node from, long sequence) {
    super(DISCOVER_PONG, null);
    Discover.Endpoint toEndpoint = Discover.Endpoint.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .setPort(from.getPort())
        .setNodeId(ByteString.copyFrom(from.getId()))
        .build();
    this.pongMessage = Discover.PongMessage.newBuilder()
        .setFrom(toEndpoint)
        .setEcho(Args.getInstance().getNodeP2pVersion())
        .setTimestamp(sequence)
        .build();
    this.data = this.pongMessage.toByteArray();
  }

  public int getVersion() {
    return this.pongMessage.getEcho();
  }

  @Override
  public long getTimestamp() {
    return this.pongMessage.getTimestamp();
  }

  @Override
  public Node getFrom() {
    return Message.getNode(pongMessage.getFrom());
  }

  @Override
  public String toString() {
    return "[pongMessage: " + pongMessage;
  }
}
