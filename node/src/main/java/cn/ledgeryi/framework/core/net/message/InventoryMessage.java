package cn.ledgeryi.framework.core.net.message;

import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.protos.Protocol;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


public class InventoryMessage extends LedgerYiMessage {

  protected Protocol.Inventory inv;

  public InventoryMessage(byte[] data) throws Exception {
    super(data);
    this.type = MessageTypes.INVENTORY.asByte();
    this.inv = Protocol.Inventory.parseFrom(data);
  }

  public InventoryMessage(Protocol.Inventory inv) {
    this.inv = inv;
    this.type = MessageTypes.INVENTORY.asByte();
    this.data = inv.toByteArray();
  }

  public InventoryMessage(List<Sha256Hash> hashList, Protocol.Inventory.InventoryType type) {
    Protocol.Inventory.Builder invBuilder = Protocol.Inventory.newBuilder();
    for (Sha256Hash hash : hashList) {
      invBuilder.addIds(hash.getByteString());
    }
    invBuilder.setType(type);
    inv = invBuilder.build();
    this.type = MessageTypes.INVENTORY.asByte();
    this.data = inv.toByteArray();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public Protocol.Inventory getInventory() {
    return inv;
  }

  public MessageTypes getInvMessageType() {
    return getInventoryType().equals(Protocol.Inventory.InventoryType.BLOCK) ? MessageTypes.BLOCK : MessageTypes.TX;

  }

  public Protocol.Inventory.InventoryType getInventoryType() {
    return inv.getType();
  }

  @Override
  public String toString() {
    Deque<Sha256Hash> hashes = new LinkedList<>(getHashList());
    StringBuilder builder = new StringBuilder();
    builder.append(super.toString()).append("invType: ").append(getInvMessageType())
        .append(", size: ").append(hashes.size())
        .append(", First hash: ").append(hashes.peekFirst());
    if (hashes.size() > 1) {
      builder.append(", End hash: ").append(hashes.peekLast());
    }
    return builder.toString();
  }

  public List<Sha256Hash> getHashList() {
    return getInventory().getIdsList().stream()
        .map(hash -> Sha256Hash.wrap(hash.toByteArray()))
        .collect(Collectors.toList());
  }

}
