package cn.ledgeryi.sdk.contract.compiler.entity;

public class Result {
    public String errors;
    public String output;
    private boolean success;

    public Result(String errors, String output, boolean success) {
        this.errors = errors;
        this.output = output;
        this.success = success;
    }

    public boolean isFailed() {
        return !success;
    }
}
