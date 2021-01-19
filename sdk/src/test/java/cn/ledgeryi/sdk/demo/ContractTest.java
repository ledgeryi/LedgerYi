package cn.ledgeryi.sdk.demo;

import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.AccountYi;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.common.utils.LedgerYiUtils;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractParam;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractReturn;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

public class ContractTest {

    private static String privateKey = "e8b5177d5a69898dcc437d0e96a9343a37bac001cb9bec7a90b660eee66b4587";
    private static String ownerAddress = "ada95a8734256b797efcd862e0b208529283ac56";

    private LedgerYiApiService ledgerYiApiService;

    @Before
    public void start(){
        ledgerYiApiService = new LedgerYiApiService();
        createOwner();
    }

    private void createOwner(){
        AccountYi accountYi = LedgerYiUtils.createAccountYi();
        if (StringUtils.isEmpty(privateKey)){
            privateKey = accountYi.getPrivateKeyStr();
            System.out.println("privateKey: " + privateKey);
        }
        if (StringUtils.isEmpty(ownerAddress)){
            ownerAddress = accountYi.getAddress();
            System.out.println("address: " + ownerAddress);
        }
    }

    /**
     * single contract demo
     */
    private static String testContratSingle = "// SPDX-License-Identifier: GPL-3.0\n" +
            "\n" +
            "pragma solidity ^0.6.9;\n" +
            "\n" +
            "contract Storage {\n" +
            "\n" +
            "    uint256 number;\n" +
            "\n" +
            "    function store(uint256 num) public {\n" +
            "        number = num;\n" +
            "    }\n" +
            "\n" +
            "    function retrieve() public view returns (uint256){\n" +
            "        return number;\n" +
            "    }\n" +
            "}";


    @Test
    public void compileContractTest() {
        DeployContractParam result = null;
        try {
            result = ledgerYiApiService.compileSingleContract(testContratSingle);
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
            result = ledgerYiApiService.compileSingleContract(testContratSingle);
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

    private static String contractAddres = "ead5b5850f779622f9cf0c0ec858adb54037ccb2";

    @Test
    public void getContractFromOnChain(){
        SmartContractOuterClass.SmartContract contract = ledgerYiApiService.getContract(DecodeUtil.decode(contractAddres));
        System.out.println(JsonFormatUtil.printSmartContract(contract));
    }

    @Test
    public void triggerStorage() {
        String args = "5";
        triggerContract("store(uint256)", args,false);
    }

    @Test
    public void triggerRetrieve() {
        triggerContract("retrieve()","", true);
    }

    @Test
    public void clearContractAbi(){
        boolean result = ledgerYiApiService.clearContractABI(DecodeUtil.decode(ownerAddress),
                DecodeUtil.decode(privateKey), DecodeUtil.decode(contractAddres));
        System.out.println("clear result: " +  result);
    }

    /**
     * [1] constant function和非constant function 函数调用从对链上属性是否有更改可分为两种：constant function 和 非constant function。
     * Constant function 是指用 view/pure/constant 修饰的函数。会在调用的节点上直接返回结果，并不以一笔交易的形式广播出去。
     * 非constant function是指需要依托一笔交易的形式被广播的方法调用。函数会改变链上数据的内容，比如转账，改变合约内部变量的值等等。
     *
     * [2] 注意: 如果在合约内部使用create指令（CREATE instruction），即使用view/pure/constant来修饰这个动态创建的合约合约方法，
     * 这个合约方法仍会被当作非constant function，以交易的形式来处理。
     *
     */
    private void triggerContract(String method, String args, boolean isConstant) {
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
            System.out.println("trigger contract result: " + ByteUtil.bytesToBigInteger(result.getCallResult().toByteArray()));
        }
    }
}
