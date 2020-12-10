package cn.ledgeryi.framework.common.runtime;

import cn.ledgeryi.chainbase.actuator.Actuator;
import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.common.runtime.Runtime;
import cn.ledgeryi.chainbase.core.db.TransactionContext;
import cn.ledgeryi.common.core.exception.ContractExeException;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.framework.core.actuator.ActuatorCreator;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.protos.Protocol.Transaction.Result.contractResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

@Slf4j(topic = "VM")
public class RuntimeImpl implements Runtime {

  TransactionContext context;

  Manager dbManger;

  List<Actuator> actuatorList = null;

  public RuntimeImpl(Manager manager) {
    this.dbManger = manager;
  }

  @Override
  public void execute(TransactionContext context) throws ContractValidateException, ContractExeException {
    this.context = context;
    actuatorList = ActuatorCreator.getINSTANCE().createActuator(context.getTxCap());
    for (Actuator act : actuatorList) {
      act.validate();
      act.execute(context.getProgramResult().getRet());
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
      result.setResultCode(contractResult.SUCCESS);
      return;
    }
    result.setResultCode(contractResult.UNKNOWN);
  }

}

