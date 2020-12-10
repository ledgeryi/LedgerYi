package cn.ledgeryi.chainbase.core.db;

import cn.ledgeryi.chainbase.common.runtime.InternalTransaction;
import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.common.utils.ForkUtils;
import cn.ledgeryi.chainbase.core.capsule.*;
import cn.ledgeryi.chainbase.core.store.*;
import cn.ledgeryi.common.core.exception.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import cn.ledgeryi.common.runtime.vm.DataWord;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;
import cn.ledgeryi.chainbase.common.runtime.Runtime;

import java.util.Objects;

import static cn.ledgeryi.chainbase.common.runtime.InternalTransaction.TxType.TX_CONTRACT_CALL_TYPE;
import static cn.ledgeryi.chainbase.common.runtime.InternalTransaction.TxType.TX_CONTRACT_CREATION_TYPE;

@Slf4j(topic = "TransactionTrace")
public class TransactionTrace {

  private TransactionCapsule tx;

  private ReceiptCapsule receipt;

  private StoreFactory storeFactory;

  private DynamicPropertiesStore dynamicPropertiesStore;

  private AccountStore accountStore;

  private CodeStore codeStore;


  private InternalTransaction.TxType txType;

  private long txStartTimeInMs;

  private Runtime runtime;

  private ForkUtils forkUtils;

  @Getter
  private TransactionContext transactionContext;
  @Getter
  @Setter
  private TimeResultType timeResultType = TimeResultType.NORMAL;

  public TransactionTrace(TransactionCapsule tx, StoreFactory storeFactory, Runtime runtime) {
    this.tx = tx;
    txType = InternalTransaction.TxType.TX_PRECOMPILED_TYPE;
    this.storeFactory = storeFactory;
    this.dynamicPropertiesStore = storeFactory.getChainBaseManager().getDynamicPropertiesStore();
    this.codeStore = storeFactory.getChainBaseManager().getCodeStore();
    this.accountStore = storeFactory.getChainBaseManager().getAccountStore();

    this.receipt = new ReceiptCapsule(Sha256Hash.ZERO_HASH);
    this.runtime = runtime;
    this.forkUtils = new ForkUtils();
    forkUtils.init(dynamicPropertiesStore);
  }

  public TransactionCapsule getTx() {
    return tx;
  }

  private boolean needVM() {
    return this.txType == TX_CONTRACT_CALL_TYPE || this.txType == TX_CONTRACT_CREATION_TYPE;
  }

  public void init(BlockCapsule blockCap) {
    init(blockCap, false);
  }

  //pre transaction check
  public void init(BlockCapsule blockCap, boolean eventPluginLoaded) {
    txStartTimeInMs = System.currentTimeMillis();
    transactionContext = new TransactionContext(blockCap, tx, storeFactory, false, eventPluginLoaded);
  }

  public void exec() throws ContractExeException, ContractValidateException {
    //  VM execute
    runtime.execute(transactionContext);
    if (InternalTransaction.TxType.TX_PRECOMPILED_TYPE != txType) {
      if (System.currentTimeMillis() - txStartTimeInMs > DBConfig.getLongRunningTime()) {
        setTimeResultType(TimeResultType.LONG_RUNNING);
      }
    }
  }

  public void finalization() {
    if (StringUtils.isEmpty(transactionContext.getProgramResult().getRuntimeError())) {
      for (DataWord contract : transactionContext.getProgramResult().getDeleteAccounts()) {
        deleteContract(contract.getLast20Bytes());
      }
    }
  }

  public boolean checkNeedRetry() {
    if (!needVM()) {
      return false;
    }
    return false;
  }

  public void check() throws ReceiptCheckErrException {
    if (!needVM()) {
      return;
    }
    if (Objects.isNull(tx.getContractRet())) {
      throw new ReceiptCheckErrException("null resultCode");
    }
    if (!tx.getContractRet().equals(receipt.getResult())) {
      log.info("this tx id: {}, the resultCode in received block: {}, the resultCode in self: {}",
          Hex.toHexString(tx.getTransactionId().getBytes()), tx.getContractRet(), receipt.getResult());
      throw new ReceiptCheckErrException("Different resultCode");
    }
  }

  public ReceiptCapsule getReceipt() {
    return receipt;
  }

  public void setResult() {
    if (!needVM()) {
      return;
    }
    receipt.setResult(transactionContext.getProgramResult().getResultCode());
  }

  public String getRuntimeError() {
    return transactionContext.getProgramResult().getRuntimeError();
  }

  public ProgramResult getRuntimeResult() {
    return transactionContext.getProgramResult();
  }

  public Runtime getRuntime() {
    return runtime;
  }

  public void deleteContract(byte[] address) {
    codeStore.delete(address);
    accountStore.delete(address);
  }

  public enum TimeResultType {
    NORMAL,
    LONG_RUNNING,
  }
}
