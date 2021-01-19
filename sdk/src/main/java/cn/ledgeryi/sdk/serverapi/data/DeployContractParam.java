package cn.ledgeryi.sdk.serverapi.data;

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
