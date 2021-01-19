package cn.ledgeryi.sdk.common;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class DeployContractParam {
    @NonNull
    private String abi;
    @NonNull
    private String contractName;
    @NonNull
    private String contractByteCodes;
}
