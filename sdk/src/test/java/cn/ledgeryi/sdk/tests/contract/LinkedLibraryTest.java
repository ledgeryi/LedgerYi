package cn.ledgeryi.sdk.tests.contract;

import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.ByteUtil;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.contract.compiler.entity.Library;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractParam;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractReturn;
import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LinkedLibraryTest {
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
            Library library = new Library();
            String libraryAddress = "1aa005c69039e27b2c2c3f462047b752feccd8e0";
            Path math = Paths.get("src","test","resources","library","Math.sol");
            library.addLibrary(math.toFile().getAbsolutePath().replace("\\","/").concat(":").concat("Math"),libraryAddress);
            Path source = Paths.get("src","test","resources","library","LinkedLibrary.sol");
            result = ledgerYiApiService.compileContractFromFileNeedLibrary(source, "LinkedLibrary", library);
        } catch (ContractException e) {
            e.printStackTrace();
            System.out.println("contract compile error: " + e.getMessage());
        }
        System.out.println("name: " + result.getContractName());
        System.out.println("abi: " + result.getAbi());
        System.out.println("code: " + result.getContractByteCodes());
    }


    /**
     * D:\LedgerYi\sdk\src\main\resources\native\win\solc\solc.exe
     * --optimize
     * --combined-json
     * bin,abi,interface
     * D:/LedgerYi/sdk/src/test/resources/library/LinkedLibrary.sol
     * --libraries
     * D:/LedgerYi/sdk/src/test/resources/library/Math.sol:Math:ada95a8734256b797efcd862e0b208529283ac56
     */
    @Test
    public void compileAndDeployContract(){
        DeployContractParam result = null;
        DeployContractReturn deployContract = null;
        try {
            Path math = Paths.get("src","test","resources","library","Math.sol");
            Library library = new Library();
            String libraryAddress = "1aa005c69039e27b2c2c3f462047b752feccd8e0";
            library.addLibrary(math.toFile().getAbsolutePath().replace("\\","/").concat(":").concat("Math"),libraryAddress);
            Path source = Paths.get("src","test","resources","library","LinkedLibrary.sol");
            result = ledgerYiApiService.compileContractFromFileNeedLibrary(source, "LinkedLibrary", library);
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
    private static String contractAddress = "627a2343060d14a48950ac38fdd15cad7f38cc33";

    @Test
    public void getContractFromOnChain(){
        SmartContractOuterClass.SmartContract contract = ledgerYiApiService.getContract(DecodeUtil.decode(contractAddress));
        System.out.println(JsonFormatUtil.printSmartContract(contract));
        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(contract.getAbi()));
        String abi = jsonObject.getString("entrys");
        System.out.println(abi);
    }

    @Test
    public void currentBalance() {
        List args = Collections.EMPTY_LIST;
        String method = "currentBalance()";
        TriggerContractReturn result = triggerContract(method, args,true);
        System.out.println("trigger contract result: " + ByteUtil.bytesToBigInteger(result.getCallResult().toByteArray()));
    }

    @Test
    public void transfer() {
        List args = Arrays.asList(6);
        String method = "transfer(uint256)";
        triggerContract(method, args,false);
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
