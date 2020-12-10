package cn.ledgeryi.framework.common.overlay.message;

import cn.ledgeryi.chainbase.common.message.Message;

public abstract class P2pMessage extends Message {

  public P2pMessage() {
  }

  public P2pMessage(byte[] rawData) {
    super(rawData);
  }

  public P2pMessage(byte type, byte[] rawData) {
    super(type, rawData);
  }

}
