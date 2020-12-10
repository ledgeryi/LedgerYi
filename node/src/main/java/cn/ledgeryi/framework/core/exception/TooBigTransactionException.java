package cn.ledgeryi.framework.core.exception;

import cn.ledgeryi.common.core.exception.LedgerYiException;

public class TooBigTransactionException extends LedgerYiException {

  public TooBigTransactionException() {
    super();
  }

  public TooBigTransactionException(String message) {
    super(message);
  }
}
