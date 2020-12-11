package cn.ledgeryi.chainbase.common.runtime;

import cn.ledgeryi.protos.Protocol.Transaction.Result.ContractResult;
import cn.ledgeryi.chainbase.core.capsule.TransactionResultCapsule;
import cn.ledgeryi.common.runtime.vm.DataWord;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import static cn.ledgeryi.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

public class ProgramResult {

  private byte[] hReturn = EMPTY_BYTE_ARRAY;
  private byte[] contractAddress = EMPTY_BYTE_ARRAY;
  private RuntimeException exception;
  private Set<DataWord> deleteAccounts;
  private List<InternalTransaction> internalTransactions;
  private TransactionResultCapsule ret = new TransactionResultCapsule();

  @Setter
  @Getter
  private String runtimeError;

  @Getter
  @Setter
  private ContractResult resultCode;

  public byte[] getContractAddress() {
    return Arrays.copyOf(contractAddress, contractAddress.length);
  }

  public byte[] getHReturn() {
    return hReturn;
  }

  public TransactionResultCapsule getRet() {
    return ret;
  }

  public void setRet(TransactionResultCapsule ret) {
    this.ret = ret;
  }

  public RuntimeException getException() {
    return exception;
  }

  public void setException(RuntimeException exception) {
    this.exception = exception;
  }

  public Set<DataWord> getDeleteAccounts() {
    if (deleteAccounts == null) {
      deleteAccounts = new HashSet<>();
    }
    return deleteAccounts;
  }

  public List<InternalTransaction> getInternalTransactions() {
    if (internalTransactions == null) {
      internalTransactions = new ArrayList<>();
    }
    return internalTransactions;
  }
}
