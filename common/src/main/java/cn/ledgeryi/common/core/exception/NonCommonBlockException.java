package cn.ledgeryi.common.core.exception;

public class NonCommonBlockException extends LedgerYiException {

  public NonCommonBlockException() {
    super();
  }

  public NonCommonBlockException(String message) {
    super(message);
  }

  public NonCommonBlockException(String message, Throwable cause) {
    super(message, cause);
  }
}
