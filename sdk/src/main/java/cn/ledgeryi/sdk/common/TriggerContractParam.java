package cn.ledgeryi.sdk.common;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class TriggerContractParam {
    @NonNull
    private byte[] data;
    @NonNull
    private long callValue;
    @NonNull
    private boolean isConstant;
    @NonNull
    private byte[] contractAddress;
}
