package cn.ledgeryi.framework.core.net.message;


import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.common.utils.Sha256Hash;

public class BlockMessage extends LedgerYiMessage {

  private BlockCapsule block;

  public BlockMessage(byte[] data) throws Exception {
    super(data);
    this.type = MessageTypes.BLOCK.asByte();
    this.block = new BlockCapsule(getCodedInputStream(data));
    if (Message.isFilter()) {
      Message.compareBytes(data, block.getInstance().toByteArray());
      TransactionCapsule.validContractProto(block.getInstance().getTransactionsList());
    }
  }

  public BlockMessage(BlockCapsule block) {
    data = block.getData();
    this.type = MessageTypes.BLOCK.asByte();
    this.block = block;
  }

  public BlockCapsule.BlockId getBlockId() {
    return getBlockCapsule().getBlockId();
  }

  public BlockCapsule getBlockCapsule() {
    return block;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public Sha256Hash getMessageId() {
    return getBlockCapsule().getBlockId();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append(block.getBlockId().getString())
        .append(", tx size: ").append(block.getTransactions().size()).append("\n").toString();
  }
}
