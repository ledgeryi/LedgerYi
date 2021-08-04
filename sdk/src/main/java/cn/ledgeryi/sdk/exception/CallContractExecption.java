package cn.ledgeryi.sdk.exception;

/**
 * @author Brian
 * @date 2021/8/4 16:29
 */
public class CallContractExecption extends Exception {

    public CallContractExecption(String message) {
        super(message);
    }

    public CallContractExecption(Throwable cause) {
        super(cause);
    }

    public CallContractExecption(String message, Throwable cause) {
        super(message, cause);
    }
}
