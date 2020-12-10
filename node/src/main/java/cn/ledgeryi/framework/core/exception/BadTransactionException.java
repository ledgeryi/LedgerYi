package cn.ledgeryi.framework.core.exception;

import cn.ledgeryi.common.core.exception.LedgerYiException;

public class BadTransactionException extends LedgerYiException {

  public BadTransactionException() {
    super();
  }

  public BadTransactionException(String message) {
    super(message);
  }

  public BadTransactionException(String message, Throwable cause) {
    super(message, cause);
  }
}
