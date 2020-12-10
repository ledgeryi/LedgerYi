package cn.ledgeryi.framework.common.overlay.message;

import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.common.core.exception.P2pException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.overlay.server.Channel;
import cn.ledgeryi.framework.core.net.message.LedgerYiMessageFactory;

@Component
@Scope("prototype")
public class MessageCodec extends ByteToMessageDecoder {

  private Channel channel;
  private P2pMessageFactory p2pMessageFactory = new P2pMessageFactory();
  private LedgerYiMessageFactory ledgerYiMessageFactory = new LedgerYiMessageFactory();

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
      throws Exception {
    int length = buffer.readableBytes();
    byte[] encoded = new byte[length];
    buffer.readBytes(encoded);
    try {
      Message msg = createMessage(encoded);
      channel.getNodeStatistics().tcpFlow.add(length);
      out.add(msg);
    } catch (Exception e) {
      channel.processException(e);
    }
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  private Message createMessage(byte[] encoded) throws Exception {
    byte type = encoded[0];
    if (MessageTypes.inP2pRange(type)) {
      return p2pMessageFactory.create(encoded);
    }
    if (MessageTypes.inLedgerYiRange(type)) {
      return ledgerYiMessageFactory.create(encoded);
    }
    throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, "type=" + encoded[0]);
  }

}