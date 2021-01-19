package cn.ledgeryi.sdk.exception;

public class CreateContractExecption extends Exception {

    public CreateContractExecption(String message) {
        super(message);
    }

    public CreateContractExecption(Throwable cause) {
        super(cause);
    }

    public CreateContractExecption(String message, Throwable cause) {
        super(message, cause);
    }

}
