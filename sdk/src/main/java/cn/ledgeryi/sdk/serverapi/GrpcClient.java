package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.*;
import cn.ledgeryi.api.WalletGrpc;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.Account;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.Protocol.TransactionInfo;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.*;
import cn.ledgeryi.sdk.common.utils.ByteArray;
import cn.ledgeryi.sdk.config.Configuration;
import cn.ledgeryi.sdk.serverapi.data.RequestUserInfo;
import com.google.protobuf.Any;
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

    /*public Account queryAccount(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStubFull.getAccount(request);
    }*/

    public MastersList queryMasters(RequestUserInfo requestUser){
        return blockingStubFull.getMasters(setRequestUser(requestUser));
    }

    public boolean broadcastTransaction(Transaction signaturedTransaction, RequestUserInfo requestUser) {
        GrpcRequest.Builder builder = setRequestUser(requestUser).toBuilder();
        builder.setParam(Any.pack(signaturedTransaction));
        int repeatTimes = BROADCAST_TRANSACTION_REPEAT_TIMES;
        Return response = blockingStubFull.broadcastTransaction(builder.build());
        while (!response.getResult() && response.getCode() == Return.response_code.SERVER_BUSY && repeatTimes > 0) {
            repeatTimes--;
            response = blockingStubFull.broadcastTransaction(builder.build());
            log.info("broadcast tx, repeat times: " + (BROADCAST_TRANSACTION_REPEAT_TIMES - repeatTimes + 1));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!response.getResult()) {
            log.error("broadcast tx, code: {}, message: {}" + response.getCode(), response.getMessage().toStringUtf8());
        }
        return response.getResult();
    }

    public BlockExtention getNowBlock(RequestUserInfo requestUser) {
        GrpcRequest.Builder builder = setRequestUser(requestUser).toBuilder();
        return blockingStubFull.getNowBlock(builder.build());
    }

    public BlockExtention getBlockByNum(long blockNum, RequestUserInfo requestUser) {
        GrpcRequest.Builder builder = setRequestUser(requestUser).toBuilder();
        if (blockNum < 0) {
            return blockingStubFull.getNowBlock(builder.build());
        }
        NumberMessage.Builder numberBuilder = NumberMessage.newBuilder();
        numberBuilder.setNum(blockNum);
        builder.setParam(Any.pack(numberBuilder.build()));
        return blockingStubFull.getBlockByNum(builder.build());
    }

    public BlockListExtention getBlockByLimitNext(long start, long end, RequestUserInfo requestUser) {
        BlockLimit.Builder builder = BlockLimit.newBuilder();
        builder.setStartNum(start);
        builder.setEndNum(end);
        GrpcRequest.Builder grpcBuilder = setRequestUser(requestUser).toBuilder();
        grpcBuilder.setParam(Any.pack(builder.build()));
        return blockingStubFull.getBlockByLimitNext(grpcBuilder.build());
    }

    public Transaction getTransactionById(String hash, RequestUserInfo requestUser) {
        GrpcAPI.BytesMessage.Builder builder = GrpcAPI.BytesMessage.newBuilder();
        builder.setValue(ByteString.copyFrom(ByteArray.fromHexString(hash)));
        GrpcRequest.Builder grpcBuilder = setRequestUser(requestUser).toBuilder();
        grpcBuilder.setParam(Any.pack(builder.build()));
        return blockingStubFull.getTransactionById(grpcBuilder.build());
    }

    public TransactionInfo getTransactionInfoById(String txId, RequestUserInfo requestUser) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        GrpcRequest.Builder grpcBuilder = setRequestUser(requestUser).toBuilder();
        grpcBuilder.setParam(Any.pack(request));
        return blockingStubFull.getTransactionInfoById(grpcBuilder.build());
    }

    public NumberMessage getTransactionCountByBlockNum(long blockNum, RequestUserInfo requestUser) {
        NumberMessage.Builder builder = NumberMessage.newBuilder();
        builder.setNum(blockNum);
        GrpcRequest.Builder grpcBuilder = setRequestUser(requestUser).toBuilder();
        grpcBuilder.setParam(Any.pack(builder.build()));
        return blockingStubFull.getTransactionCountByBlockNum(grpcBuilder.build());
    }

    public TransactionExtention triggerContract(TriggerSmartContract request, RequestUserInfo requestUser) {
        GrpcRequest.Builder builder = setRequestUser(requestUser).toBuilder();
        builder.setParam(Any.pack(request));
        return blockingStubFull.triggerContract(builder.build());
    }

    public TransactionExtention deployContract(CreateSmartContract request, RequestUserInfo requestUser) {
        GrpcRequest.Builder builder = setRequestUser(requestUser).toBuilder();
        builder.setParam(Any.pack(request));
        return blockingStubFull.deployContract(builder.build());
    }

    public TransactionExtention triggerConstantContract(TriggerSmartContract request, RequestUserInfo requestUser) {
        GrpcRequest.Builder builder = setRequestUser(requestUser).toBuilder();
        builder.setParam(Any.pack(request));
        return blockingStubFull.triggerConstantContract(builder.build());
    }

    public SmartContract getContract(byte[] address, RequestUserInfo requestUser) {
        ByteString byteString = ByteString.copyFrom(address);
        BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(byteString).build();
        GrpcRequest.Builder builder = setRequestUser(requestUser).toBuilder();
        builder.setParam(Any.pack(bytesMessage));
        return blockingStubFull.getContract(builder.build());
    }

    public TransactionExtention clearContractABI(ClearABIContract request, RequestUserInfo requestUser) {
        GrpcRequest.Builder builder = setRequestUser(requestUser).toBuilder();
        builder.setParam(Any.pack(request));
        return blockingStubFull.clearContractABI(builder.build());
    }

    public NodeList getConnectNodes(RequestUserInfo requestUser) {
        return blockingStubFull.getNodes(setRequestUser(requestUser));
    }

    public Protocol.NodeInfo getNodeInfo(RequestUserInfo requestUser) {
        return blockingStubFull.getNodeInfo(setRequestUser(requestUser));
    }

    private GrpcRequest setRequestUser(RequestUserInfo requestUser){
        if (requestUser != null) {
            GrpcRequest.Builder builder = GrpcRequest.newBuilder();
            builder.setRequestAddress(requestUser.getAddress()).setRequestRole(requestUser.getRoleId());
            return builder.build();
        }
        return GrpcRequest.getDefaultInstance();
    }

}
