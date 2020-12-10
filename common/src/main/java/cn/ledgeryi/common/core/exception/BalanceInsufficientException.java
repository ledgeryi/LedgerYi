package cn.ledgeryi.common.core.exception;

public class BalanceInsufficientException extends LedgerYiException {

  public BalanceInsufficientException() {
    super();
  }

  public BalanceInsufficientException(String message) {
    super(message);
  }
}
