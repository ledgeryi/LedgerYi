package cn.ledgeryi.sdk.contract.compiler.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompilationResult {

    @Getter
    private Map<String, ContractMetadata> contracts;
    @Getter
    private String version;

    public static CompilationResult parse(String rawJson) throws IOException {
        if(rawJson == null || rawJson.isEmpty()){
            CompilationResult empty = new CompilationResult();
            empty.contracts = Collections.emptyMap();
            empty.version = "";

            return empty;
        } else {
            return new ObjectMapper().readValue(rawJson, CompilationResult.class);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContractMetadata {
        public String abi;
        public String bin;
        public String solInterface;

        public String getInterface() {
            return solInterface;
        }

        public void setInterface(String solInterface) {
            this.solInterface = solInterface;
        }
    }
}
