package cn.ledgeryi.chainbase.core.capsule;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import cn.ledgeryi.common.core.exception.BadItemException;
import cn.ledgeryi.protos.Protocol.Transaction.Result;
import cn.ledgeryi.protos.Protocol.Transaction.Result.contractResult;

@Slf4j(topic = "capsule")
public class TransactionResultCapsule implements ProtoCapsule<Result> {

  private Result transactionResult;

  /**
   * constructor TransactionCapsule.
   */
  public TransactionResultCapsule(Result txRet) {
    this.transactionResult = txRet;
  }

  public TransactionResultCapsule(byte[] data) throws BadItemException {
    try {
      this.transactionResult = Result.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("TransactionResult proto data parse exception");
    }
  }

  public TransactionResultCapsule() {
    this.transactionResult = Result.newBuilder().build();
  }

  public TransactionResultCapsule(contractResult code) {
    this.transactionResult = Result.newBuilder().setContractRet(code).build();
  }

  public void setStatus(Result.code code) {
    this.transactionResult = this.transactionResult.toBuilder().setRet(code).build();
  }

  public void setErrorCode(Result.code code) {
    this.transactionResult = this.transactionResult.toBuilder().setRet(code).build();
  }

  @Override
  public byte[] getData() {
    return this.transactionResult.toByteArray();
  }

  @Override
  public Result getInstance() {
    return this.transactionResult;
  }
}