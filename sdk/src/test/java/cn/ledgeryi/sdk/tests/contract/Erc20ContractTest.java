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
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.Strings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Erc20ContractTest {

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
            Path source = Paths.get("src","test","resources","erc20.sol");
            result = ledgerYiApiService.compileContractFromFile(source,"Erc20",false);
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
            Path source = Paths.get("src","test","resources","erc20.sol");
            result = ledgerYiApiService.compileContractFromFile(source,"Erc20",false);
            result.setConstructor("constructor(string,string,uint256)");
            ArrayList<Object> args = Lists.newArrayList();
            args.add("ERC20Basic");
            args.add("BSC");
            args.add(1000000000);
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
    private static String contractAddress = "ffdfafc401d7815452bcac04a2aa58263fc3a4b8";

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
        List args = Arrays.asList(contractAddress,"5");
        String method = "mint(address,uint256)";
        triggerContract(method, args,false);
    }

    @Test
    public void balanceOf() {
        List args = Arrays.asList("bb8d66327f7bf0e91e7b7c118aebeca4f730de64");
        String method = "balanceOf(address)";
        TriggerContractReturn result = triggerContract(method, args, true);
        System.out.println("trigger contract result: " + ByteUtil.bytesToBigInteger(result.getCallResult().toByteArray()));
    }

    @Test
    public void burn() {
        for (int i = 0; i < 1; i++) {
            List args = Arrays.asList(contractAddress,"1");
            String method = "burn(address,uint256)";
            triggerContract(method, args, false);
        }
    }

    @Test
    public void totalSupply() {
        List args = Collections.EMPTY_LIST;
        String method = "totalSupply()";
        TriggerContractReturn result = triggerContract(method, args, true);
        System.out.println("trigger contract result: " + ByteUtil.bytesToBigInteger(result.getCallResult().toByteArray()));
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

    @Test
    public void transfer() {
        String receiver = "bb8d66327f7bf0e91e7b7c118aebeca4f730de64";
        List args = Arrays.asList(receiver,"500");
        String method = "transfer(address,uint256)";
        triggerContract(method, args, false);
    }

    @Test
    public void bachTransfer() {
        for (int i = 0; i < 10; i++) {
            String receiver = contractAddress;
            List args = Arrays.asList(receiver,"1");
            String method = "transfer(address,uint256)";
            triggerContract(method, args, false);
        }
    }

    @Test
    public void clearContractAbi(){
        boolean result = ledgerYiApiService.clearContractABI(DecodeUtil.decode(ownerAddress),
                DecodeUtil.decode(privateKey), DecodeUtil.decode(contractAddress));
        System.out.println("clear result: " +  result);
    }

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
