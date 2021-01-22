package cn.ledgeryi.sdk.tests.contract;

import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
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
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OwnedTokenTest {

    private static String privateKey = "e8b5177d5a69898dcc437d0e96a9343a37bac001cb9bec7a90b660eee66b4587";
    private static String ownerAddress = "ada95a8734256b797efcd862e0b208529283ac56";

    private LedgerYiApiService ledgerYiApiService;

    @Before
    public void start(){
        ledgerYiApiService = new LedgerYiApiService();
    }


    private static String ownedToken = "// SPDX-License-Identifier: GPL-3.0\n" +
            "pragma solidity ^0.6.9;\n" +
            "contract OwnedToken {\n" +
            "    address owner;\n" +
            "    bytes32 name;\n" +
            "    constructor(bytes32 _name) public {\n" +
            "        owner = msg.sender;\n" +
            "        name = _name;\n" +
            "    }\n" +
            "    function changeName(bytes32 newName) public {\n" +
            "        if (msg.sender == owner)\n" +
            "            name = newName;\n" +
            "    }\n" +
            "    function getName() public view returns (bytes32) {\n" +
            "        return name;\n" +
            "    }\n" +
            "}";


    @Test
    public void compileContractTest() {
        DeployContractParam result = null;
        try {
            String contract = ownedToken;
            result = ledgerYiApiService.compileSingleContract(contract);
            result.setConstructor("constructor(bytes32)");
            ArrayList<Object> args = Lists.newArrayList();
            String readableString = Hex.toHexString(("Lili").getBytes());
            args.add(readableString);
            result.setArgs(args);
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
            String contract = ownedToken;
            result = ledgerYiApiService.compileSingleContract(contract);
            result.setConstructor("constructor(bytes32)");
            ArrayList<Object> args = Lists.newArrayList();
            String readableString = Hex.toHexString(("Lili").getBytes());
            args.add(readableString);
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

    //ownedToken
    private static String contractAddres = "81170d4143ba0e19b889065fecf99a411fcfa5b5";

    @Test
    public void getName(){
        List args = Collections.EMPTY_LIST;
        String method = "getName()";
        triggerContract(method, args,true);
    }

    @Test
    public void changeName(){
        ArrayList<Object> args = Lists.newArrayList();
        String toHexString = Hex.toHexString(("bob").getBytes());
        args.add(toHexString);
        String method = "changeName(bytes32)";
        triggerContract(method, args,false);
    }

    @Test
    public void getContractFromOnChain(){
        SmartContractOuterClass.SmartContract contract = ledgerYiApiService.getContract(DecodeUtil.decode(contractAddres));
        System.out.println(JsonFormatUtil.printSmartContract(contract));
        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(contract.getAbi()));
        String abi = jsonObject.getString("entrys");
        System.out.println(abi);
    }

    private void triggerContract(String method, List<Object> args, boolean isConstant) {
        TriggerContractParam triggerContractParam = new TriggerContractParam()
                .setContractAddress(DecodeUtil.decode(contractAddres))
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
        } else {
            System.out.println("trigger contract result: " + Strings.fromByteArray(result.getCallResult().toByteArray()));
        }
    }
}
