package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.*;
import cn.ledgeryi.api.WalletGrpc;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.Account;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.Protocol.TransactionInfo;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.ByteArray;
import cn.ledgeryi.sdk.config.Configuration;
import com.google.protobuf.ByteString;
import com.typesafe.config.Config;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class GrpcClient {

    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    private static final int BROADCAST_TRANSACTION_REPEAT_TIMES = 10;

    private GrpcClient(String ledgerYiNode) {
        if (!StringUtils.isEmpty(ledgerYiNode)) {
            ManagedChannel channelFull = ManagedChannelBuilder.forTarget(ledgerYiNode).usePlaintext(true).build();
            blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        }
    }

    public static GrpcClient initGrpcClient() {
        Config config = Configuration.getConfig();
        String ledgerYiNode;
        if (config.hasPath("ledgernode.ip.list") && config.getStringList("ledgernode.ip.list").size() != 0) {
            ledgerYiNode = config.getStringList("ledgernode.ip.list").get(0);
        } else {
            throw new RuntimeException("No connection information is configured!");
        }
        return new GrpcClient(ledgerYiNode);
    }

    public Account queryAccount(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStubFull.getAccount(request);
    }

    public MastersList queryMasters(){
        EmptyMessage message = EmptyMessage.newBuilder().build();
        return blockingStubFull.getMasters(message);
    }

    public boolean broadcastTransaction(Transaction signaturedTransaction) {
        int repeatTimes = BROADCAST_TRANSACTION_REPEAT_TIMES;
        Return response = blockingStubFull.broadcastTransaction(signaturedTransaction);
        while (!response.getResult() && response.getCode() == Return.response_code.SERVER_BUSY && repeatTimes > 0) {
            repeatTimes--;
            response = blockingStubFull.broadcastTransaction(signaturedTransaction);
            log.info("broadcast transaction, repeat times: " + (BROADCAST_TRANSACTION_REPEAT_TIMES - repeatTimes + 1));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!response.getResult()) {
            log.error("broadcast transaction, result is false, code: " + response.getCode());
            log.error("broadcast transaction, result is false, message: " + response.getMessage().toStringUtf8());
        }
        return response.getResult();
    }

    public BlockExtention getNowBlock() {
        return blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
    }


    public BlockExtention getBlockByNum(long blockNum) {
        if (blockNum < 0) {
            return blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
        }
        NumberMessage.Builder builder = NumberMessage.newBuilder();
        builder.setNum(blockNum);
        return blockingStubFull.getBlockByNum(builder.build());
    }

    public BlockListExtention getBlockByLimitNext(long start, long end){
        BlockLimit.Builder builder = BlockLimit.newBuilder();
        builder.setStartNum(start);
        builder.setEndNum(end);
        return blockingStubFull.getBlockByLimitNext(builder.build());
    }

    public Transaction getTransactionById(String hash){
        GrpcAPI.BytesMessage.Builder builder = GrpcAPI.BytesMessage.newBuilder();
        builder.setValue(ByteString.copyFrom(ByteArray.fromHexString(hash)));
        return blockingStubFull.getTransactionById(builder.build());
    }

    public TransactionInfo getTransactionInfoById(String txId) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        return blockingStubFull.getTransactionInfoById(request);
    }

    public NumberMessage getTransactionCountByBlockNum(long blockNum){
        NumberMessage.Builder builder = NumberMessage.newBuilder();
        builder.setNum(blockNum);
        return blockingStubFull.getTransactionCountByBlockNum(builder.build());
    }

    public TransactionExtention triggerContract(SmartContractOuterClass.TriggerSmartContract request) {
        return blockingStubFull.triggerContract(request);
    }

    public TransactionExtention deployContract(SmartContractOuterClass.CreateSmartContract request) {
        return blockingStubFull.deployContract(request);
    }

    public TransactionExtention triggerConstantContract(SmartContractOuterClass.TriggerSmartContract request) {
        return blockingStubFull.triggerConstantContract(request);
    }

    public SmartContractOuterClass.SmartContract getContract(byte[] address) {
        ByteString byteString = ByteString.copyFrom(address);
        BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(byteString).build();
        return blockingStubFull.getContract(bytesMessage);
    }

    public TransactionExtention clearContractABI(SmartContractOuterClass.ClearABIContract request) {
        return blockingStubFull.clearContractABI(request);
    }

    public NodeList getConnectNodes(){
        EmptyMessage message = EmptyMessage.newBuilder().build();
        return blockingStubFull.getNodes(message);
    }

    public Protocol.NodeInfo getNodeInfo(){
        EmptyMessage message = EmptyMessage.newBuilder().build();
        return blockingStubFull.getNodeInfo(message);
    }
}
