package cn.ledgeryi.framework.core.exception;

import cn.ledgeryi.common.core.exception.LedgerYiException;

public class TraitorPeerException extends LedgerYiException {

  public TraitorPeerException() {
    super();
  }

  public TraitorPeerException(String message) {
    super(message);
  }

  public TraitorPeerException(String message, Throwable cause) {
    super(message, cause);
  }
}
