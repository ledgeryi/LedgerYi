package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.*;
import cn.ledgeryi.api.WalletGrpc;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.protos.contract.InnerContract;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.lang3.StringUtils;
import cn.ledgeryi.protos.Protocol.Account;
import cn.ledgeryi.protos.Protocol.Transaction;

public class GrpcClient {
    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;

    public GrpcClient(String fullNode) {
        if (!StringUtils.isEmpty(fullNode)) {
            channelFull = ManagedChannelBuilder.forTarget(fullNode).usePlaintext(true).build();
            blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        }
    }

    public Account queryAccount(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStubFull.getAccount(request);
    }

    public TransactionExtention createTransaction(com.google.protobuf.Message message,
                                                    Transaction.Contract.ContractType contractType) {
        InnerContract.SystemContract.Builder builder = InnerContract.SystemContract.newBuilder();
        builder.setContract(message instanceof Any ? (Any) message : Any.pack(message));
        builder.setType(contractType);
        return blockingStubFull.createTransaction(builder.build());
    }

    boolean broadcastTransaction(Transaction signaturedTransaction) {
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
}
