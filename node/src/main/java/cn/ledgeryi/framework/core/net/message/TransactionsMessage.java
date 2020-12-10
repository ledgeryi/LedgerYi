package cn.ledgeryi.framework.core.net.message;

import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.protos.Protocol;

import java.util.List;

public class TransactionsMessage extends LedgerYiMessage {

  private Protocol.Transactions transactions;

  public TransactionsMessage(List<Protocol.Transaction> txs) {
    Protocol.Transactions.Builder builder = Protocol.Transactions.newBuilder();
    txs.forEach(tx -> builder.addTransactions(tx));
    this.transactions = builder.build();
    this.type = MessageTypes.TXS.asByte();
    this.data = this.transactions.toByteArray();
  }

  public TransactionsMessage(byte[] data) throws Exception {
    super(data);
    this.type = MessageTypes.TXS.asByte();
    this.transactions = Protocol.Transactions.parseFrom(getCodedInputStream(data));
    if (isFilter()) {
      compareBytes(data, transactions.toByteArray());
      TransactionCapsule.validContractProto(transactions.getTransactionsList());
    }
  }

  public Protocol.Transactions getTransactions() {
    return transactions;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append("tx size: ")
        .append(this.transactions.getTransactionsList().size()).toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

}
