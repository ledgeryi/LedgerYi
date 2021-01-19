package cn.ledgeryi.sdk.serverapi.data;

import com.google.protobuf.ByteString;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TriggerContractReturn {
    /**
     * whether to call a constant contract
     */
    private boolean isConstant;

    /**
     * the result returned by invoking the contract
     */
    private ByteString callResult;

    /**
     * contract address
     */
    private String contractAddress;

    /**
     * trigger contract transaction id
     */
    private String transactionId;
}
