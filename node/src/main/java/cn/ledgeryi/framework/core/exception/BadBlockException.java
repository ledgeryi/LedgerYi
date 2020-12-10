package cn.ledgeryi.framework.core.exception;

import cn.ledgeryi.common.core.exception.LedgerYiException;

public class BadBlockException extends LedgerYiException {

  public BadBlockException() {
    super();
  }

  public BadBlockException(String message) {
    super(message);
  }
}
