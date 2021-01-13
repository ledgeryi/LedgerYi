package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.Return;
import cn.ledgeryi.api.GrpcAPI.TransactionExtention;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.contract.BalanceContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.ClearABIContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.CreateSmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import cn.ledgeryi.sdk.common.utils.TransactionUtils;
import cn.ledgeryi.sdk.common.utils.Utils;
import cn.ledgeryi.sdk.execption.CipherException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Optional;

@Slf4j
public class RequestNodeApi {

    private static GrpcClient rpcCli = GrpcClient.initGrpcClient();

    public static Protocol.Account queryAccount(byte[] address){
        return rpcCli.queryAccount(address);
    }

    public static GrpcAPI.BlockExtention getNowBlock(){
        return rpcCli.getNowBlock();
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

    public static Protocol.TransactionInfo getTransactionInfoById(String hash) {
        return rpcCli.getTransactionInfoById(hash);
    }

    public static GrpcAPI.NumberMessage getTransactionCountByBlockNum(long blockNum){
        return rpcCli.getTransactionCountByBlockNum(blockNum);
    }

    public static SmartContract getContract(byte[] address) {
        return rpcCli.getContract(address);
    }

    public static boolean clearContractABI(byte[] owner, byte[] contractAddress, byte[] privateKeys) {
        ClearABIContract clearABIContract = createClearABIContract(owner, contractAddress);
        TransactionExtention transactionExtention = rpcCli.clearContractABI(clearABIContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            log.error("RPC create trx failed!");
            if (transactionExtention != null) {
                log.info("Code = " + transactionExtention.getResult().getCode());
                log.info("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return false;
        }
        return processTransaction(transactionExtention, privateKeys);
    }

    public static boolean triggerContract(byte[] owner, byte[] contractAddress, long callValue, byte[] data,
                                   long feeLimit, boolean isConstant, byte[] privateKey) {
        TriggerSmartContract triggerContract = triggerCallContract(owner, contractAddress, callValue, data);
        TransactionExtention transactionExtention;
        if (isConstant) {
            transactionExtention = rpcCli.triggerConstantContract(triggerContract);
        } else {
            transactionExtention = rpcCli.triggerContract(triggerContract);
        }
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            log.error("RPC create call tx failed!");
            log.error("Code = " + transactionExtention.getResult().getCode());
            log.error("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            return false;
        }

        Transaction transaction = transactionExtention.getTransaction();
        // for constant
        if (isConstant && transaction.getRetCount() != 0 && transactionExtention.getConstantResult(0) != null
                && transactionExtention.getResult() != null) {
            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
            log.info("Message:" + transaction.getRet(0).getRet());
            log.info(":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
            log.info("Result:" + Hex.toHexString(result));
            return true;
        }
        TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData().toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        ByteString s = transactionExtention.getTransaction().getSignature();
        transBuilder.setSignature(s);
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Transaction.Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();
        return processTransaction(transactionExtention, privateKey);
    }

    public static boolean deployContract( byte[] owner, String contractName, String abi, String code, long feeLimit,
                                   long value, long consumeUserResourcePercent, byte[] privateKey) {
        CreateSmartContract contractDeployContract = createContractDeployContract(contractName, owner, abi, code,
                value, consumeUserResourcePercent);
        GrpcAPI.TransactionExtention transactionExtention = rpcCli.deployContract(contractDeployContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create trx failed!");
            if (transactionExtention != null) {
                System.out.println("Code = " + transactionExtention.getResult().getCode());
                System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return false;
        }
        GrpcAPI.TransactionExtention.Builder texBuilder = GrpcAPI.TransactionExtention.newBuilder();
        Protocol.Transaction.Builder transBuilder = Protocol.Transaction.newBuilder();
        Protocol.Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData().toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Protocol.Transaction.Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();
        return processTransaction(transactionExtention, privateKey);
    }

    private static TriggerSmartContract triggerCallContract( byte[] address, byte[] contractAddress, long callValue, byte[] data) {
        TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(address));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(data));
        builder.setCallValue(callValue);
        return builder.build();
    }

    private static ClearABIContract createClearABIContract(byte[] owner, byte[] contractAddress) {
        ClearABIContract.Builder builder = ClearABIContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        return builder.build();
    }

    private static boolean processTransaction(TransactionExtention transactionExtention, byte[] privKeyBytes) {
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            log.error("Code = " + ret.getCode());
            log.error("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContract() == null) {
            log.error("Transaction is empty");
            return false;
        }
        System.out.println(Utils.printTransactionExceptId(transactionExtention.getTransaction()));
        transaction = TransactionUtils.sign(transaction, privKeyBytes);
        boolean validTransaction = TransactionUtils.validTransaction(transaction);
        if (!validTransaction) {
            log.error("Verification signature failed！");
            throw new RuntimeException();
        }
        return rpcCli.broadcastTransaction(transaction);
    }

    private static CreateSmartContract createContractDeployContract(String contractName, byte[] address, String ABI,
                                                             String code, long value, long consumeUserResourcePercent) {
        SmartContract.ABI abi = jsonStr2ABI(ABI);
        if (abi == null) {
            System.out.println("abi is null");
            return null;
        }
        SmartContract.Builder builder = SmartContract.newBuilder();
        builder.setName(contractName);
        builder.setOriginAddress(ByteString.copyFrom(address));
        builder.setAbi(abi);
        builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
        if (value != 0) {
            builder.setCallValue(value);
        }
        byte[] byteCode = Hex.decode(code);
        builder.setBytecode(ByteString.copyFrom(byteCode));
        CreateSmartContract.Builder createSmartContractBuilder = CreateSmartContract.newBuilder();
        createSmartContractBuilder.setOwnerAddress(ByteString.copyFrom(address)).setNewContract(builder.build());
        return createSmartContractBuilder.build();
    }

    public static SmartContract.ABI jsonStr2ABI(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
        JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
        SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
        for (int index = 0; index < jsonRoot.size(); index++) {
            JsonElement abiItem = jsonRoot.get(index);
            boolean anonymous = abiItem.getAsJsonObject().get("anonymous") != null
                                ? abiItem.getAsJsonObject().get("anonymous")
                                .getAsBoolean()
                                : false;
            boolean constant = abiItem.getAsJsonObject().get("constant") != null
                                ? abiItem.getAsJsonObject().get("constant")
                                .getAsBoolean()
                                : false;
            String name = abiItem.getAsJsonObject().get("name") != null
                                ? abiItem.getAsJsonObject().get("name").getAsString()
                                : null;
            JsonArray inputs = abiItem.getAsJsonObject().get("inputs") != null
                                ? abiItem.getAsJsonObject().get("inputs")
                                .getAsJsonArray()
                                : null;
            JsonArray outputs = abiItem.getAsJsonObject().get("outputs") != null
                                ? abiItem.getAsJsonObject().get("outputs")
                                .getAsJsonArray()
                                : null;
            String type = abiItem.getAsJsonObject().get("type") != null
                                ? abiItem.getAsJsonObject().get("type").getAsString()
                                : null;
            boolean payable = abiItem.getAsJsonObject().get("payable") != null
                                ? abiItem.getAsJsonObject().get("payable")
                                .getAsBoolean()
                                : false;
            String stateMutability = abiItem.getAsJsonObject().get("stateMutability") != null
                                ? abiItem.getAsJsonObject().get("stateMutability")
                                .getAsString()
                                : null;
            if (type == null) {
                log.error("No type!");
                return null;
            }
            if (!type.equalsIgnoreCase("fallback") && null == inputs) {
                log.error("No inputs!");
                return null;
            }
            SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
            entryBuilder.setAnonymous(anonymous);
            entryBuilder.setConstant(constant);
            if (name != null) {
                entryBuilder.setName(name);
            }
            /* { inputs : optional } since fallback function not requires inputs*/
            if (null != inputs) {
                for (int j = 0; j < inputs.size(); j++) {
                    JsonElement inputItem = inputs.get(j);
                    if (inputItem.getAsJsonObject().get("name") == null || inputItem.getAsJsonObject().get("type") == null) {
                        log.error("Input argument invalid due to no name or no type!");
                        return null;
                    }
                    String inputName = inputItem.getAsJsonObject().get("name").getAsString();
                    String inputType = inputItem.getAsJsonObject().get("type").getAsString();
                    Boolean inputIndexed = false;
                    if (inputItem.getAsJsonObject().get("indexed") != null) {
                        inputIndexed = Boolean.valueOf(inputItem.getAsJsonObject().get("indexed").getAsString());
                    }
                    SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param.newBuilder();
                    paramBuilder.setIndexed(inputIndexed);
                    paramBuilder.setName(inputName);
                    paramBuilder.setType(inputType);
                    entryBuilder.addInputs(paramBuilder.build());
                }
            }

            /* { outputs : optional } */
            if (outputs != null) {
                for (int k = 0; k < outputs.size(); k++) {
                    JsonElement outputItem = outputs.get(k);
                    if (outputItem.getAsJsonObject().get("name") == null || outputItem.getAsJsonObject().get("type") == null) {
                        log.error("Output argument invalid due to no name or no type!");
                        return null;
                    }
                    String outputName = outputItem.getAsJsonObject().get("name").getAsString();
                    String outputType = outputItem.getAsJsonObject().get("type").getAsString();
                    Boolean outputIndexed = false;
                    if (outputItem.getAsJsonObject().get("indexed") != null) {
                        outputIndexed = Boolean.valueOf(outputItem.getAsJsonObject().get("indexed").getAsString());
                    }
                    SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param.newBuilder();
                    paramBuilder.setIndexed(outputIndexed);
                    paramBuilder.setName(outputName);
                    paramBuilder.setType(outputType);
                    entryBuilder.addOutputs(paramBuilder.build());
                }
            }
            entryBuilder.setType(getEntryType(type));
            entryBuilder.setPayable(payable);
            if (stateMutability != null) {
                entryBuilder.setStateMutability(getStateMutability(stateMutability));
            }
            abiBuilder.addEntrys(entryBuilder.build());
        }
        return abiBuilder.build();
    }
    public static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
        switch (type) {
            case "constructor":
                return SmartContract.ABI.Entry.EntryType.Constructor;
            case "function":
                return SmartContract.ABI.Entry.EntryType.Function;
            case "event":
                return SmartContract.ABI.Entry.EntryType.Event;
            case "fallback":
                return SmartContract.ABI.Entry.EntryType.Fallback;
            default:
                return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
        }
    }

    public static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
            String stateMutability) {
        switch (stateMutability) {
            case "pure":
                return SmartContract.ABI.Entry.StateMutabilityType.Pure;
            case "view":
                return SmartContract.ABI.Entry.StateMutabilityType.View;
            case "nonpayable":
                return SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
            case "payable":
                return SmartContract.ABI.Entry.StateMutabilityType.Payable;
            default:
                return SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
        }
    }
}