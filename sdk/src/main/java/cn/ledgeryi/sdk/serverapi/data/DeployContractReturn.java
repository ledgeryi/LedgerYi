package cn.ledgeryi.sdk.serverapi.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeployContractReturn {
    /**
     * contract's ABI
     */
    private String contractAbi;

    /**
     * contract's name
     */
    private String contractName;

    /**
     * contract address
     */
    private String contractAddress;

    /**
     * address of contract owner
     */
    private String ownerAddress;

    /**
     * deploy contract transaction id
     */
    private String transactionId;

    /**
     * contract's byte codes
     */
    private String contractByteCodes;
}
