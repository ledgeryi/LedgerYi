package cn.ledgeryi.framework.common.net.udp.message.discover;

import static cn.ledgeryi.framework.common.net.udp.message.UdpMessageTypeEnum.DISCOVER_PING;

import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.protos.Discover;
import com.google.protobuf.ByteString;
import cn.ledgeryi.framework.common.net.udp.message.Message;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.core.config.args.Args;

public class PingMessage extends Message {

  private Discover.PingMessage pingMessage;

  public PingMessage(byte[] data) throws Exception {
    super(DISCOVER_PING, data);
    this.pingMessage = Discover.PingMessage.parseFrom(data);
  }

  public PingMessage(Node from, Node to) {
    super(DISCOVER_PING, null);
    Discover.Endpoint fromEndpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .build();
    Discover.Endpoint toEndpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(to.getId()))
        .setPort(to.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(to.getHost())))
        .build();
    this.pingMessage = Discover.PingMessage.newBuilder()
        .setVersion(Args.getInstance().getNodeP2pVersion())
        .setFrom(fromEndpoint)
        .setTo(toEndpoint)
        .setTimestamp(System.currentTimeMillis())
        .build();
    this.data = this.pingMessage.toByteArray();
  }

  public int getVersion() {
    return this.pingMessage.getVersion();
  }

  public Node getTo() {
    Discover.Endpoint to = this.pingMessage.getTo();
    Node node = new Node(to.getNodeId().toByteArray(),
        ByteArray.toStr(to.getAddress().toByteArray()), to.getPort());
    return node;
  }

  @Override
  public long getTimestamp() {
    return this.pingMessage.getTimestamp();
  }

  @Override
  public Node getFrom() {
    return Message.getNode(pingMessage.getFrom());
  }

  @Override
  public String toString() {
    return "[pingMessage: " + pingMessage;
  }

}
