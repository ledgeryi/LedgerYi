package cn.ledgeryi.framework.core.exception;

import cn.ledgeryi.common.core.exception.LedgerYiException;

public class NonUniqueObjectException extends LedgerYiException {

  public NonUniqueObjectException() {
    super();
  }

  public NonUniqueObjectException(String s) {
    super(s);
  }

  public NonUniqueObjectException(String message, Throwable cause) {
    super(message, cause);
  }

  public NonUniqueObjectException(Throwable cause) {
    super("", cause);
  }
}
