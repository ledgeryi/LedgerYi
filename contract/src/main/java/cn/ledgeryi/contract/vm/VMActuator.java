package cn.ledgeryi.contract.vm;

import cn.ledgeryi.chainbase.actuator.Actuator;
import cn.ledgeryi.chainbase.common.runtime.InternalTransaction;
import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.common.utils.ContractUtils;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.capsule.AccountCapsule;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.ContractCapsule;
import cn.ledgeryi.chainbase.core.db.TransactionContext;
import cn.ledgeryi.common.core.exception.ContractExeException;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.logsfilter.trigger.ContractTrigger;
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
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static cn.ledgeryi.contract.utils.MUtil.transfer;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

@Slf4j(topic = "VM")
public class VMActuator implements Actuator {

  private Protocol.Transaction tx;
  private BlockCapsule blockCap;
  private Repository repository;
  private InternalTransaction rootInternalTransaction;
  private ProgramInvokeFactory programInvokeFactory;


  private VM vm;
  private Program program;
  private VmConfig vmConfig = VmConfig.getInstance();

  @Getter
  @Setter
  private InternalTransaction.TxType txType;
  private InternalTransaction.ExecutorType executorType;

  @Getter
  @Setter
  private boolean isConstanCall = false;

  @Setter
  private boolean enableEventLinstener;

  private LogInfoTriggerParser logInfoTriggerParser;

  public VMActuator(boolean isConstanCall) {
    this.isConstanCall = isConstanCall;
    programInvokeFactory = new ProgramInvokeFactoryImpl();
  }

  private static long getEnergyFee(long callerEnergyUsage, long callerEnergyFrozen, long callerEnergyTotal) {
    if (callerEnergyTotal <= 0) {
      return 0;
    }
    return BigInteger.valueOf(callerEnergyFrozen).multiply(BigInteger.valueOf(callerEnergyUsage))
        .divide(BigInteger.valueOf(callerEnergyTotal)).longValueExact();
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
    //Route Type
    Protocol.Transaction.Contract.ContractType contractType = this.tx.getRawData().getContract().getType();
    //Prepare Repository
    repository = RepositoryImpl.createRoot(context.getStoreFactory());
    enableEventLinstener = context.isEventPluginLoaded();
    if (Objects.nonNull(blockCap)) {
      this.executorType = InternalTransaction.ExecutorType.ET_NORMAL_TYPE;
    } else {
      //打包交易或push交易时，blockCap为空，只为在打包交易时预执行交易
      this.blockCap = new BlockCapsule(Protocol.Block.newBuilder().build());
      this.executorType = InternalTransaction.ExecutorType.ET_PRE_TYPE;
    }
    //打包交易和处理区块时，为false
    if (isConstanCall) {
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
  public void execute(Object object) throws ContractExeException {
    TransactionContext context = (TransactionContext) object;
    if (Objects.isNull(context)){
      throw new RuntimeException("TransactionContext is null");
    }
    ProgramResult result = context.getProgramResult();
    try {
      if (vm != null) {
        if (null != blockCap && blockCap.generatedByMyself && blockCap.hasMasterSignature()
            && null != TransactionUtil.getContractRet(tx)
            && Protocol.Transaction.Result.ContractResult.OUT_OF_TIME == TransactionUtil.getContractRet(tx)) {
          result = program.getResult();
          program.spendAllEnergy();

          Program.OutOfTimeException e = Program.Exception.alreadyTimeOut();
          result.setRuntimeError(e.getMessage());
          result.setException(e);
          throw e;
        }

        vm.play(program);
        result = program.getResult();

        //打包交易和处理区块时为false
        if (isConstanCall) {
          long callValue = TransactionUtil.getCallValue(tx.getRawData().getContract());
          long callTokenValue = TransactionUtil.getCallTokenValue(tx.getRawData().getContract());
          if (callValue > 0 || callTokenValue > 0) {
            result.setRuntimeError("constant cannot set call value or call token value.");
            result.rejectInternalTransactions();
          }
          if (result.getException() != null) {
            result.setRuntimeError(result.getException().getMessage());
            result.rejectInternalTransactions();
          }
          context.setProgramResult(result);
          return;
        }

        if (InternalTransaction.TxType.TX_CONTRACT_CREATION_TYPE == txType && !result.isRevert()) {
          byte[] code = program.getResult().getHReturn();
          /*long saveCodeEnergy = (long) getLength(code) * EnergyCost.getInstance().getCREATE_DATA();
          long afterSpend = program.getEnergyLimitLeft().longValue() - saveCodeEnergy;
          if (afterSpend < 0) {
            if (null == result.getException()) {
              result.setException(Program.Exception
                  .notEnoughSpendEnergy("save just created contract code",
                      saveCodeEnergy, program.getEnergyLimitLeft().longValue()));
            }
          } else {
            result.spendEnergy(saveCodeEnergy);
            if (VmConfig.allowTvmConstantinople()) {
              repository.saveCode(program.getContractAddress().getNoLeadZeroesData(), code);
            }
          }*/
          result.spendEnergy(0/*saveCodeEnergy*/);
          if (VmConfig.allowTvmConstantinople()) {
            repository.saveCode(program.getContractAddress().getNoLeadZeroesData(), code);
          }
        }

        if (result.getException() != null || result.isRevert()) {
          result.getDeleteAccounts().clear();
          result.getLogInfoList().clear();
          result.resetFutureRefund();
          result.rejectInternalTransactions();
          if (result.getException() != null) {
            if (!(result.getException() instanceof Program.TransferException)) {
              program.spendAllEnergy();
            }
            result.setRuntimeError(result.getException().getMessage());
            throw result.getException();
          } else {
            result.setRuntimeError("REVERT opcode executed");
          }
        } else {
          repository.commit();
          if (logInfoTriggerParser != null) {
            List<ContractTrigger> triggers = logInfoTriggerParser.parseLogInfos(program.getResult().getLogInfoList(), repository);
            program.getResult().setTriggerList(triggers);
          }
        }
      } else {
        repository.commit();
      }
    } catch (Program.JVMStackOverFlowException e) {
      program.spendAllEnergy();
      result = program.getResult();
      result.setException(e);
      result.rejectInternalTransactions();
      result.setRuntimeError(result.getException().getMessage());
      log.info("JVMStackOverFlowException: {}", result.getException().getMessage());
    } catch (Program.OutOfTimeException e) {
      program.spendAllEnergy();
      result = program.getResult();
      result.setException(e);
      result.rejectInternalTransactions();
      result.setRuntimeError(result.getException().getMessage());
      log.info("timeout: {}", result.getException().getMessage());
    } catch (Throwable e) {
      if (!(e instanceof Program.TransferException)) {
        program.spendAllEnergy();
      }
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
      String traceContent = program.getTrace()
          .result(result.getHReturn())
          .error(result.getException())
          .toString();
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
    SmartContractOuterClass.CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(tx);
    if (contract == null) {
      throw new ContractValidateException("Cannot get CreateSmartContract from transaction");
    }
    //获取合约参数信息：contractName、originAddress、abi、consumeUserResourcePercent、originEnergyLimit、value
    SmartContractOuterClass.SmartContract newSmartContract = contract.getNewContract();
    if (!contract.getOwnerAddress().equals(newSmartContract.getOriginAddress())) {
      log.info("OwnerAddress not equals OriginAddress");
      throw new ContractValidateException("OwnerAddress is not equals OriginAddress");
    }
    byte[] contractName = newSmartContract.getName().getBytes();
    //合约名称字符长度校验，要求长度不大于32字节
    if (contractName.length > VMConstant.CONTRACT_NAME_LENGTH) {
      throw new ContractValidateException("contractName's length cannot be greater than 32");
    }
    long percent = contract.getNewContract().getConsumeUserResourcePercent();
    if (percent < 0 || percent > VMConstant.ONE_HUNDRED) {
      throw new ContractValidateException("percent must be >= 0 and <= 100");
    }
    //生成合约地址
    byte[] contractAddress = ContractUtils.generateContractAddress(tx);
    // insure the new contract address haven't exist
    if (repository.getAccount(contractAddress) != null) {
      throw new ContractValidateException("Trying to create a contract with existing contract address: "
              + DecodeUtil.createReadableString(contractAddress));
    }
    newSmartContract = newSmartContract.toBuilder().setContractAddress(ByteString.copyFrom(contractAddress)).build();
    long callValue = newSmartContract.getCallValue();
    long tokenValue = 0;
    long tokenId = 0;
    /*if (VmConfig.allowTvmTransferTrc10()) {
      tokenValue = contract.getCallTokenValue();
      tokenId = contract.getTokenId();
    }*/
    //获取调用者地址
    byte[] callerAddress = contract.getOwnerAddress().toByteArray();
    // create vm to constructor smart contract
    try {
      long feeLimit = tx.getRawData().getFeeLimit();
      if (feeLimit < 0 || feeLimit > VmConfig.MAX_FEE_LIMIT) {
        log.info("invalid feeLimit {}", feeLimit);
        throw new ContractValidateException("feeLimit must be >= 0 and <= " + VmConfig.MAX_FEE_LIMIT);
      }
      //查询创建合约的地址对应的账户
      AccountCapsule creator = this.repository.getAccount(newSmartContract.getOriginAddress().toByteArray());
      long energyLimit;
      // according to version
      if (VmConfig.getEnergyLimitHardFork()) {
        if (callValue < 0) {
          throw new ContractValidateException("callValue must be >= 0");
        }
        if (tokenValue < 0) {
          throw new ContractValidateException("tokenValue must be >= 0");
        }
        if (newSmartContract.getOriginEnergyLimit() <= 0) {
          throw new ContractValidateException("The originEnergyLimit must be > 0");
        }
        energyLimit = getAccountEnergyLimitWithFixRatio(creator, feeLimit, callValue);
      } else {
        energyLimit = 0;//getAccountEnergyLimitWithFloatRatio(creator, feeLimit, callValue);
      }
      checkTokenValueAndId(tokenValue, tokenId);
      //获取合约的操作字节码
      byte[] ops = newSmartContract.getBytecode().toByteArray();
      rootInternalTransaction = new InternalTransaction(tx, txType);
      long maxCpuTimeOfOneTx = repository.getDynamicPropertiesStore().getMaxCpuTimeOfOneTx() * VMConstant.ONE_THOUSAND;
      long thisTxCPULimitInUs = (long) (maxCpuTimeOfOneTx * getCpuLimitInUsRatio());
      long vmStartInUs = System.nanoTime() / VMConstant.ONE_THOUSAND;
      long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;
      ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(InternalTransaction.TxType.TX_CONTRACT_CREATION_TYPE, executorType, tx,
              tokenValue, tokenId, blockCap.getInstance(), repository, vmStartInUs, vmShouldEndInUs, energyLimit);
      this.vm = new VM();
      this.program = new Program(ops, programInvoke, rootInternalTransaction, vmConfig);
      byte[] txId = TransactionUtil.getTransactionId(tx).getBytes();
      this.program.setRootTransactionId(txId);
      if (enableEventLinstener && isCheckTransaction()) {
        logInfoTriggerParser = new LogInfoTriggerParser(blockCap.getNum(), blockCap.getTimeStamp(), txId, callerAddress);
      }
    } catch (Exception e) {
      log.info(e.getMessage());
      throw new ContractValidateException(e.getMessage());
    }
    program.getResult().setContractAddress(contractAddress);
    //创建合约账户，保存合约地址与合约名称的关系
    repository.createAccount(contractAddress, newSmartContract.getName(), Protocol.AccountType.Contract);
    //创建合约，保存合约地址与合约内容的关系，合约内容有SmartContract数据结构（smart_contract.proto）
    repository.createContract(contractAddress, new ContractCapsule(newSmartContract));
    byte[] code = newSmartContract.getBytecode().toByteArray();
    if (!VmConfig.allowTvmConstantinople()) {
      repository.saveCode(contractAddress, ProgramPrecompile.getCode(code));
    }
    // transfer from callerAddress to contractAddress according to callValue
    if (callValue > 0) {
      transfer(this.repository, callerAddress, contractAddress, callValue);
    }
    /*if (VmConfig.allowTvmTransferTrc10()) {
      if (tokenValue > 0) {
        transferToken(this.repository, callerAddress, contractAddress, String.valueOf(tokenId), tokenValue);
      }
    }*/

  }

  private void call() throws ContractValidateException {
    /*if (!repository.getDynamicPropertiesStore().supportVM()) {
      log.info("vm work is off, need to be opened by the committee");
      throw new ContractValidateException("VM work is off, need to be opened by the committee");
    }*/
    SmartContractOuterClass.TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(tx);
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

    long callValue = contract.getCallValue();
    long tokenValue = 0;
    long tokenId = 0;
    /*if (VmConfig.allowTvmTransferTrc10()) {
      tokenValue = contract.getCallTokenValue();
      tokenId = contract.getTokenId();
    }*/

    /*if (VmConfig.getEnergyLimitHardFork()) {
      if (callValue < 0) {
        throw new ContractValidateException("callValue must be >= 0");
      }
      if (tokenValue < 0) {
        throw new ContractValidateException("tokenValue must be >= 0");
      }
    }*/
    byte[] callerAddress = contract.getOwnerAddress().toByteArray();
    checkTokenValueAndId(tokenValue, tokenId);
    byte[] code = repository.getCode(contractAddress);
    if (isNotEmpty(code)) {
      long feeLimit = tx.getRawData().getFeeLimit();
      if (feeLimit < 0 || feeLimit > VmConfig.MAX_FEE_LIMIT) {
        log.info("invalid feeLimit {}", feeLimit);
        throw new ContractValidateException("feeLimit must be >= 0 and <= " + VmConfig.MAX_FEE_LIMIT);
      }
      AccountCapsule caller = repository.getAccount(callerAddress);
      long energyLimit;
      if (isConstanCall) {
        energyLimit = VMConstant.ENERGY_LIMIT_IN_CONSTANT_TX;
      } else {
        AccountCapsule creator = repository.getAccount(deployedContract.getInstance().getOriginAddress().toByteArray());
        energyLimit = getTotalEnergyLimit(creator, caller, contract, feeLimit, callValue);
      }
      long maxCpuTimeOfOneTx = repository.getDynamicPropertiesStore().getMaxCpuTimeOfOneTx() * VMConstant.ONE_THOUSAND;
      long thisTxCPULimitInUs = (long) (maxCpuTimeOfOneTx * getCpuLimitInUsRatio());
      long vmStartInUs = System.nanoTime() / VMConstant.ONE_THOUSAND;
      long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;
      ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(InternalTransaction.TxType.TX_CONTRACT_CALL_TYPE,
              executorType, tx, tokenValue, tokenId, blockCap.getInstance(), repository, vmStartInUs, vmShouldEndInUs, energyLimit);
      if (isConstanCall) {
        programInvoke.setConstantCall();
      }
      this.vm = new VM();
      rootInternalTransaction = new InternalTransaction(tx, txType);
      this.program = new Program(code, programInvoke, rootInternalTransaction, vmConfig);
      byte[] txId = TransactionUtil.getTransactionId(tx).getBytes();
      this.program.setRootTransactionId(txId);
      if (enableEventLinstener && isCheckTransaction()) {
        logInfoTriggerParser = new LogInfoTriggerParser(blockCap.getNum(), blockCap.getTimeStamp(), txId, callerAddress);
      }
    }

    program.getResult().setContractAddress(contractAddress);
    //transfer from callerAddress to targetAddress according to callValue

    if (callValue > 0) {
      transfer(this.repository, callerAddress, contractAddress, callValue);
    }
    /*if (VmConfig.allowTvmTransferTrc10()) {
      if (tokenValue > 0) {
        transferToken(this.repository, callerAddress, contractAddress, String.valueOf(tokenId), tokenValue);
      }
    }*/
  }

  public long getAccountEnergyLimitWithFixRatio(AccountCapsule account, long feeLimit, long callValue) {
    long sunPerEnergy = VMConstant.SUN_PER_ENERGY;
    if (repository.getDynamicPropertiesStore().getEnergyFee() > 0) {
      sunPerEnergy = repository.getDynamicPropertiesStore().getEnergyFee();
    }
    long leftFrozenEnergy = repository.getAccountLeftEnergyFromFreeze(account);
    long energyFromBalance = max(account.getBalance() - callValue, 0) / sunPerEnergy;
    long availableEnergy = Math.addExact(leftFrozenEnergy, energyFromBalance);
    long energyFromFeeLimit = feeLimit / sunPerEnergy;
    return min(availableEnergy, energyFromFeeLimit);
  }

  private long getAccountEnergyLimitWithFloatRatio(AccountCapsule account, long feeLimit, long callValue) {
    long sunPerEnergy = VMConstant.SUN_PER_ENERGY;
    if (repository.getDynamicPropertiesStore().getEnergyFee() > 0) {
      sunPerEnergy = repository.getDynamicPropertiesStore().getEnergyFee();
    }
    // can change the calc way
    long leftEnergyFromFreeze = repository.getAccountLeftEnergyFromFreeze(account);
    callValue = max(callValue, 0);
    long energyFromBalance = Math.floorDiv(max(account.getBalance() - callValue, 0), sunPerEnergy);
    long energyFromFeeLimit;
    long totalBalanceForEnergyFreeze = 0;//account.getAllFrozenBalanceForEnergy();
    if (0 == totalBalanceForEnergyFreeze) {
      energyFromFeeLimit = feeLimit / sunPerEnergy;
    } else {
      long totalEnergyFromFreeze = repository.calculateGlobalEnergyLimit(account);
      long leftBalanceForEnergyFreeze = getEnergyFee(totalBalanceForEnergyFreeze,
          leftEnergyFromFreeze, totalEnergyFromFreeze);
      if (leftBalanceForEnergyFreeze >= feeLimit) {
        energyFromFeeLimit = BigInteger.valueOf(totalEnergyFromFreeze)
            .multiply(BigInteger.valueOf(feeLimit))
            .divide(BigInteger.valueOf(totalBalanceForEnergyFreeze)).longValueExact();
      } else {
        energyFromFeeLimit = Math.addExact(leftEnergyFromFreeze,
                (feeLimit - leftBalanceForEnergyFreeze) / sunPerEnergy);
      }
    }
    return min(Math.addExact(leftEnergyFromFreeze, energyFromBalance), energyFromFeeLimit);
  }

  public long getTotalEnergyLimit(AccountCapsule creator, AccountCapsule caller,
                                  SmartContractOuterClass.TriggerSmartContract contract, long feeLimit, long callValue)
      throws ContractValidateException {
    if (Objects.isNull(creator) && VmConfig.allowTvmConstantinople()) {
      return getAccountEnergyLimitWithFixRatio(caller, feeLimit, callValue);
    }
    //  according to version
    if (VmConfig.getEnergyLimitHardFork()) {
      return getTotalEnergyLimitWithFixRatio(creator, caller, contract, feeLimit, callValue);
    } else {
      return getTotalEnergyLimitWithFloatRatio(creator, caller, contract, feeLimit, callValue);
    }
  }


  public void checkTokenValueAndId(long tokenValue, long tokenId) throws ContractValidateException {
    /*if (VmConfig.allowTvmTransferTrc10()) {
      if (VmConfig.allowMultiSign()) { //allowMultiSigns
        // tokenid can only be 0
        // or (MIN_TOKEN_ID, Long.Max]
        if (tokenId <= VMConstant.MIN_TOKEN_ID && tokenId != 0) {
          throw new ContractValidateException("tokenId must be > " + VMConstant.MIN_TOKEN_ID);
        }
        // tokenid can only be 0 when tokenvalue = 0,
        // or (MIN_TOKEN_ID, Long.Max]
        if (tokenValue > 0 && tokenId == 0) {
          throw new ContractValidateException("invalid arguments with tokenValue = " + tokenValue + ", tokenId = " + tokenId);
        }
      }
    }*/
  }

  private double getCpuLimitInUsRatio() {
    double cpuLimitRatio;
    if (InternalTransaction.ExecutorType.ET_NORMAL_TYPE == executorType) {
      // self witness generates block
      if (this.blockCap != null && blockCap.generatedByMyself &&
          !this.blockCap.hasMasterSignature()) {
        cpuLimitRatio = 1.0;
      } else {
        // self witness or other witness or fullnode verifies block
        if (tx.getRet(0).getContractRet() == Protocol.Transaction.Result.ContractResult.OUT_OF_TIME) {
          cpuLimitRatio = DBConfig.getMinTimeRatio();
        } else {
          cpuLimitRatio = DBConfig.getMaxTimeRatio();
        }
      }
    } else {
      // self witness or other witness or fullnode receives tx
      cpuLimitRatio = 1.0;
    }
    return cpuLimitRatio;
  }

  public long getTotalEnergyLimitWithFixRatio(AccountCapsule creator, AccountCapsule caller,
                                              SmartContractOuterClass.TriggerSmartContract contract, long feeLimit, long callValue)
      throws ContractValidateException {
    long callerEnergyLimit = getAccountEnergyLimitWithFixRatio(caller, feeLimit, callValue);
    if (Arrays.equals(creator.getAddress().toByteArray(), caller.getAddress().toByteArray())) {
      // when the creator calls his own contract, this logic will be used.
      // so, the creator must use a BIG feeLimit to call his own contract,
      // which will cost the feeLimit tx when the creator's frozen energy is 0.
      return callerEnergyLimit;
    }
    long creatorEnergyLimit = 0;
    ContractCapsule contractCapsule = repository.getContract(contract.getContractAddress().toByteArray());
    long consumeUserResourcePercent = contractCapsule.getConsumeUserResourcePercent();
    long originEnergyLimit = contractCapsule.getOriginEnergyLimit();
    if (originEnergyLimit < 0) {
      throw new ContractValidateException("originEnergyLimit can't be < 0");
    }
    if (consumeUserResourcePercent <= 0) {
      creatorEnergyLimit = min(repository.getAccountLeftEnergyFromFreeze(creator), originEnergyLimit);
    } else {
      if (consumeUserResourcePercent < VMConstant.ONE_HUNDRED) {
        // creatorEnergyLimit =
        // min(callerEnergyLimit * (100 - percent) / percent, creatorLeftFrozenEnergy, originEnergyLimit)
        creatorEnergyLimit = min(
            BigInteger.valueOf(callerEnergyLimit)
                .multiply(BigInteger.valueOf(VMConstant.ONE_HUNDRED - consumeUserResourcePercent))
                .divide(BigInteger.valueOf(consumeUserResourcePercent)).longValueExact(),
            min(repository.getAccountLeftEnergyFromFreeze(creator), originEnergyLimit)
        );
      }
    }
    return Math.addExact(callerEnergyLimit, creatorEnergyLimit);
  }

  private long getTotalEnergyLimitWithFloatRatio(AccountCapsule creator, AccountCapsule caller,
                                                 SmartContractOuterClass.TriggerSmartContract contract, long feeLimit, long callValue) {
    long callerEnergyLimit = getAccountEnergyLimitWithFloatRatio(caller, feeLimit, callValue);
    if (Arrays.equals(creator.getAddress().toByteArray(), caller.getAddress().toByteArray())) {
      return callerEnergyLimit;
    }

    // creatorEnergyFromFreeze
    long creatorEnergyLimit = repository.getAccountLeftEnergyFromFreeze(creator);
    ContractCapsule contractCapsule = repository.getContract(contract.getContractAddress().toByteArray());
    long consumeUserResourcePercent = contractCapsule.getConsumeUserResourcePercent();
    if (creatorEnergyLimit * consumeUserResourcePercent
        > (VMConstant.ONE_HUNDRED - consumeUserResourcePercent) * callerEnergyLimit) {
      return Math.floorDiv(callerEnergyLimit * VMConstant.ONE_HUNDRED, consumeUserResourcePercent);
    } else {
      return Math.addExact(callerEnergyLimit, creatorEnergyLimit);
    }
  }

  private boolean isCheckTransaction() {
    return this.blockCap != null && !this.blockCap.getInstance().getBlockHeader().getMasterSignature().isEmpty();
  }
}
