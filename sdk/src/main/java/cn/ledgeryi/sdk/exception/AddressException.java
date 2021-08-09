package cn.ledgeryi.sdk.exception;

/**
 * @author Brian
 * @date 2021/8/9 18:04
 */
public class AddressException extends Exception {

    public AddressException(String message) {
        super(message);
    }

    public AddressException(Throwable cause) {
        super(cause);
    }

    public AddressException(String message, Throwable cause) {
        super(message, cause);
    }
}
