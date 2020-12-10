package cn.ledgeryi.framework.common.overlay.message;


import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.protos.Protocol;

public class DisconnectMessage extends P2pMessage {

  private Protocol.DisconnectMessage disconnectMessage;

  public DisconnectMessage(byte type, byte[] rawData) throws Exception {
    super(type, rawData);
    this.disconnectMessage = Protocol.DisconnectMessage.parseFrom(this.data);
  }

  public DisconnectMessage(Protocol.ReasonCode reasonCode) {
    this.disconnectMessage = Protocol.DisconnectMessage
        .newBuilder()
        .setReason(reasonCode)
        .build();
    this.type = MessageTypes.P2P_DISCONNECT.asByte();
    this.data = this.disconnectMessage.toByteArray();
  }

  public int getReason() {
    return this.disconnectMessage.getReason().getNumber();
  }

  public Protocol.ReasonCode getReasonCode() {
    return disconnectMessage.getReason();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append("reason: ")
        .append(this.disconnectMessage.getReason()).toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}