package cn.ledgeryi.framework.common.overlay.message;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.protos.Discover;
import cn.ledgeryi.protos.Protocol;
import com.google.protobuf.ByteString;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.core.config.args.Args;

public class HelloMessage extends P2pMessage {

  private Protocol.HelloMessage helloMessage;

  public HelloMessage(byte type, byte[] rawData) throws Exception {
    super(type, rawData);
    this.helloMessage = Protocol.HelloMessage.parseFrom(rawData);
  }

  public HelloMessage(Node from, long timestamp, BlockCapsule.BlockId genesisBlockId,
      BlockCapsule.BlockId solidBlockId, BlockCapsule.BlockId headBlockId) {

    Discover.Endpoint fromEndpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .build();

    Protocol.HelloMessage.BlockId gBlockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(genesisBlockId.getByteString())
        .setNumber(genesisBlockId.getNum())
        .build();

    Protocol.HelloMessage.BlockId sBlockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(solidBlockId.getByteString())
        .setNumber(solidBlockId.getNum())
        .build();

    Protocol.HelloMessage.BlockId hBlockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(headBlockId.getByteString())
        .setNumber(headBlockId.getNum())
        .build();

    Protocol.HelloMessage.Builder builder = Protocol.HelloMessage.newBuilder();

    builder.setFrom(fromEndpoint);
    builder.setVersion(Args.getInstance().getNodeP2pVersion());
    builder.setTimestamp(timestamp);
    builder.setGenesisBlockId(gBlockId);
    builder.setSolidBlockId(sBlockId);
    builder.setHeadBlockId(hBlockId);

    this.helloMessage = builder.build();
    this.type = MessageTypes.P2P_HELLO.asByte();
    this.data = this.helloMessage.toByteArray();
  }

  public int getVersion() {
    return this.helloMessage.getVersion();
  }

  public long getTimestamp() {
    return this.helloMessage.getTimestamp();
  }

  public Node getFrom() {
    Discover.Endpoint from = this.helloMessage.getFrom();
    return new Node(from.getNodeId().toByteArray(),
        ByteArray.toStr(from.getAddress().toByteArray()), from.getPort());
  }

  public BlockCapsule.BlockId getGenesisBlockId() {
    return new BlockCapsule.BlockId(this.helloMessage.getGenesisBlockId().getHash(),
        this.helloMessage.getGenesisBlockId().getNumber());
  }

  public BlockCapsule.BlockId getSolidBlockId() {
    return new BlockCapsule.BlockId(this.helloMessage.getSolidBlockId().getHash(),
        this.helloMessage.getSolidBlockId().getNumber());
  }

  public BlockCapsule.BlockId getHeadBlockId() {
    return new BlockCapsule.BlockId(this.helloMessage.getHeadBlockId().getHash(),
        this.helloMessage.getHeadBlockId().getNumber());
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append(helloMessage.toString()).toString();
  }

}