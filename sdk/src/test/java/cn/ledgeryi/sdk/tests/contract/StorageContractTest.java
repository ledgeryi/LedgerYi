package cn.ledgeryi.sdk.tests.contract;

import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.ByteUtil;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.event.CallTransaction;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import cn.ledgeryi.sdk.serverapi.data.*;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StorageContractTest {

    private static String privateKey = "ec19148056c4cfc5fc1b1923b8bb657e1e481a8f092415d5af96dd60f3e6806d";
    private static String ownerAddress = "42979c83d087b612fdc82c560b3131b9c7f34a76";

    private LedgerYiApiService ledgerYiApiService;

    @Before
    public void start(){
        ledgerYiApiService = new LedgerYiApiService();
    }

    @Test
    public void compileContractTest() {
        DeployContractParam result = null;
        try {
            Path source = Paths.get("src","test","resources","Storage.sol");
            result = ledgerYiApiService.compileContractFromFile(source, "Storage");
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
            Path source = Paths.get("src","test","resources","Storage.sol");
            result = ledgerYiApiService.compileContractFromFile(source, "Storage");
            //RequestUserInfo requestUserInfo = RequestUserInfo.builder().address(ownerAddress).roleId(3).build();
            deployContract = ledgerYiApiService.deployContract(DecodeUtil.decode(ownerAddress),
                    DecodeUtil.decode(privateKey), result);
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
    private static String contractAddress = "080d49c962d6d2c92b799c87dda86036b43ee4d8";

    @Test
    public void getContractFromOnChain(){
        SmartContractOuterClass.SmartContract contract = ledgerYiApiService.getContract(DecodeUtil.decode(contractAddress));
        System.out.println(JsonFormatUtil.printSmartContract(contract));
        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(contract.getAbi()));
        String abi = jsonObject.getString("entrys");
        System.out.println(abi);
    }

    @Test
    public void addData() {
        List<Object> args = Collections.singletonList("b");
        String method = "addData(string)";
        triggerContract(method, args,false);
    }

    @Test
    public void getData() {
        List<Object> args = Collections.emptyList();
        String method = "getData()";
        ByteString triggerContract = triggerContract(method, args,true);
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getData",
                new String[]{}, new String[]{"string[]"});
        Object[] objects = function.decodeResult(triggerContract.toByteArray());
        for (Object object : objects) {
            if (object instanceof String) {
                String data = (String) object;
                System.out.println("data: " + data);
            } else if (object instanceof BigInteger) {
                BigInteger id = (BigInteger) object;
                System.out.println("id: " + id);
            } else if (object instanceof byte[]) {
                String address = DecodeUtil.createReadableString((byte[]) object);
                System.out.println("address: " + address);
            } else if (object instanceof Boolean) {
                boolean uesd = (Boolean) object;
                System.out.println("uesd: " + uesd);
            } else if (object instanceof Object[]) {
                Object[] object1 = (Object[]) object;
                for (int i = 0; i < (object1).length; i++) {
                    Object objects1 = object1[i];
                    System.out.println("data: " + (String) objects1);
                }
            }
        }
    }

    @Test
    public void triggerStorage() {
        List args = Arrays.asList(3,"a423eccf212820bf0869e9735d63f84fdf373795");
        String method = "store(uint32,address)";
        triggerContract(method, args,false);
    }

    @Test
    public void triggerUsed() {
        List args = Arrays.asList(3);
        String method = "checkUsed(uint32)";
        ByteString triggerContract = triggerContract(method, args, true);
        System.out.println(ByteUtil.byteArrayToInt(triggerContract.toByteArray()));
    }

    @Test
    public void triggerRetrieve() {
        List args = Arrays.asList(3);
        String method = "retrieve(uint32)";
        ByteString triggerContract = triggerContract(method, args, true);

        CallTransaction.Function function = CallTransaction.Function.fromSignature("retrieve",
                new String[]{"uint32"}, new String[]{"address", "uint32", "bool"});
        Object[] objects = function.decodeResult(triggerContract.toByteArray());
        for (Object object : objects) {
            if (object instanceof String) {
                String data = (String) object;
                System.out.println("data: " + data);
            } else if (object instanceof BigInteger) {
                BigInteger id = (BigInteger) object;
                System.out.println("id: " + id);
            } else if (object instanceof byte[]) {
                String address = DecodeUtil.createReadableString((byte[]) object);
                System.out.println("address: " + address);
            } else if (object instanceof Boolean) {
                boolean uesd = (Boolean) object;
                System.out.println("uesd: " + uesd);
            }
        }
    }

    /*@Test
    public void clearContractAbi(){
        boolean result = ledgerYiApiService.clearContractABI(DecodeUtil.decode(ownerAddress),
                DecodeUtil.decode(privateKey), DecodeUtil.decode(contractAddress));
        System.out.println("clear result: " +  result);
    }*/

    private ByteString triggerContract(String method, List<Object> args, boolean isConstant) {
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
        return result.getCallResult();
    }
}
