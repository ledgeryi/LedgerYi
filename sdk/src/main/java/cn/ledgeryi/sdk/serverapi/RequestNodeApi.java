package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.AccountContract;
import cn.ledgeryi.protos.contract.BalanceContract;
import cn.ledgeryi.sdk.common.utils.TransactionUtils;
import cn.ledgeryi.sdk.core.config.Configuration;
import com.google.protobuf.ByteString;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestNodeApi {
    private static GrpcClient rpcCli = init();

    private static GrpcClient init() {
        Config config = Configuration.getConfig();

        String fullNode = "";
        if (config.hasPath("fullnode.ip.list")) {
            fullNode = config.getStringList("fullnode.ip.list").get(0);
        }
        return new GrpcClient(fullNode);
    }

    public static Sha256Hash createTransfer(byte[] ownerAddress, byte[] toAddress, long amount,  byte[] privateKeys){
        BalanceContract.TransferContract transferContract = createTransferContract(ownerAddress, toAddress, amount);
        GrpcAPI.TransactionExtention transaction = rpcCli.createTransaction(transferContract,
                Protocol.Transaction.Contract.ContractType.TransferContract);
        return processTransaction(transaction, privateKeys);
    }

    private static BalanceContract.TransferContract createTransferContract(byte[] ownerAddress,
                                                                           byte[] toAddress, long amount){
        BalanceContract.TransferContract.Builder builder = BalanceContract.TransferContract.newBuilder();
        builder.setAmount(amount);
        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        builder.setToAddress(ByteString.copyFrom(toAddress));
        return builder.build();
    }

    public static Protocol.Account queryAccount(byte[] address){
        return rpcCli.queryAccount(address);
    }

    private static Sha256Hash processTransaction(GrpcAPI.TransactionExtention transactionExtention, byte[] privateKeys) {
        if (transactionExtention == null) {
            return null;
        }
        GrpcAPI.Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            log.warn("Code = " + ret.getCode());
            log.warn("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        Protocol.Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContract() == null) {
            return null;
        }
        transaction = TransactionUtils.sign(transaction, privateKeys);
        rpcCli.broadcastTransaction(transaction);
        return Sha256Hash.of(true, transaction.getRawData().toByteArray());
    }

    private static AccountContract.AccountCreateContract createAccountContract(byte[] owner, byte[] address) {
        AccountContract.AccountCreateContract.Builder builder = AccountContract.AccountCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setAccountAddress(ByteString.copyFrom(address));
        builder.setType(Protocol.AccountType.Normal);
        return builder.build();
    }

    public static GrpcAPI.BlockExtention getBlock(long blockNum) {
        return rpcCli.getBlockByNum(blockNum);
    }


    public static GrpcAPI.BlockListExtention getBlockByLimitNext(long start, long end){
        return rpcCli.getBlockByLimitNext(start,end);
    }

    public static Protocol.Transaction getTransactionById(String hash){
        return rpcCli.getTransactionById(hash);
    }

    public static GrpcAPI.NumberMessage getTransactionCountByBlockNum(long blockNum){
        return rpcCli.getTransactionCountByBlockNum(blockNum);
    }
}
