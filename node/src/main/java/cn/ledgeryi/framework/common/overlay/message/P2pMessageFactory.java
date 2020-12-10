package cn.ledgeryi.framework.common.overlay.message;

import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.common.core.exception.P2pException;
import org.apache.commons.lang3.ArrayUtils;

public class P2pMessageFactory extends MessageFactory {

  @Override
  public P2pMessage create(byte[] data) throws Exception {
    if (data.length <= 1) {
      throw new P2pException(P2pException.TypeEnum.MESSAGE_WITH_WRONG_LENGTH,
          "messageType=" + (data.length == 1 ? data[0] : "unknow"));
    }
    try {
      byte type = data[0];
      byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
      return create(type, rawData);
    } catch (Exception e) {
      if (e instanceof P2pException) {
        throw e;
      } else {
        throw new P2pException(P2pException.TypeEnum.PARSE_MESSAGE_FAILED,
            "type=" + data[0] + ", len=" + data.length);
      }
    }
  }

  private P2pMessage create(byte type, byte[] rawData) throws Exception {
    MessageTypes messageType = MessageTypes.fromByte(type);
    if (messageType == null) {
      throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
          "type=" + type + ", len=" + rawData.length);
    }
    switch (messageType) {
      case P2P_HELLO:
        return new HelloMessage(type, rawData);
      case P2P_DISCONNECT:
        return new DisconnectMessage(type, rawData);
      case P2P_PING:
        return new PingMessage(type, rawData);
      case P2P_PONG:
        return new PongMessage(type, rawData);
      default:
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, messageType.toString());
    }
  }
}
