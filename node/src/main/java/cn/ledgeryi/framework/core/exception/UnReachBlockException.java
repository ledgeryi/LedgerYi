package cn.ledgeryi.framework.core.exception;

import cn.ledgeryi.common.core.exception.LedgerYiException;

public class UnReachBlockException extends LedgerYiException {

  public UnReachBlockException() {
    super();
  }

  public UnReachBlockException(String message) {
    super(message);
  }

  public UnReachBlockException(String message, Throwable cause) {
    super(message, cause);
  }
}
