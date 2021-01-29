package cn.ledgeryi.sdk.tests.contract;

import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import org.junit.Before;
import org.junit.Test;

public class CompilerTest {

    private LedgerYiApiService ledgerYiApiService;
    private static String privateKey = "e8b5177d5a69898dcc437d0e96a9343a37bac001cb9bec7a90b660eee66b4587";
    private static String ownerAddress = "ada95a8734256b797efcd862e0b208529283ac56";

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

    @Before
    public void start(){
        ledgerYiApiService = new LedgerYiApiService();
    }

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
}
