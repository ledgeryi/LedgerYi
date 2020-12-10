package cn.ledgeryi.chainbase.core.capsule;

import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.protos.Protocol.ResourceReceipt;
import cn.ledgeryi.protos.Protocol.Transaction;

public class ReceiptCapsule {

  private ResourceReceipt receipt;

  private Sha256Hash receiptAddress;

  public ReceiptCapsule(ResourceReceipt data, Sha256Hash receiptAddress) {
    this.receipt = data;
    this.receiptAddress = receiptAddress;
  }

  public ReceiptCapsule(Sha256Hash receiptAddress) {
    this.receipt = ResourceReceipt.newBuilder().build();
    this.receiptAddress = receiptAddress;
  }

  public static ResourceReceipt copyReceipt(ReceiptCapsule origin) {
    return origin.getReceipt().toBuilder().build();
  }

  public Sha256Hash getReceiptAddress() {
    return this.receiptAddress;
  }

  public Transaction.Result.contractResult getResult() {
    return this.receipt.getResult();
  }

  public void setResult(Transaction.Result.contractResult success) {
    this.receipt = receipt.toBuilder().setResult(success).build();
  }

  public ResourceReceipt getReceipt() {
    return receipt;
  }
}
