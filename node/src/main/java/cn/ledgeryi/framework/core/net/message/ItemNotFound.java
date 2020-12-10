package cn.ledgeryi.framework.core.net.message;

import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.Items;

public class ItemNotFound extends LedgerYiMessage {

  private Items notFound;

  /**
   * means can not find this block or tx.
   */
  public ItemNotFound() {
    Protocol.Items.Builder itemsBuilder = Protocol.Items.newBuilder();
    itemsBuilder.setType(Protocol.Items.ItemType.ERR);
    notFound = itemsBuilder.build();
    this.type = MessageTypes.ITEM_NOT_FOUND.asByte();
    this.data = notFound.toByteArray();
  }

  @Override
  public String toString() {
    return "item not found";
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

}
