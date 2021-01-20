package cn.ledgeryi.sdk.serverapi.data;

import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.TransactionInfo;
import cn.ledgeryi.protos.Protocol.Transaction.Result.ContractResult;
import cn.ledgeryi.sdk.parse.event.Log;
import com.google.protobuf.ByteString;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionInformation {
    private String txId;
    private long blockNumber;
    private long blockTimeStamp;
    private List<String> contractResult;
    private String contractAddress;
    private ResourceReceipt receipt;
    private List<Log> logs;


    public static TransactionInformation parseTransactionInfo(TransactionInfo tx){
        return TransactionInformation.builder()
                .txId(DecodeUtil.createReadableString(tx.getId()))
                .blockNumber(tx.getBlockNumber())
                .blockTimeStamp(tx.getBlockTimeStamp())
                .contractResult(parseContractResult(tx.getContractResultList()))
                .contractAddress(DecodeUtil.createReadableString(tx.getContractAddress()))
                .receipt(ResourceReceipt.parseReceipt(tx.getReceipt()))
                .build();
    }

    private static List<String> parseContractResult(List<ByteString> contractResult){
        List<String> tmp = new ArrayList<>();
        contractResult.forEach(result -> {
            tmp.add(DecodeUtil.createReadableString(result));
        });
        return tmp;
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
