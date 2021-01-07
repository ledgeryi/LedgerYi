package cn.ledgeryi.chainbase.core.capsule;

import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
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

  public Transaction.Result.ContractResult getResult() {
    return this.receipt.getResult();
  }

  public void setResult(ProgramResult result) {
    this.receipt = receipt.toBuilder()
            .setResult(result.getResultCode())
            .setStorageUsed(result.getStorageUsed())
            .setCpuTimeUsed(result.getCpuTimeUsed())
            .build();
  }

  public ResourceReceipt getReceipt() {
    return receipt;
  }
}
