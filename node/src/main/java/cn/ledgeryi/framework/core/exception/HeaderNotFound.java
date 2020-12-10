package cn.ledgeryi.framework.core.exception;

import cn.ledgeryi.common.core.exception.StoreException;

public class HeaderNotFound extends StoreException {

  public HeaderNotFound() {
    super();
  }

  public HeaderNotFound(String message) {
    super(message);
  }
}
