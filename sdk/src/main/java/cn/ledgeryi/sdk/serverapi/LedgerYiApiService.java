package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.Return;
import cn.ledgeryi.api.GrpcAPI.TransactionExtention;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.ClearABIContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.CreateSmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import cn.ledgeryi.sdk.common.AccountYi;
import cn.ledgeryi.sdk.common.utils.*;
import cn.ledgeryi.sdk.config.Configuration;
import cn.ledgeryi.sdk.contract.compiler.SolidityCompiler;
import cn.ledgeryi.sdk.contract.compiler.SolidityCompiler.Options;
import cn.ledgeryi.sdk.contract.compiler.entity.CompilationResult;
import cn.ledgeryi.sdk.contract.compiler.entity.Result;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.parse.event.Log;
import cn.ledgeryi.sdk.serverapi.data.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class LedgerYiApiService {

    private GrpcClient rpcCli;
    private String signerAddress;
    private String signerPrivateKey;

    public LedgerYiApiService() {
        rpcCli = GrpcClient.initGrpcClient();
        signerAddress = Configuration.getAccountyiAddress();
        signerPrivateKey = Configuration.getAccountyiPrivateKey();
    }

    public AccountYi createDefaultAccount(){
        return LedgerYiUtils.createAccountYi();
    }

    public Protocol.Account getAccount(String address){
        return rpcCli.queryAccount(DecodeUtil.decode(address));
    }

    public GrpcAPI.MastersList getMasters(){
        return rpcCli.queryMasters();
    }

    public GrpcAPI.BlockExtention getNowBlock(){
        return rpcCli.getNowBlock();
    }

    public GrpcAPI.BlockExtention getBlock(long blockNum) {
        return rpcCli.getBlockByNum(blockNum);
    }

    public GrpcAPI.BlockListExtention getBlockByLimitNext(long start, long end){
        return rpcCli.getBlockByLimitNext(start,end);
    }

    public Protocol.Transaction getTransactionById(String hash){
        return rpcCli.getTransactionById(hash);
    }

    public TransactionInformation getTransactionInfoReadable(String hash) {
        Protocol.TransactionInfo infoById = getTransactionInfoById(hash);
        TransactionInformation information = TransactionInformation.parseTransactionInfo(infoById);
        SmartContract contract = getContract(infoById.getContractAddress().toByteArray());
        List<Log> logs = Log.parseLogInfo(infoById.getLogList(), contract.getAbi());
        information.setLogs(logs);
        return information;
    }

    public Protocol.TransactionInfo getTransactionInfoById(String hash) {
        return rpcCli.getTransactionInfoById(hash);
    }

    public GrpcAPI.NumberMessage getTransactionCountByBlockNum(long blockNum){
        return rpcCli.getTransactionCountByBlockNum(blockNum);
    }

    public GrpcAPI.NodeList getConnectNodes(){
        return rpcCli.getConnectNodes();
    }

    public Protocol.NodeInfo getNodeInfo(){
        return rpcCli.getNodeInfo();
    }

    /**
     * compile contract from a file of type 'sol'
     * @param source contract file path
     * @param isSingle single contract(true)
     */
    public DeployContractParam compileContractFromFile(Path source, boolean isSingle) throws ContractException {
        Result res;
        try {
            res = SolidityCompiler.compileSrc(source.toFile(), true,
                    Options.ABI, Options.BIN, Options.INTERFACE, Options.METADATA);
        } catch (IOException e) {
            log.error("Compile contract error, io exception: ", e);
            throw new ContractException("Compilation io exception: " + e.getMessage());
        }
        if (!res.errors.isEmpty()) {
            log.error("Compilation contract error: " + res.errors);
            throw new ContractException("Compilation error: " + res.errors);
        }
        if (res.output.isEmpty()) {
            log.error("Compilation contract error:  output is empty: " + res.errors);
            throw new ContractException("compilation error, output is empty: " + res.errors);
        }

        // read from JSON and assemble request body
        CompilationResult result;
        try {
            result = CompilationResult.parse(res.output);
        } catch (IOException e) {
            log.error("Compilation contract error: Parse result error, io exception: ", e);
            throw new ContractException("parse result error, io exception: " + e.getMessage());
        }
        int contractSize = result.getContracts().size();
        if (contractSize == 0) {
            log.error("Compilation contract error: No Contract found after compile");
            throw new RuntimeException("Compilation error: No Contract found after compile" + result);
        }
        if (isSingle && result.getContracts().size() > 1) {
            log.error("Compilation contract error: Multiple Contracts found after compile");
            throw new RuntimeException("Compilation contract error: Multiple Contracts found after compile" + result);
        }

        Iterator<Map.Entry<String, CompilationResult.ContractMetadata>> iterator = result.getContracts().entrySet().iterator();
        Map.Entry<String, CompilationResult.ContractMetadata> contractMeta = iterator.next();
        int i = 0;
        while (iterator.hasNext() && i < contractSize) {
            contractMeta = iterator.next();
            i++;
        }
        String[] split = contractMeta.getKey().split(":");
        String contractName = split[split.length - 1];
        String abi = contractMeta.getValue().abi;
        String opCode = contractMeta.getValue().bin;

        return DeployContractParam.builder()
                .abi(abi)
                .contractName(contractName)
                .contractByteCodes(opCode)
                .build();
    }

    /**
     * compile single contract from a file of type 'sol'
     * @param contract contract content
     * @return
     * @throws ContractException
     */
    public DeployContractParam compileSingleContractFromContent(String contract) throws ContractException {
        Result res;
        try {
            res = SolidityCompiler.compile(contract.getBytes(), true,
                    SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        } catch (IOException e) {
            log.error("Compile contract error, io exception: ", e);
            throw new ContractException("Compilation io exception: " + e.getMessage());
        }
        if (!res.errors.isEmpty()) {
            log.error("Compilation contract error: " + res.errors);
            throw new ContractException("Compilation error: " + res.errors);
        }
        if (res.output.isEmpty()) {
            log.error("Compilation contract error:  output is empty: " + res.errors);
            throw new ContractException("compilation error, output is empty: " + res.errors);
        }

        // read from JSON and assemble request body
        CompilationResult result;
        try {
            result = CompilationResult.parse(res.output);
        } catch (IOException e) {
            log.error("Compilation contract error: Parse result error, io exception: ", e);
            throw new ContractException("parse result error, io exception: " + e.getMessage());
        }
        if (result.getContracts().size() == 0) {
            log.error("Compilation contract error: No Contract found after compile");
            throw new RuntimeException("Compilation error: No Contract found after compile" + result);
        }
        if (result.getContracts().size() > 1) {
            log.error("Compilation contract error: Multiple Contracts found after compile");
            throw new RuntimeException("Compilation contract error: Multiple Contracts found after compile" + result);
        }
        Map.Entry<String, CompilationResult.ContractMetadata> contractMeta = result.getContracts().entrySet().iterator().next();
        String[] split = contractMeta.getKey().split(":");
        String contractName = split[split.length - 1];
        String abi = contractMeta.getValue().abi;
        String opCode = contractMeta.getValue().bin;

        return DeployContractParam.builder()
                .abi(abi)
                .contractName(contractName)
                .contractByteCodes(opCode)
                .build();
    }

    public SmartContract getContract(byte[] address) {
        return rpcCli.getContract(address);
    }

    public boolean clearContractABI(byte[] ownerAddress, byte[] privateKey, byte[] contractAddress) {
        if (!checkOwnerAddressAndPrivateKey(ownerAddress, privateKey)){
            ownerAddress = DecodeUtil.decode(signerAddress);
            privateKey = DecodeUtil.decode(signerPrivateKey);
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
    public TriggerContractReturn triggerContract(byte[] ownerAddress, byte[] privateKey, TriggerContractParam param) {
        if (!checkOwnerAddressAndPrivateKey(ownerAddress, privateKey)){
            ownerAddress = DecodeUtil.decode(signerAddress);
            privateKey = DecodeUtil.decode(signerPrivateKey);
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
    public DeployContractReturn deployContract(byte[] ownerAddress, byte[] privateKey, DeployContractParam param) throws CreateContractExecption {
        if (!checkOwnerAddressAndPrivateKey(ownerAddress, privateKey)){
            ownerAddress = DecodeUtil.decode(signerAddress);
            privateKey = DecodeUtil.decode(signerPrivateKey);
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

    private TriggerSmartContract triggerCallContract( byte[] ownerAddress, byte[] contractAddress, long callValue, byte[] data) {
        TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(data));
        builder.setCallValue(callValue);
        return builder.build();
    }

    private ClearABIContract createClearABIContract(byte[] ownerAddress, byte[] contractAddress) {
        ClearABIContract.Builder builder = ClearABIContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        return builder.build();
    }

    private boolean processTransaction(TransactionExtention transactionExtention, byte[] privateKey) {
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

    private CreateSmartContract createContract(byte[] ownerAddress, DeployContractParam contractParam) throws CreateContractExecption {
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

    private boolean checkOwnerAddressAndPrivateKey(byte[] ownerAddress, byte[] privateKey){
        if (ByteUtil.isNullOrZeroArray(ownerAddress) || ByteUtil.isNullOrZeroArray(privateKey)){
            log.error("Require account's private key and address to be empty");
            return false;
        }
        return true;
    }
}