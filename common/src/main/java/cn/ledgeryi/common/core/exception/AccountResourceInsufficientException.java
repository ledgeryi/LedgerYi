package cn.ledgeryi.common.core.exception;

public class AccountResourceInsufficientException extends LedgerYiException {

  public AccountResourceInsufficientException() {
    super("Insufficient bandwidth and balance to create new account");
  }

  public AccountResourceInsufficientException(String message) {
    super(message);
  }
}

