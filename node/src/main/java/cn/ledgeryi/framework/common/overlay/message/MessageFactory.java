package cn.ledgeryi.framework.common.overlay.message;

import cn.ledgeryi.chainbase.common.message.Message;

public abstract class MessageFactory {

  protected abstract Message create(byte[] data) throws Exception;

}
