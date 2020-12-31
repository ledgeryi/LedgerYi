package cn.ledgeryi.chainbase.actuator;

import cn.ledgeryi.common.core.exception.ContractExeException;
import cn.ledgeryi.common.core.exception.ContractValidateException;

public interface Actuator {

  void execute(Object object) throws ContractExeException;

  void validate(Object object) throws ContractValidateException;
}