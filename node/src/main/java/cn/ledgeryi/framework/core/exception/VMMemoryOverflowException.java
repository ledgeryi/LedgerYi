package cn.ledgeryi.framework.core.exception;

import cn.ledgeryi.common.core.exception.LedgerYiException;

public class VMMemoryOverflowException extends LedgerYiException {

  public VMMemoryOverflowException() {
    super("VM memory overflow");
  }

  public VMMemoryOverflowException(String message) {
    super(message);
  }

}
