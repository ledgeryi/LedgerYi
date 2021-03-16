package cn.ledgeryi.sdk.contract;

import cn.ledgeryi.sdk.contract.compiler.SolidityCompiler;
import cn.ledgeryi.sdk.contract.compiler.entity.CompilationResult;
import cn.ledgeryi.sdk.contract.compiler.entity.Library;
import cn.ledgeryi.sdk.contract.compiler.entity.Result;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Brian
 * @date 2021/3/16 14:31
 */
@Slf4j
public class ContactCompileUtil {
    /**
     * compile contract from a file of type 'sol',
     * support 'library'
     * @param source contract file path
     * @param contractName contract name
     * @param library contract library
     * @throws ContractException
     */
    public static DeployContractParam compileContractFromFileNeedLibrary(
            Path source, String contractName, Library library) throws ContractException {
        return compileContractFromFiles(source, contractName,true, library);
    }

    /**
     * compile contract from a file of type 'sol',
     * support 'Contract inheritance', not support 'library'
     *
     * @param source contract file path
     * @param contractName contract name
     */
    public static DeployContractParam compileContractFromFile(Path source, String contractName) throws ContractException {
        return compileContractFromFiles(source,contractName,false,null);
    }

    public static DeployContractParam compileContractFromFiles(Path source, String contractName, boolean isNeedLibrary,
                                                         Library library) throws ContractException {
        Result res;
        try {
            //SolidityCompiler.Option allowPathsOption = new SolidityCompiler.Options.AllowPaths(Collections.singletonList(source.getParent().toFile()));
            res = SolidityCompiler.compileSrc(source.toFile(), isNeedLibrary, library, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN, SolidityCompiler.Options.INTERFACE/*, allowPathsOption*/);
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

        Iterator<Map.Entry<String, CompilationResult.ContractMetadata>> iterator = result.getContracts().entrySet().iterator();
        Map.Entry<String, CompilationResult.ContractMetadata> contractMeta = null;
        while (iterator.hasNext()) {
            Map.Entry<String, CompilationResult.ContractMetadata> next = iterator.next();
            String[] split = next.getKey().split(":");
            String contractNameOfParse = split[split.length - 1];
            if (contractName.equals(contractNameOfParse)){
                contractMeta = next;
                break;
            }
        }
        if (contractMeta == null) {
            log.error("Compilation contract error: The contract names are inconsistent, input: {}", contractName);
            throw new ContractException("compilation error, The contract names are inconsistent");
        }
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
    public static DeployContractParam compileSingleContractFromContent(String contract) throws ContractException {
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
}
