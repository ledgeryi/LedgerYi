package cn.ledgeryi.chainbase.common.runtime;

import cn.ledgeryi.chainbase.core.db.TransactionContext;
import cn.ledgeryi.common.core.exception.ContractExeException;
import cn.ledgeryi.common.core.exception.ContractValidateException;


public interface Runtime {

  void execute(TransactionContext context) throws ContractValidateException, ContractExeException;

  ProgramResult getResult();
}
