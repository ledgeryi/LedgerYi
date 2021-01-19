package cn.ledgeryi.sdk.common;

import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract.ABI;
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
