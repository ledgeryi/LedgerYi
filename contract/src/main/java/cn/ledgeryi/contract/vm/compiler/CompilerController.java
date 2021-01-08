package cn.ledgeryi.contract.vm.compiler;

import lombok.extern.slf4j.Slf4j;

public class CompilerController {

    static class CompilationResult {
        public String code;
        public CompilationInfo info;

        @Override
        public String toString() {
            return "CompilationResult{" +
                    "code='" + code + '\'' +
                    ", info=" + info +
                    '}';
        }
    }

    static class CompilationInfo {
        public String source;
        public String language;
        public String languageVersion;
        public String compilerVersion;
        //        public CallTransaction.Function[] abiDefinition;
        public String userDoc;
        public String developerDoc;

        @Override
        public String toString() {
            return "CompilationInfo{" +
                    "source='" + source + '\'' +
                    ", language='" + language + '\'' +
                    ", languageVersion='" + languageVersion + '\'' +
                    ", compilerVersion='" + compilerVersion + '\'' +
//                    ", abiDefinition=" + abiDefinition +
                    ", userDoc='" + userDoc + '\'' +
                    ", developerDoc='" + developerDoc + '\'' +
                    '}';
        }
    }

    public CompilationResult eth_compileSolidity(String contract) throws Exception {
        CompilationResult s = null;
        try {
            SolidityCompiler.Result res = SolidityCompiler.compile(
                    contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
            if (!res.errors.isEmpty()) {
                throw new RuntimeException("Compilation error: " + res.errors);
            }
            cn.ledgeryi.contract.vm.compiler.CompilationResult result = cn.ledgeryi.contract.vm.compiler.CompilationResult.parse(res.output);
            CompilationResult ret = new CompilationResult();
            cn.ledgeryi.contract.vm.compiler.CompilationResult.ContractMetadata contractMetadata = result.contracts.values().iterator().next();
////            ret.code = toJsonHex(contractMetadata.bin);
//            ret.info = new CompilationInfo();
//            ret.info.source = contract;
//            ret.info.language = "Solidity";
//            ret.info.languageVersion = "0";
//            ret.info.compilerVersion = result.version;
//            ret.info.abiDefinition = new CallTransaction.Contract(contractMetadata.abi).functions;
            return s = ret;
        } finally {
            log.info("eth_compileSolidity(" + contract + ")" + s);
        }
    }
}
