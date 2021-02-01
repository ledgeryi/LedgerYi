package cn.ledgeryi.sdk.tests.contract;

import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CompilerTest {

    private LedgerYiApiService ledgerYiApiService;

    @Before
    public void start(){
        ledgerYiApiService = new LedgerYiApiService();
    }

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
    public void compileSingleContractFromContentTest() {
        DeployContractParam result = null;
        try {
            result = ledgerYiApiService.compileSingleContractFromContent(testContratSingle);
        } catch (ContractException e) {
            e.printStackTrace();
            System.out.println("contract compile error: " + e.getMessage());
        }
        System.out.println("name: " + result.getContractName());
        System.out.println("abi: " + result.getAbi());
        System.out.println("code: " + result.getContractByteCodes());
    }

    @Test
    public void compileSingleContractFromFileTest() {
        DeployContractParam result = null;
        try {
            Path source = Paths.get("src","test","resources","Storage.sol");
            result = ledgerYiApiService.compileContractFromFile(source, true);
        } catch (ContractException e) {
            e.printStackTrace();
            System.out.println("contract compile error: " + e.getMessage());
        }
        System.out.println("name: " + result.getContractName());
        System.out.println("abi: " + result.getAbi());
        System.out.println("code: " + result.getContractByteCodes());
    }

    @Test
    public void compileMultipleContractsFromFile(){
        DeployContractParam result = null;
        try {
            Path source = Paths.get("src","test","resources","erc20.sol");
            result = ledgerYiApiService.compileContractFromFile(source, false);
        } catch (ContractException e) {
            e.printStackTrace();
            System.out.println("contract compile error: " + e.getMessage());
        }
        System.out.println("name: " + result.getContractName());
        System.out.println("abi: " + result.getAbi());
        System.out.println("code: " + result.getContractByteCodes());
    }
}
