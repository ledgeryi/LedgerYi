package cn.ledgeryi.framework.core.exception;

import cn.ledgeryi.common.core.exception.LedgerYiException;

public class AuthorizeException extends LedgerYiException {

    public AuthorizeException() {
        super();
    }

    public AuthorizeException(String message) {
        super(message);
    }
}
