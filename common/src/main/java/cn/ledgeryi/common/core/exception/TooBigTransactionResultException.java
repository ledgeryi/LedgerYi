package cn.ledgeryi.common.core.exception;

public class TooBigTransactionResultException extends LedgerYiException {

  public TooBigTransactionResultException() {
    super("too big transaction result");
  }

  public TooBigTransactionResultException(String message) {
    super(message);
  }
}
