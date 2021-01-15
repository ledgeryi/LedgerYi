package cn.ledgeryi.sdk.demo;

import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.contract.compiler.*;
import cn.ledgeryi.sdk.contract.compiler.entity.CompilationResult;
import cn.ledgeryi.sdk.contract.compiler.entity.Result;
import cn.ledgeryi.sdk.serverapi.RequestNodeAPI;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class CompilerTest {

    private static String privateKey = "e8b5177d5a69898dcc437d0e96a9343a37bac001cb9bec7a90b660eee66b4587";
    private static String address = "ada95a8734256b797efcd862e0b208529283ac56";

    private static String testContratSingle1 = "contract Storage {\n" +
            " \n" +
            "    uint256 number;\n" +
            " \n" +
            "    function store(uint256 num) public {\n" +
            "        number = num;\n" +
            "    }\n" +
            " \n" +
            "    function retrieve() public view returns (uint256){\n" +
            "        return number;\n" +
            "    }\n" +
            "}";

    // TODO: Wrap this function in SDK manner
    // TODO: refactor Output

    public static Result compileSingleContract(String contract) throws IOException {
        Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        return res;
    }

    // TODO: refactor Input of priKey
    // TODO: add constructor support
    public static boolean compileThenDeploySingleContract(String contract, String privateKey) throws IOException, RuntimeException {
        Result res = compileSingleContract(contract);

        if (!res.errors.isEmpty()) {
            // TODO: add to logger
            System.out.println("Compilation error: " + res.errors);
        }

        // compile not success
        if (res.output.isEmpty()) {
            throw new RuntimeException("Compilation error: " + res.errors);
        }

        // read from JSON and assemble request body
        CompilationResult result = CompilationResult.parse(res.output);
        System.out.println(result);


        if (result.contracts.size() == 0) {
            throw new RuntimeException("Compilation error: No Contract found after compile" + result);
        }
        if (result.contracts.size() > 1) {
            throw new RuntimeException("Compilation error: Multiple Contracts found after compile" + result);
        }


        Map.Entry<String, CompilationResult.ContractMetadata> contractMeta = result.contracts.entrySet().iterator().next();
        byte[] ownerAddr = DecodeUtil.decode(address);
        String contractName = contractMeta.getKey().split(":")[1];
        String abi = contractMeta.getValue().abi;
        String code = contractMeta.getValue().bin;
        byte[] priKey = DecodeUtil.decode(privateKey);

        RequestNodeAPI.deployContract(ownerAddr, contractName, abi, code, 0, priKey);

        return true;
    }

    @Test
    public void compileContractTest1() throws IOException {
        String contractT1 = testContratSingle1;
        Result res = compileSingleContract(contractT1);
    }

    @Test
    public void deployContractTest1() throws IOException {
        String contractT1 = testContratSingle1;
        compileThenDeploySingleContract(contractT1, privateKey);
    }

}
