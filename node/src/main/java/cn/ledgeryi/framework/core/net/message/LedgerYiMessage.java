package cn.ledgeryi.framework.core.net.message;

import cn.ledgeryi.chainbase.common.message.Message;

public abstract class LedgerYiMessage extends Message {

  public LedgerYiMessage() {
  }

  public LedgerYiMessage(byte[] rawData) {
    super(rawData);
  }

  public LedgerYiMessage(byte type, byte[] rawData) {
    super(type, rawData);
  }
}
