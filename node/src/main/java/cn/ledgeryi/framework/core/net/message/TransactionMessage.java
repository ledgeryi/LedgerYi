package cn.ledgeryi.framework.core.net.message;


import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.protos.Protocol;

public class TransactionMessage extends LedgerYiMessage {

  private TransactionCapsule transactionCapsule;

  public TransactionMessage(byte[] data) throws Exception {
    super(data);
    this.transactionCapsule = new TransactionCapsule(getCodedInputStream(data));
    this.type = MessageTypes.TX.asByte();
    if (Message.isFilter()) {
      compareBytes(data, transactionCapsule.getInstance().toByteArray());
      TransactionCapsule.validContractProto(transactionCapsule.getInstance().getRawData().getContract());
    }
  }

  public TransactionMessage(Protocol.Transaction tx) {
    this.transactionCapsule = new TransactionCapsule(tx);
    this.type = MessageTypes.TX.asByte();
    this.data = tx.toByteArray();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString())
        .append("messageId: ").append(super.getMessageId()).toString();
  }

  @Override
  public Sha256Hash getMessageId() {
    return this.transactionCapsule.getTransactionId();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public TransactionCapsule getTransactionCapsule() {
    return this.transactionCapsule;
  }
}
