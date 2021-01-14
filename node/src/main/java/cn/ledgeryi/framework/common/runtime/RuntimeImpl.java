package cn.ledgeryi.framework.common.runtime;

import cn.ledgeryi.chainbase.actuator.Actuator;
import cn.ledgeryi.chainbase.actuator.VmActuator;
import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.common.runtime.Runtime;
import cn.ledgeryi.chainbase.core.db.TransactionContext;
import cn.ledgeryi.common.core.exception.ContractExeException;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.contract.vm.LedgerYiVmActuator;
import cn.ledgeryi.contract.vm.program.Program;
import cn.ledgeryi.framework.core.actuator.ActuatorCreator;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;
import cn.ledgeryi.protos.Protocol.Transaction.Result.ContractResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

@Slf4j(topic = "VM")
public class RuntimeImpl implements Runtime {

  Manager dbManger;

  VmActuator vmActuator = null;

  TransactionContext context;

  List<Actuator> actuatorList = null;

  public RuntimeImpl(Manager manager) {
    this.dbManger = manager;
  }

  @Override
  public void execute(TransactionContext context) throws ContractValidateException, ContractExeException {
    this.context = context;
    ContractType contractType = context.getTxCap().getInstance().getRawData().getContract().getType();
        switch (contractType.getNumber()) {
          case ContractType.TriggerSmartContract_VALUE:
          case ContractType.CreateSmartContract_VALUE:
            vmActuator = new LedgerYiVmActuator(context.isStatic());
        break;
      default:
        // ContractType.ClearABIContract
        actuatorList = ActuatorCreator.getINSTANCE().createActuator(context.getTxCap());
        break;
    }
    if (vmActuator != null) {
      vmActuator.validate(context);
      vmActuator.execute(context);
    } else {
      for (Actuator act : actuatorList) {
        act.validate();
        act.execute(context.getProgramResult().getRet());
      }
    }
    setResultCode(context.getProgramResult());
  }

  @Override
  public ProgramResult getResult() {
    return context.getProgramResult();
  }

  private void setResultCode(ProgramResult result) {
    RuntimeException exception = result.getException();
    if (Objects.isNull(exception) && StringUtils.isEmpty(result.getRuntimeError())) {
      result.setResultCode(ContractResult.SUCCESS);
      return;
    }
    if (result.isRevert()) {
      result.setResultCode(ContractResult.REVERT);
      return;
    }
    if (exception instanceof Program.IllegalOperationException) {
      result.setResultCode(ContractResult.ILLEGAL_OPERATION);
      return;
    }
    if (exception instanceof Program.BadJumpDestinationException) {
      result.setResultCode(ContractResult.BAD_JUMP_DESTINATION);
      return;
    }
    if (exception instanceof Program.StackTooSmallException) {
      result.setResultCode(ContractResult.STACK_TOO_SMALL);
      return;
    }
    if (exception instanceof Program.StackTooLargeException) {
      result.setResultCode(ContractResult.STACK_TOO_LARGE);
      return;
    }
    if (exception instanceof Program.JVMStackOverFlowException) {
      result.setResultCode(ContractResult.JVM_STACK_OVER_FLOW);
      return;
    }
    log.info("uncaught exception", exception);
    result.setResultCode(ContractResult.UNKNOWN);
  }

}

