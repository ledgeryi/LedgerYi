package cn.ledgeryi.framework.common.net.udp.message.discover;

import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.framework.common.net.udp.message.Message;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.protos.Discover;
import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.List;

import static cn.ledgeryi.framework.common.net.udp.message.UdpMessageTypeEnum.DISCOVER_NEIGHBORS;

public class NeighborsMessage extends Message {

  private Discover.Neighbours neighbours;

  public NeighborsMessage(byte[] data) throws Exception {
    super(DISCOVER_NEIGHBORS, data);
    this.neighbours = Discover.Neighbours.parseFrom(data);
  }

  public NeighborsMessage(Node from, List<Node> neighbours, long sequence) {
    super(DISCOVER_NEIGHBORS, null);
    Discover.Neighbours.Builder builder = Discover.Neighbours.newBuilder()
        .setTimestamp(sequence);

    neighbours.forEach(neighbour -> {
      Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
          .setAddress(ByteString.copyFrom(ByteArray.fromString(neighbour.getHost())))
          .setPort(neighbour.getPort())
          .setNodeId(ByteString.copyFrom(neighbour.getId()))
          .build();

      builder.addNeighbours(endpoint);
    });

    Discover.Endpoint fromEndpoint = Discover.Endpoint.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .setPort(from.getPort())
        .setNodeId(ByteString.copyFrom(from.getId()))
        .build();

    builder.setFrom(fromEndpoint);

    this.neighbours = builder.build();

    this.data = this.neighbours.toByteArray();
  }

  public List<Node> getNodes() {
    List<Node> nodes = new ArrayList<>();
    neighbours.getNeighboursList().forEach(neighbour -> nodes.add(
        new Node(neighbour.getNodeId().toByteArray(),
            ByteArray.toStr(neighbour.getAddress().toByteArray()),
            neighbour.getPort())));
    return nodes;
  }

  @Override
  public long getTimestamp() {
    return this.neighbours.getTimestamp();
  }

  @Override
  public Node getFrom() {
    return Message.getNode(neighbours.getFrom());
  }

  @Override
  public String toString() {
    return "[neighbours: " + neighbours;
  }

}
