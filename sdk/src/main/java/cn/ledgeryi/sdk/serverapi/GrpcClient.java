package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.*;
import cn.ledgeryi.api.WalletGrpc;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.config.Configuration;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.typesafe.config.Config;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.lang3.StringUtils;
import cn.ledgeryi.protos.Protocol.Account;
import cn.ledgeryi.protos.Protocol.Transaction;

public class GrpcClient {

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;

    private static GrpcClient instance;

    private GrpcClient(String ledgerYiNode) {
        if (!StringUtils.isEmpty(ledgerYiNode)) {
            channelFull = ManagedChannelBuilder.forTarget(ledgerYiNode).usePlaintext(true).build();
            blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        }
    }

    public static GrpcClient initGrpcClient() {
        if (instance == null) {
            Config config = Configuration.getConfig();
            String ledgerYiNode = "";
            if (config.hasPath("ledgernode.ip.list")) {
                ledgerYiNode = config.getStringList("ledgernode.ip.list").get(0);
            }
            return new GrpcClient(ledgerYiNode);
        }
        return instance;
    }

    public Account queryAccount(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStubFull.getAccount(request);
    }

    public boolean broadcastTransaction(Transaction signaturedTransaction) {
        int i = 10;
        Return response = blockingStubFull.broadcastTransaction(signaturedTransaction);
        while (!response.getResult() && response.getCode() == Return.response_code.SERVER_BUSY && i > 0) {
            i--;
            response = blockingStubFull.broadcastTransaction(signaturedTransaction);
            System.out.println("repeat times = " + (11 - i));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!response.getResult()) {
            System.out.println("Code = " + response.getCode());
            System.out.println("Message = " + response.getMessage().toStringUtf8());
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
}
