package cn.ledgeryi.contract.vm;

import cn.ledgeryi.chainbase.actuator.VmActuator;
import cn.ledgeryi.chainbase.common.runtime.InternalTransaction;
import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.common.utils.ContractUtils;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.ContractCapsule;
import cn.ledgeryi.chainbase.core.db.TransactionContext;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.contract.utils.TransactionUtil;
import cn.ledgeryi.contract.vm.config.ConfigLoader;
import cn.ledgeryi.contract.vm.config.VmConfig;
import cn.ledgeryi.contract.vm.program.Program;
import cn.ledgeryi.contract.vm.program.ProgramPrecompile;
import cn.ledgeryi.contract.vm.program.invoke.ProgramInvoke;
import cn.ledgeryi.contract.vm.program.invoke.ProgramInvokeFactory;
import cn.ledgeryi.contract.vm.program.invoke.ProgramInvokeFactoryImpl;
import cn.ledgeryi.contract.vm.repository.Repository;
import cn.ledgeryi.contract.vm.repository.RepositoryImpl;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.CreateSmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.util.Objects;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

@Slf4j(topic = "VM")
public class LedgerYiVmActuator implements VmActuator {

  private VM vm;
  private Program program;
  private BlockCapsule blockCap;
  private Repository repository;
  private Protocol.Transaction tx;
  private ProgramInvokeFactory programInvokeFactory;
  private VmConfig vmConfig = VmConfig.getInstance();
  private InternalTransaction rootInternalTransaction;
  private InternalTransaction.ExecutorType executorType;

  @Getter
  @Setter
  private InternalTransaction.TxType txType;

  @Getter
  @Setter
  private boolean isConstantCall;

  public LedgerYiVmActuator(boolean isConstantCall) {
    this.isConstantCall = isConstantCall;
    programInvokeFactory = new ProgramInvokeFactoryImpl();
  }

  @Override
  public void validate(Object object) throws ContractValidateException {
    TransactionContext context = (TransactionContext) object;
    if (Objects.isNull(context)){
      throw new RuntimeException("TransactionContext is null");
    }

    //Load Config
    ConfigLoader.load(context.getStoreFactory());
    tx = context.getTxCap().getInstance();
    blockCap = context.getBlockCap();
    Protocol.Transaction.Contract.ContractType contractType = this.tx.getRawData().getContract().getType();
    //Prepare Repository
    repository = RepositoryImpl.createRoot(context.getStoreFactory());
    if (Objects.nonNull(blockCap)) {
      this.executorType = InternalTransaction.ExecutorType.ET_NORMAL_TYPE;
    } else {
      this.blockCap = new BlockCapsule(Protocol.Block.newBuilder().build());
      this.executorType = InternalTransaction.ExecutorType.ET_PRE_TYPE;
    }
    if (isConstantCall) {
      this.executorType = InternalTransaction.ExecutorType.ET_PRE_TYPE;
    }

    switch (contractType.getNumber()) {
      case Protocol.Transaction.Contract.ContractType.TriggerSmartContract_VALUE:
        txType = InternalTransaction.TxType.TX_CONTRACT_CALL_TYPE;
        call();
        break;
      case Protocol.Transaction.Contract.ContractType.CreateSmartContract_VALUE:
        txType = InternalTransaction.TxType.TX_CONTRACT_CREATION_TYPE;
        create();
        break;
      default:
        throw new ContractValidateException("Unknown contract type");
    }
  }

  @Override
  public void execute(Object object) {
    TransactionContext context = (TransactionContext) object;
    if (Objects.isNull(context)){
      throw new RuntimeException("TransactionContext is null");
    }
    ProgramResult result = context.getProgramResult();
    try {
      if (vm != null) {
        vm.play(program);
        result = program.getResult();

        long cpuTimeUsed = program.getCpuTimeUsed();
        result.setCpuTimeUsed(cpuTimeUsed);
        repository.putCpuTimeUsedValue(program.getContractAddress().getNoLeadZeroesData(), cpuTimeUsed);
        long storageUsed = program.getStorageUsed();
        result.setStorageUsed(storageUsed);
        repository.putStorageUsedValue(program.getContractAddress().getNoLeadZeroesData(), storageUsed);

        if (isConstantCall) {
          if (result.getException() != null) {
            result.setRuntimeError(result.getException().getMessage());
            result.rejectInternalTransactions();
          }
          context.setProgramResult(result);
          return;
        }

        if (InternalTransaction.TxType.TX_CONTRACT_CREATION_TYPE == txType && !result.isRevert() && isCheckTransaction()) {
          byte[] code = program.getResult().getHReturn();
          repository.saveCode(program.getContractAddress().getNoLeadZeroesData(), code);
        }

        if (result.getException() != null || result.isRevert()) {
          result.getDeleteAccounts().clear();
          result.getLogInfoList().clear();
          result.rejectInternalTransactions();
          if (result.getException() != null) {
            result.setRuntimeError(result.getException().getMessage());
            throw result.getException();
          } else {
            result.setRuntimeError("REVERT opcode executed");
          }
        } else if(isCheckTransaction()) {
          repository.commit();
        }
      }
    } catch (Program.JVMStackOverFlowException e) {
      result = program.getResult();
      result.setException(e);
      result.rejectInternalTransactions();
      result.setRuntimeError(result.getException().getMessage());
      log.info("jvm stack overflow exception: {}", result.getException().getMessage());
    } catch (Throwable e) {
      result = program.getResult();
      result.rejectInternalTransactions();
      if (Objects.isNull(result.getException())) {
        log.error(e.getMessage(), e);
        result.setException(new RuntimeException("Unknown Throwable"));
      }
      if (StringUtils.isEmpty(result.getRuntimeError())) {
        result.setRuntimeError(result.getException().getMessage());
      }
      log.info("runtime result is :{}", result.getException().getMessage());
    }

    //use program returned fill context
    context.setProgramResult(result);

    if (VmConfig.vmTrace() && program != null) {
      String traceContent = program.getTrace().result(result.getHReturn()).error(result.getException()).toString();
      if (VmConfig.vmTraceCompressed()) {
        traceContent = VMUtils.zipAndEncode(traceContent);
      }
      String txHash = Hex.toHexString(rootInternalTransaction.getHash());
      VMUtils.saveProgramTraceFile(txHash, traceContent);
    }
  }

  private void create() throws ContractValidateException {
    if (!repository.getDynamicPropertiesStore().supportVM()) {
      throw new ContractValidateException("vm work is off, need to be opened by the committee");
    }
    CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(tx);
    if (contract == null) {
      throw new ContractValidateException("Cannot get CreateSmartContract from transaction");
    }
    SmartContract newSmartContract = contract.getNewContract();
    if (!contract.getOwnerAddress().equals(newSmartContract.getOriginAddress())) {
      log.info("OwnerAddress not equals OriginAddress");
      throw new ContractValidateException("OwnerAddress is not equals OriginAddress");
    }
    byte[] contractName = newSmartContract.getName().getBytes();
    if (contractName.length > VMConstant.CONTRACT_NAME_LENGTH) {
      throw new ContractValidateException("contractName's length cannot be greater than 32");
    }
    long percent = contract.getNewContract().getConsumeUserResourcePercent();
    if (percent < 0 || percent > VMConstant.ONE_HUNDRED) {
      throw new ContractValidateException("percent must be >= 0 and <= 100");
    }
    byte[] contractAddress = ContractUtils.generateContractAddress(tx);
    // insure the new contract address haven't exist
    if (repository.getAccount(contractAddress) != null) {
      throw new ContractValidateException("Trying to create a contract with existing contract address: "
              + DecodeUtil.createReadableString(contractAddress));
    }
    newSmartContract = newSmartContract.toBuilder().setContractAddress(ByteString.copyFrom(contractAddress)).build();
    // create vm to constructor smart contract
    try {
      long feeLimit = tx.getRawData().getFeeLimit();
      if (feeLimit < 0 || feeLimit > VmConfig.MAX_FEE_LIMIT) {
        log.info("invalid feeLimit {}", feeLimit);
        throw new ContractValidateException("feeLimit must be >= 0 and <= " + VmConfig.MAX_FEE_LIMIT);
      }
      byte[] ops = newSmartContract.getBytecode().toByteArray();
      rootInternalTransaction = new InternalTransaction(tx, txType);
      ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(InternalTransaction.TxType.TX_CONTRACT_CREATION_TYPE, executorType, tx,
              blockCap.getInstance(), repository, 0, 0);
      this.vm = new VM();
      this.program = new Program(ops, programInvoke, rootInternalTransaction, vmConfig, isCheckTransaction());
      byte[] txId = TransactionUtil.getTransactionId(tx).getBytes();
      this.program.setRootTransactionId(txId);
    } catch (Exception e) {
      log.info(e.getMessage());
      throw new ContractValidateException(e.getMessage());
    }
    program.getResult().setContractAddress(contractAddress);
    if (isCheckTransaction()) {
      repository.createAccount(contractAddress, newSmartContract.getName(), Protocol.AccountType.Contract);
      repository.createContract(contractAddress, new ContractCapsule(newSmartContract));
      byte[] code = newSmartContract.getBytecode().toByteArray();
      repository.saveCode(contractAddress, ProgramPrecompile.getCode(code));
    }
  }

  private void call() throws ContractValidateException {
    TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(tx);
    if (contract == null) {
      return;
    }
    if (contract.getContractAddress() == null) {
      throw new ContractValidateException("Cannot get contract address from TriggerContract");
    }
    byte[] contractAddress = contract.getContractAddress().toByteArray();
    ContractCapsule deployedContract = repository.getContract(contractAddress);
    if (null == deployedContract) {
      log.info("No contract or not a smart contract");
      throw new ContractValidateException("No contract or not a smart contract");
    }
    byte[] code = repository.getCode(contractAddress);
    if (isNotEmpty(code)) {
      ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(InternalTransaction.TxType.TX_CONTRACT_CALL_TYPE,
              executorType, tx, blockCap.getInstance(), repository, 0, 0);
      if (isConstantCall) {
        programInvoke.setConstantCall();
      }
      this.vm = new VM();
      rootInternalTransaction = new InternalTransaction(tx, txType);
      this.program = new Program(code, programInvoke, rootInternalTransaction, vmConfig, isCheckTransaction());
      byte[] txId = TransactionUtil.getTransactionId(tx).getBytes();
      this.program.setRootTransactionId(txId);
    }
    program.getResult().setContractAddress(contractAddress);
  }

  private boolean isCheckTransaction() {
    return this.blockCap != null && !this.blockCap.getInstance().getBlockHeader().getMasterSignature().isEmpty();
  }
}
