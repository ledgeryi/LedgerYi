package cn.ledgeryi.framework.core.capsule;

import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.ProtoCapsule;
import cn.ledgeryi.chainbase.core.capsule.ReceiptCapsule;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.chainbase.core.db.TransactionTrace;
import cn.ledgeryi.common.core.exception.BadItemException;
import cn.ledgeryi.common.runtime.vm.LogInfo;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.TransactionInfo.Log;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j(topic = "capsule")
public class TransactionInfoCapsule implements ProtoCapsule<Protocol.TransactionInfo> {

  private Protocol.TransactionInfo transactionInfo;

  /**
   * constructor TransactionCapsule.
   */
  public TransactionInfoCapsule(Protocol.TransactionInfo txRet) {
    this.transactionInfo = txRet;
  }

  public TransactionInfoCapsule(byte[] data) throws BadItemException {
    try {
      this.transactionInfo = Protocol.TransactionInfo.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("TransactionInfoCapsule proto data parse exception");
    }
  }

  public static TransactionInfoCapsule buildInstance(TransactionCapsule txCap, BlockCapsule block, TransactionTrace trace) {
    Protocol.TransactionInfo.Builder builder = Protocol.TransactionInfo.newBuilder();
    ReceiptCapsule traceReceipt = trace.getReceipt();
    builder.setResult(Protocol.TransactionInfo.code.SUCESS);
    if (StringUtils.isNoneEmpty(trace.getRuntimeError()) || Objects.nonNull(trace.getRuntimeResult().getException())) {
      builder.setResult(Protocol.TransactionInfo.code.FAILED);
      builder.setResMessage(ByteString.copyFromUtf8(trace.getRuntimeError()));
    }
    builder.setId(ByteString.copyFrom(txCap.getTransactionId().getBytes()));
    ProgramResult programResult = trace.getRuntimeResult();
    ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
    ByteString contractAddress = ByteString.copyFrom(programResult.getContractAddress());

    builder.addContractResult(contractResult);
    builder.setContractAddress(contractAddress);

    List<Log> logList = new ArrayList<>();
    programResult.getLogInfoList().forEach(logInfo -> logList.add(LogInfo.buildLog(logInfo)));
    builder.addAllLog(logList);

    if (Objects.nonNull(block)) {
      builder.setBlockNumber(block.getInstance().getBlockHeader().getRawData().getNumber());
      builder.setBlockTimeStamp(block.getInstance().getBlockHeader().getRawData().getTimestamp());
    }

    builder.setReceipt(traceReceipt.getReceipt());

    return new TransactionInfoCapsule(builder.build());
  }

  public byte[] getId() {
    return transactionInfo.getId().toByteArray();
  }

  public void setId(byte[] id) {
    this.transactionInfo = this.transactionInfo.toBuilder().setId(ByteString.copyFrom(id)).build();
  }

  public void setResult(Protocol.TransactionInfo.code result) {
    this.transactionInfo = this.transactionInfo.toBuilder().setResult(result).build();
  }

  public void setResMessage(String message) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .setResMessage(ByteString.copyFromUtf8(message)).build();
  }

  public long getBlockNumber() {
    return transactionInfo.getBlockNumber();
  }

  public void setBlockNumber(long num) {
    this.transactionInfo = this.transactionInfo.toBuilder().setBlockNumber(num).build();
  }

  public long getBlockTimeStamp() {
    return transactionInfo.getBlockTimeStamp();
  }

  public void setBlockTimeStamp(long time) {
    this.transactionInfo = this.transactionInfo.toBuilder().setBlockTimeStamp(time).build();
  }

  public void setContractResult(byte[] ret) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .addContractResult(ByteString.copyFrom(ret))
        .build();
  }

  public void setContractAddress(byte[] contractAddress) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .setContractAddress(ByteString.copyFrom(contractAddress))
        .build();
  }

  public void setReceipt(ReceiptCapsule receipt) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .setReceipt(receipt.getReceipt())
        .build();
  }

  public void addAllLog(List<Protocol.TransactionInfo.Log> logs) {
    this.transactionInfo = this.transactionInfo.toBuilder()
        .addAllLog(logs)
        .build();
  }

  @Override
  public byte[] getData() {
    return this.transactionInfo.toByteArray();
  }

  @Override
  public Protocol.TransactionInfo getInstance() {
    return this.transactionInfo;
  }
}