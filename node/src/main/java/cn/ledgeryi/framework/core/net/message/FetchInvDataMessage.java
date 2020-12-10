package cn.ledgeryi.framework.core.net.message;

import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.protos.Protocol;

import java.util.List;

public class FetchInvDataMessage extends InventoryMessage {


  public FetchInvDataMessage(byte[] packed) throws Exception {
    super(packed);
    this.type = MessageTypes.FETCH_INV_DATA.asByte();
  }

  public FetchInvDataMessage(Protocol.Inventory inv) {
    super(inv);
    this.type = MessageTypes.FETCH_INV_DATA.asByte();
  }

  public FetchInvDataMessage(List<Sha256Hash> hashList, Protocol.Inventory.InventoryType type) {
    super(hashList, type);
    this.type = MessageTypes.FETCH_INV_DATA.asByte();
  }

}
