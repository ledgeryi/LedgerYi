package cn.ledgeryi.sdk.serverapi.data;

import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.Transaction.Result.ContractResult;
import cn.ledgeryi.protos.Protocol.TransactionInfo;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.parse.event.Log;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionInformation {
    private String txId;
    private long blockNumber;
    private long blockTimeStamp;
    private String contractAddress;
    private ResourceReceipt receipt;
    private List<Log> logs;
    private String resMassage;


    public static TransactionInformation parseTransactionInfo(TransactionInfo tx){
        return TransactionInformation.builder()
                .txId(DecodeUtil.createReadableString(tx.getId()))
                .blockNumber(tx.getBlockNumber())
                .blockTimeStamp(tx.getBlockTimeStamp())
                .contractAddress(DecodeUtil.createReadableString(tx.getContractAddress()))
                .receipt(ResourceReceipt.parseReceipt(tx.getReceipt()))
                .resMassage(DecodeUtil.createReadableString(tx.getResMessage()))
                .build();
    }


    @Data
    @NoArgsConstructor
    public static class ResourceReceipt {
        private String contractResult;
        private long cpuTimeUsed;
        private long storageUsed;

        public ResourceReceipt(ContractResult contractResult, long cpuTimeUsed, long storageUsed) {
            this.contractResult = contractResult.name();
            this.cpuTimeUsed = cpuTimeUsed;
            this.storageUsed = storageUsed;
        }

        public static ResourceReceipt parseReceipt(Protocol.ResourceReceipt receipt){
            return new ResourceReceipt(receipt.getResult(),receipt.getCpuTimeUsed(),receipt.getStorageUsed());
        }
    }
}
