package cn.ledgeryi.chainbase.core.db;

import cn.ledgeryi.chainbase.common.runtime.InternalTransaction;
import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.common.runtime.Runtime;
import cn.ledgeryi.chainbase.common.utils.ContractUtils;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.common.utils.ForkUtils;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.ContractCapsule;
import cn.ledgeryi.chainbase.core.capsule.ReceiptCapsule;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.chainbase.core.store.*;
import cn.ledgeryi.common.core.exception.ContractExeException;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.core.exception.ReceiptCheckErrException;
import cn.ledgeryi.common.core.exception.VMIllegalException;
import cn.ledgeryi.common.runtime.vm.DataWord;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.Transaction.Result.ContractResult;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;

import java.util.Objects;

import static cn.ledgeryi.chainbase.common.runtime.InternalTransaction.TxType.*;

@Slf4j(topic = "TransactionTrace")
public class TransactionTrace {

  private TransactionCapsule tx;
  private ReceiptCapsule receipt;
  private StoreFactory storeFactory;
  private DynamicPropertiesStore dynamicPropertiesStore;
  private AccountStore accountStore;
  private CodeStore codeStore;
  private ContractStore contractStore;
  private InternalTransaction.TxType txType;
  private Runtime runtime;
  private ForkUtils forkUtils;
  @Getter
  private TransactionContext transactionContext;

  public TransactionTrace(TransactionCapsule tx, StoreFactory storeFactory, Runtime runtime) {
    this.tx = tx;
    Protocol.Transaction.Contract.ContractType contractType = this.tx.getInstance().getRawData().getContract().getType();
    switch (contractType.getNumber()) {
      case Protocol.Transaction.Contract.ContractType.TriggerSmartContract_VALUE:
        txType = TX_CONTRACT_CALL_TYPE;
        break;
      case Protocol.Transaction.Contract.ContractType.CreateSmartContract_VALUE:
        txType = TX_CONTRACT_CREATION_TYPE;
        break;
      default:
        txType = TX_PRECOMPILED_TYPE;
    }

    this.storeFactory = storeFactory;
    this.dynamicPropertiesStore = storeFactory.getChainBaseManager().getDynamicPropertiesStore();
    this.contractStore = storeFactory.getChainBaseManager().getContractStore();
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

  //pre transaction check
  public void init(BlockCapsule blockCap) {
    transactionContext = new TransactionContext(blockCap, tx, storeFactory, false);
  }

  public void checkIsConstant() throws ContractValidateException, VMIllegalException {
    TriggerSmartContract triggerContract = ContractCapsule.getTriggerContractFromTransaction(this.getTx().getInstance());
    if (TX_CONTRACT_CALL_TYPE == this.txType) {
      ContractCapsule contract = contractStore.get(triggerContract.getContractAddress().toByteArray());
      if (contract == null) {
        log.info("contract: {} is not in contract store",
                DecodeUtil.createReadableString(triggerContract.getContractAddress().toByteArray()));
        throw new ContractValidateException("contract: " +
                DecodeUtil.createReadableString(triggerContract.getContractAddress().toByteArray())
                + " is not in contract store");
      }
      SmartContractOuterClass.SmartContract.ABI abi = contract.getInstance().getAbi();
      if (ContractUtils.isConstant(abi, triggerContract)) {
        throw new VMIllegalException("cannot call constant method");
      }
    }
  }

  public void exec() throws ContractExeException, ContractValidateException {
    runtime.execute(transactionContext);
  }

  public void finalization() {
    if (StringUtils.isEmpty(transactionContext.getProgramResult().getRuntimeError())) {
      for (DataWord contract : transactionContext.getProgramResult().getDeleteAccounts()) {
        deleteContract(contract.getLast20Bytes());
      }
    }
  }

  public void check() throws ReceiptCheckErrException {
    if (!needVM()) {
      return;
    }
    if (Objects.isNull(tx.getContractRet())) {
      throw new ReceiptCheckErrException("null resultCode");
    }
    //Verify that the local execution results are consistent with the received transaction results
    if (!tx.getContractRet().equals(receipt.getResult())) {
      log.info("this tx id: {}, the resultCode in received block: {}, the resultCode in myself: {}",
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
    receipt.setResult(transactionContext.getProgramResult());
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
    OUT_OF_TIME
  }
}
