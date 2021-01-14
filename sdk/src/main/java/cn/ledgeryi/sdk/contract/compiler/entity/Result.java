package cn.ledgeryi.sdk.contract.compiler.entity;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Result {
    public String errors;
    public String output;

    public Result(String errors, String output) {
        this.errors = errors;
        this.output = output;
    }

    public boolean isFailed() {
        return isNotBlank(errors);
    }
}
