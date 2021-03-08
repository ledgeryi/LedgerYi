package cn.ledgeryi.sdk.tests.contract;

import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.ByteUtil;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractParam;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractReturn;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.sun.org.apache.bcel.internal.generic.FADD;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.Strings;
import org.spongycastle.util.encoders.Hex;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Erc721ContractTest {

    private static String privateKey = "e8b5177d5a69898dcc437d0e96a9343a37bac001cb9bec7a90b660eee66b4587";
    private static String ownerAddress = "ada95a8734256b797efcd862e0b208529283ac56";
    private LedgerYiApiService ledgerYiApiService;

    @Before
    public void start(){
        ledgerYiApiService = new LedgerYiApiService();
    }
    @Test
    public void compileContractTest() {
        DeployContractParam result = null;
        try {
            Path source = Paths.get("src","test","resources","ERC721NFT.sol");
            result = ledgerYiApiService.compileContractFromFile(source, "ERC721NFT");
        } catch (ContractException e) {
            e.printStackTrace();
            System.out.println("contract compile error: " + e.getMessage());
        }
        System.out.println("name: " + result.getContractName());
        System.out.println("abi: " + result.getAbi());
        System.out.println("code: " + result.getContractByteCodes());
    }

    @Test
    public void compileAndDeployContract(){
        DeployContractParam result = null;
        DeployContractReturn deployContract = null;
        try {
            Path source = Paths.get("src","test","resources","ERC721NFT.sol");
            result = ledgerYiApiService.compileContractFromFile(source,"ERC721NFT");
            result.setConstructor("constructor(string,string,uint256)");
            ArrayList<Object> args = Lists.newArrayList();
            args.add("ERC721Basic");
            args.add("BSC");
            args.add(10000);
            result.setArgs(args);
            deployContract = ledgerYiApiService.deployContract(DecodeUtil.decode(ownerAddress), DecodeUtil.decode(privateKey), result);
        } catch (ContractException | CreateContractExecption e) {
            e.printStackTrace();
            System.out.println("contract compile error: " + e.getMessage());
        }
        System.out.println("name: " + result.getContractName());
        System.out.println("abi: " + result.getAbi());
        System.out.println("code: " + result.getContractByteCodes());
        System.out.println("contract address: " + deployContract.getContractAddress());
    }

    // contract address
    private static String contractAddress = "5e43292102d33424a1577c42c2dffac3ea20c513";

    @Test
    public void getContractFromOnChain(){
        SmartContractOuterClass.SmartContract contract = ledgerYiApiService.getContract(DecodeUtil.decode(contractAddress));
        System.out.println(JsonFormatUtil.printSmartContract(contract));
        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(contract.getAbi()));
        String abi = jsonObject.getString("entrys");
        System.out.println(abi);
    }

    @Test
    public void mint() {
        List args = Arrays.asList(5);
        String method = "issueTokens(uint256)";
        triggerContract(method, args,false);
    }

    @Test
    public void balanceOf() {
        List args = Arrays.asList(contractAddress);
        String method = "balanceOf(address)";
        TriggerContractReturn result = triggerContract(method, args, true);
        System.out.println("trigger contract result: " + ByteUtil.bytesToBigInteger(result.getCallResult().toByteArray()));
    }

    @Test
    public void tokenByIndex(){
        List args = Arrays.asList(40);
        String method = "tokenByIndex(uint256)";
        TriggerContractReturn result = triggerContract(method, args, true);
        System.out.println("trigger contract result: " + ByteUtil.bytesToBigInteger(result.getCallResult().toByteArray()));
    }

    @Test
    public void ownerOf(){
        List args = Arrays.asList(4);
        String method = "ownerOf(uint256)";
        TriggerContractReturn result = triggerContract(method, args, true);
        System.out.println(DecodeUtil.createReadableString(result.getCallResult()).substring(24));
    }

    @Test
    public void tokenOfOwnerByIndex(){
        List args = Arrays.asList(contractAddress,0);
        String method = "tokenOfOwnerByIndex(address,uint256)";
        TriggerContractReturn result = triggerContract(method, args, true);
        System.out.println("trigger contract result: " + ByteUtil.bytesToBigInteger(result.getCallResult().toByteArray()));
    }

    @Test
    public void burn() {
        List args = Arrays.asList(5);
        String method = "burnToken(uint256)";
        triggerContract(method, args, false);
    }

    @Test
    public void totalSupply() {
        List args = Collections.EMPTY_LIST;
        String method = "totalSupply()";
        TriggerContractReturn result = triggerContract(method, args, true);
        System.out.println("trigger contract result: " + ByteUtil.bytesToBigInteger(result.getCallResult().toByteArray()));
    }

    @Test
    public void transfer() {
        String receiver = contractAddress;
        List args = Arrays.asList(ownerAddress,receiver,4);
        String method = "transferFrom(address,address,uint256)";
        triggerContract(method, args, false);
    }

    @Test
    public void name() {
        List args = Collections.EMPTY_LIST;
        String method = "name()";
        TriggerContractReturn result = triggerContract(method, args, true);
        System.out.println("trigger contract result: " + Strings.fromByteArray(result.getCallResult().toByteArray()).trim());
    }

    @Test
    public void symbol() {
        List args = Collections.EMPTY_LIST;
        String method = "symbol()";
        TriggerContractReturn result = triggerContract(method, args, true);
        System.out.println("trigger contract result: " + Strings.fromByteArray(result.getCallResult().toByteArray()).trim());
    }

    /*@Test
    public void clearContractAbi(){
        boolean result = ledgerYiApiService.clearContractABI(DecodeUtil.decode(ownerAddress),
                DecodeUtil.decode(privateKey), DecodeUtil.decode(contractAddress));
        System.out.println("clear result: " +  result);
    }*/

    private TriggerContractReturn triggerContract(String method, List<Object> args, boolean isConstant) {
        TriggerContractParam triggerContractParam = new TriggerContractParam()
                .setContractAddress(DecodeUtil.decode(contractAddress))
                .setCallValue(0)
                .setConstant(isConstant)
                .setArgs(args)
                .setTriggerMethod(method);

        TriggerContractReturn result = ledgerYiApiService.triggerContract(DecodeUtil.decode(ownerAddress),
                DecodeUtil.decode(privateKey), triggerContractParam);

        String cmdMethodStr = isConstant ? "TriggerConstantContract" : "TriggerContract";
        if (!isConstant) {
            if (result != null) {
                System.out.println("Broadcast the " + cmdMethodStr + " successful.");
            } else {
                System.out.println("Broadcast the " + cmdMethodStr + " failed");
            }
        }
        return result;
    }
}
