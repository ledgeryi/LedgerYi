package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.common.utils.TransactionUtils;
import cn.ledgeryi.sdk.contract.ContactCompileUtil;
import cn.ledgeryi.sdk.contract.compiler.entity.Library;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.event.Log;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.serverapi.data.*;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Brian
 * @date 2021/7/27 10:34
 */
@Slf4j
public class LedgerYiPermissionApiService {

    private GrpcClient rpcCli;

    public LedgerYiPermissionApiService() {
        rpcCli = GrpcClient.initGrpcClient();
    }
    public GrpcAPI.MastersList getMasters(RequestUserInfo requestUser){
        return rpcCli.queryMasters(requestUser);
    }

    public GrpcAPI.BlockExtention getNowBlock(RequestUserInfo requestUser){
        return rpcCli.getNowBlock(requestUser);
    }

    public GrpcAPI.BlockExtention getBlock(long blockNum, RequestUserInfo requestUser) {
        return rpcCli.getBlockByNum(blockNum,requestUser);
    }

    public GrpcAPI.BlockListExtention getBlockByLimitNext(long start, long end, RequestUserInfo requestUser){
        return rpcCli.getBlockByLimitNext(start,end, requestUser);
    }

    public Protocol.Transaction getTransactionById(String hash, RequestUserInfo requestUser){
        return rpcCli.getTransactionById(hash,requestUser);
    }

    public TransactionInformation getTransactionInfoReadable(String hash, RequestUserInfo requestUser) {
        Protocol.TransactionInfo infoById = getTransactionInfoById(hash,requestUser);
        TransactionInformation information = TransactionInformation.parseTransactionInfo(infoById);
        SmartContractOuterClass.SmartContract contract = getContract(infoById.getContractAddress().toByteArray(),requestUser);
        List<Log> logs = Log.parseLogInfo(infoById.getLogList(), contract.getAbi());
        information.setLogs(logs);
        return information;
    }

    public Protocol.TransactionInfo getTransactionInfoById(String hash, RequestUserInfo requestUser) {
        return rpcCli.getTransactionInfoById(hash,requestUser);
    }

    public GrpcAPI.NumberMessage getTransactionCountByBlockNum(long blockNum, RequestUserInfo requestUser){
        return rpcCli.getTransactionCountByBlockNum(blockNum,requestUser);
    }

    public GrpcAPI.NodeList getConnectNodes(RequestUserInfo requestUser){
        return rpcCli.getConnectNodes(requestUser);
    }

    public Protocol.NodeInfo getNodeInfo(RequestUserInfo requestUser){
        return rpcCli.getNodeInfo(requestUser);
    }

    /**
     * compile contract from a file of type 'sol',
     * support 'library'
     * @param source contract file path
     * @param contractName contract name
     * @param library contract library
     * @throws ContractException
     */
    public DeployContractParam compileContractFromFileNeedLibrary(
            Path source, String contractName, Library library) throws ContractException {
        return ContactCompileUtil.compileContractFromFiles(source, contractName,true, library);
    }

    /**
     * compile contract from a file of type 'sol',
     * support 'Contract inheritance', not support 'library'
     *
     * @param source contract file path
     * @param contractName contract name
     */
    public DeployContractParam compileContractFromFile(Path source, String contractName) throws ContractException {
        return ContactCompileUtil.compileContractFromFiles(source,contractName,false,null);
    }

    /**
     * compile single contract from a file of type 'sol'
     * @param contract contract content
     * @return
     * @throws ContractException
     */
    public DeployContractParam compileSingleContractFromContent(String contract) throws ContractException {
        return ContactCompileUtil.compileSingleContractFromContent(contract);
    }

    public SmartContractOuterClass.SmartContract getContract(byte[] address, RequestUserInfo requestUser) {
        return rpcCli.getContract(address,requestUser);
    }

    /**
     * call contract
     * @param ownerAddress the address of contract owner
     * @param privateKey the private key of contract owner
     * @param param contract  params
     * @return TriggerContractReturn
     */
    public TriggerContractReturn triggerContract(byte[] ownerAddress,
                                                 byte[] privateKey,
                                                 TriggerContractParam param,
                                                 RequestUserInfo requestUser) {
        SmartContractOuterClass.TriggerSmartContract triggerContract = triggerCallContract(ownerAddress,
                param.getContractAddress(), param.getCallValue(), param.getData());

        GrpcAPI.TransactionExtention transactionExtention;
        if (param.isConstant()) {
            transactionExtention = rpcCli.triggerConstantContract(triggerContract,requestUser);
            int constantResultCount = transactionExtention.getConstantResultCount();
            ByteString result = constantResultCount > 0 ? transactionExtention.getConstantResult(0) : null;
            return TriggerContractReturn.builder()
                    .isConstant(true)
                    .contractAddress(DecodeUtil.createReadableString(param.getContractAddress()))
                    .callResult(result)
                    .transactionId(DecodeUtil.createReadableString(transactionExtention.getTxid()))
                    .build();
        } else {
            transactionExtention = rpcCli.triggerContract(triggerContract,requestUser);
        }
        boolean result = processTransaction(transactionExtention, privateKey,requestUser);
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
    public DeployContractReturn deployContract(byte[] ownerAddress,
                                               byte[] privateKey,
                                               DeployContractParam param,
                                               RequestUserInfo requestUser)
            throws CreateContractExecption {
        SmartContractOuterClass.CreateSmartContract createContract = createContract(ownerAddress, param);
        GrpcAPI.TransactionExtention tx = rpcCli.deployContract(createContract,requestUser);
        boolean result = processTransaction(tx, privateKey,requestUser);
        if (result) {
            SmartContractOuterClass.CreateSmartContract createSmartContract;
            try {
                Any parameter = tx.getTransaction().getRawData().getContract().getParameter();
                createSmartContract = parameter.unpack(SmartContractOuterClass.CreateSmartContract.class);
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

    private SmartContractOuterClass.TriggerSmartContract triggerCallContract(byte[] ownerAddress,
                                                                             byte[] contractAddress,
                                                                             long callValue,
                                                                             byte[] data) {
        SmartContractOuterClass.TriggerSmartContract.Builder builder = SmartContractOuterClass.TriggerSmartContract.newBuilder();
        //set call address
        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(data));
        builder.setCallValue(callValue);
        return builder.build();
    }

//    private SmartContractOuterClass.ClearABIContract createClearABIContract(byte[] ownerAddress, byte[] contractAddress) {
//        SmartContractOuterClass.ClearABIContract.Builder builder = SmartContractOuterClass.ClearABIContract.newBuilder();
//        //set call address
//        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
//        builder.setContractAddress(ByteString.copyFrom(contractAddress));
//        return builder.build();
//    }

    private boolean processTransaction(GrpcAPI.TransactionExtention transactionExtention, byte[] privateKey,
                                       RequestUserInfo requestUser){
        if (transactionExtention == null) {
            return false;
        }
        GrpcAPI.Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            log.error("result is false, code: " + ret.getCode());
            log.error("result is false, message: " + ret.getMessage().toStringUtf8());
            return false;
        }
        Protocol.Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContract() == null) {
            log.error("transaction or contract is null");
            return false;
        }
        transaction = TransactionUtils.sign(transaction, privateKey);
        return rpcCli.broadcastTransaction(transaction,requestUser).getResult();
    }

    private SmartContractOuterClass.CreateSmartContract createContract(byte[] ownerAddress, DeployContractParam contractParam)
            throws CreateContractExecption {
        String contractAbi = contractParam.getAbi();
        if (StringUtils.isEmpty(contractAbi)) {
            log.error("deploy contract, abi is null");
            throw new CreateContractExecption("deploy contract, abi is null");
        }
        SmartContractOuterClass.SmartContract.ABI abi = JsonFormatUtil.jsonStr2ABI(contractAbi);
        SmartContractOuterClass.SmartContract.Builder builder = SmartContractOuterClass.SmartContract.newBuilder();
        builder.setAbi(abi);
        builder.setName(contractParam.getContractName());
        //set contract owner
        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        byte[] byteCode = Hex.decode(contractParam.getContractByteCodes());
        builder.setBytecode(ByteString.copyFrom(byteCode));
        SmartContractOuterClass.CreateSmartContract.Builder createSmartContractBuilder = SmartContractOuterClass.CreateSmartContract.newBuilder();
        //set call address
        createSmartContractBuilder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        createSmartContractBuilder.setNewContract(builder.build());
        return createSmartContractBuilder.build();
    }
}
