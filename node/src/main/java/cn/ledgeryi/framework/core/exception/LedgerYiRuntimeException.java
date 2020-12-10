package cn.ledgeryi.framework.core.exception;

public class LedgerYiRuntimeException extends RuntimeException {

  public LedgerYiRuntimeException() {
    super();
  }

  public LedgerYiRuntimeException(String message) {
    super(message);
  }

  public LedgerYiRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public LedgerYiRuntimeException(Throwable cause) {
    super(cause);
  }

  protected LedgerYiRuntimeException(String message, Throwable cause,
                                      boolean enableSuppression,
                                      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }


}
