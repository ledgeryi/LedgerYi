package cn.ledgeryi.framework.core.net.message;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.protos.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlockInventoryMessage extends LedgerYiMessage {

  protected Protocol.BlockInventory blockInventory;

  public BlockInventoryMessage(byte[] data) throws Exception {
    super(data);
    this.type = MessageTypes.BLOCK_INVENTORY.asByte();
    this.blockInventory = Protocol.BlockInventory.parseFrom(data);
  }

  public BlockInventoryMessage(List<BlockCapsule.BlockId> blockIds, Protocol.BlockInventory.Type type) {
    Protocol.BlockInventory.Builder invBuilder = Protocol.BlockInventory.newBuilder();
    blockIds.forEach(blockId -> {
      Protocol.BlockInventory.BlockId.Builder b = Protocol.BlockInventory.BlockId.newBuilder();
      b.setHash(blockId.getByteString());
      b.setNumber(blockId.getNum());
      invBuilder.addIds(b);
    });

    invBuilder.setType(type);
    blockInventory = invBuilder.build();
    this.type = MessageTypes.BLOCK_INVENTORY.asByte();
    this.data = blockInventory.toByteArray();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  private Protocol.BlockInventory getBlockInventory() {
    return blockInventory;
  }

  public List<BlockCapsule.BlockId> getBlockIds() {
    return getBlockInventory().getIdsList().stream()
        .map(blockId -> new BlockCapsule.BlockId(blockId.getHash(), blockId.getNumber()))
        .collect(Collectors.toCollection(ArrayList::new));
  }

}
