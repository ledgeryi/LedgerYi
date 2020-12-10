package cn.ledgeryi.framework.core.exception;

import cn.ledgeryi.common.core.exception.ContractValidateException;

public class ContractSizeNotEqualToOneException extends ContractValidateException {

  public ContractSizeNotEqualToOneException() {
    super();
  }

  public ContractSizeNotEqualToOneException(String message) {
    super(message);
  }
}
