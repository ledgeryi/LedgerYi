package cn.ledgeryi.framework.core.exception;

import cn.ledgeryi.common.core.exception.LedgerYiException;

public class TransactionExpirationException extends LedgerYiException {

  public TransactionExpirationException() {
    super();
  }

  public TransactionExpirationException(String message) {
    super(message);
  }

}