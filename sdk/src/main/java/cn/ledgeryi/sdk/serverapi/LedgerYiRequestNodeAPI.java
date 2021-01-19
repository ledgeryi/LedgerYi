package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.Return;
import cn.ledgeryi.api.GrpcAPI.TransactionExtention;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.ClearABIContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.CreateSmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import cn.ledgeryi.sdk.common.DeployContractParam;
import cn.ledgeryi.sdk.common.DeployContractReturn;
import cn.ledgeryi.sdk.common.TriggerContractParam;
import cn.ledgeryi.sdk.common.TriggerContractReturn;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.common.utils.TransactionUtils;
import cn.ledgeryi.sdk.config.Configuration;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

@Slf4j
public class LedgerYiRequestNodeAPI {

    private static GrpcClient rpcCli= GrpcClient.initGrpcClient();

    public static Protocol.Account getAccount(String address){
        return rpcCli.queryAccount(DecodeUtil.decode(address));
    }

    public static GrpcAPI.MastersList getMasters(){
        return rpcCli.queryMasters();
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

    public static GrpcAPI.NodeList getConnectNodes(){
        return rpcCli.getConnectNodes();
    }

    public static Protocol.NodeInfo getNodeInfo(){
        return rpcCli.getNodeInfo();
    }

    public static SmartContract getContract(byte[] address) {
        return rpcCli.getContract(address);
    }

    public static boolean clearContractABI(byte[] ownerAddress, byte[] privateKey, byte[] contractAddress) {
        if (!checkOwnerAddressAndPrivateKey(ownerAddress, privateKey)){
            ownerAddress = DecodeUtil.decode(Configuration.getAccountyiAddress());
            privateKey = DecodeUtil.decode(Configuration.getAccountyiPrivateKey());
        }
        ClearABIContract clearABIContract = createClearABIContract(ownerAddress, contractAddress);
        TransactionExtention transactionExtention = rpcCli.clearContractABI(clearABIContract);
        return processTransaction(transactionExtention, privateKey);
    }

    /**
     * call contract
     * @param ownerAddress the address of contract owner
     * @param privateKey the private key of contract owner
     * @param param contract  params
     * @return TriggerContractReturn
     */
    public static TriggerContractReturn triggerContract(byte[] ownerAddress, byte[] privateKey, TriggerContractParam param) {
        if (!checkOwnerAddressAndPrivateKey(ownerAddress, privateKey)){
            ownerAddress = DecodeUtil.decode(Configuration.getAccountyiAddress());
            privateKey = DecodeUtil.decode(Configuration.getAccountyiPrivateKey());
        }
        TriggerSmartContract triggerContract = triggerCallContract(ownerAddress,
                param.getContractAddress(), param.getCallValue(), param.getData());

        TransactionExtention transactionExtention;
        if (param.isConstant()) {
            transactionExtention = rpcCli.triggerConstantContract(triggerContract);
            return TriggerContractReturn.builder()
                    .isConstant(true)
                    .contractAddress(DecodeUtil.createReadableString(param.getContractAddress()))
                    .callResult(transactionExtention.getConstantResult(0))
                    .transactionId(DecodeUtil.createReadableString(transactionExtention.getTxid()))
                    .build();
        } else {
            transactionExtention = rpcCli.triggerContract(triggerContract);
        }
        boolean result = processTransaction(transactionExtention, privateKey);
        if (result){
            return TriggerContractReturn.builder()
                    .isConstant(false)
                    .contractAddress(DecodeUtil.createReadableString(param.getContractAddress()))
                    .callResult(ByteString.EMPTY)
                    .transactionId(DecodeUtil.createReadableString(transactionExtention.getTxid()))
                    .build();
        } else {
            log.error("call contract failed.");
            return null;
        }
    }


    /**
     * create contract
     *
     * @param ownerAddress the address of contract owner
     * @param privateKey the private key of contract owner
     * @param param contract  params
     * @return DeployContractReturn
     * @throws CreateContractExecption abi is null
     */
    public static DeployContractReturn deployContract( byte[] ownerAddress, byte[] privateKey, DeployContractParam param) throws CreateContractExecption {
        if (!checkOwnerAddressAndPrivateKey(ownerAddress, privateKey)){
            ownerAddress = DecodeUtil.decode(Configuration.getAccountyiAddress());
            privateKey = DecodeUtil.decode(Configuration.getAccountyiPrivateKey());
        }
        CreateSmartContract createContract = createContract(ownerAddress, param);
        GrpcAPI.TransactionExtention tx = rpcCli.deployContract(createContract);
        boolean result = processTransaction(tx, privateKey);
        if (result) {
            CreateSmartContract createSmartContract;
            try {
                createSmartContract = tx.getTransaction().getRawData().getContract().getParameter().unpack(CreateSmartContract.class);
            } catch (InvalidProtocolBufferException e) {
                log.error("deploy contract, parameter parse fail");
                return null;
            }
            String contractByteCodes = DecodeUtil.createReadableString(createSmartContract.getNewContract().getBytecode());
            String contractAddress = DecodeUtil.createReadableString(
                    TransactionUtils.generateContractAddress(tx.getTransaction(), ownerAddress));
            return DeployContractReturn.builder()
                    .transactionId(DecodeUtil.createReadableString(tx.getTxid()))
                    .contractName(createSmartContract.getNewContract().getName())
                    .contractByteCodes(contractByteCodes)
                    .ownerAddress(DecodeUtil.createReadableString(ownerAddress))
                    .contractAddress(contractAddress)
                    .contractAbi(createSmartContract.getNewContract().getAbi().toString())
                    .build();
        } else {
            log.error("deploy contract failed.");
            return null;
        }
    }

    private static TriggerSmartContract triggerCallContract( byte[] ownerAddress, byte[] contractAddress, long callValue, byte[] data) {
        TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(data));
        builder.setCallValue(callValue);
        return builder.build();
    }

    private static ClearABIContract createClearABIContract(byte[] ownerAddress, byte[] contractAddress) {
        ClearABIContract.Builder builder = ClearABIContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        return builder.build();
    }

    private static boolean processTransaction(TransactionExtention transactionExtention, byte[] privateKey) {
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            log.error("result is false, code: " + ret.getCode());
            log.error("result is false, message: " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContract() == null) {
            log.error("transaction or contract is null");
            return false;
        }
        //System.out.println(JsonFormatUtil.printTransactionExceptId(transactionExtention.getTransaction()));
        transaction = TransactionUtils.sign(transaction, privateKey);
        return rpcCli.broadcastTransaction(transaction);
    }

    private static CreateSmartContract createContract(byte[] ownerAddress, DeployContractParam contractParam) throws CreateContractExecption {
        String contractAbi = contractParam.getAbi();
        if (StringUtils.isEmpty(contractAbi)) {
            log.error("deploy contract, abi is null");
            throw new CreateContractExecption("deploy contract, abi is null");
        }
        SmartContract.ABI abi = JsonFormatUtil.jsonStr2ABI(contractAbi);
        SmartContract.Builder builder = SmartContract.newBuilder();
        builder.setAbi(abi);
        builder.setName(contractParam.getContractName());
        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        byte[] byteCode = Hex.decode(contractParam.getContractByteCodes());
        builder.setBytecode(ByteString.copyFrom(byteCode));
        CreateSmartContract.Builder createSmartContractBuilder = CreateSmartContract.newBuilder();
        createSmartContractBuilder.setOwnerAddress(ByteString.copyFrom(ownerAddress)).setNewContract(builder.build());
        return createSmartContractBuilder.build();
    }

    private static boolean checkOwnerAddressAndPrivateKey(byte[] ownerAddress, byte[] privateKey){
        if (ByteUtil.isNullOrZeroArray(ownerAddress) || ByteUtil.isNullOrZeroArray(privateKey)){
            if (!ByteUtil.isNullOrZeroArray(privateKey) || !ByteUtil.isNullOrZeroArray(ownerAddress)){
                log.error("Require account's private key and address to be empty");
                return false;
            }
        }
        return true;
    }
}