package cn.ledgeryi.sdk.tests;

import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractParam;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractReturn;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Slf4j
public abstract class AbstractContractTest {
    protected LedgerYiApiService ledgerYiApiService;

    private String contractAddress;

    public void init() {
        ledgerYiApiService = new LedgerYiApiService();
    }

    /* common test-config-data for contract */
    protected abstract String getPrivateKey();
    protected abstract String getOwnerAddress();

    /**
     * will not re-deploy contract when provide DeployedOwnerAddress
     */
    protected abstract String getDeployedOwnerAddress();

    protected String getContractAddress() {
        return contractAddress;
    }

    public void waitFiveSecondToCompileAndDeployContract(Path solcSourceFilePath, String contractName, String constructor, List<Object> constructorArg){
        if (Objects.nonNull(getDeployedOwnerAddress())) {
            return;
        }

        DeployContractParam result;
        DeployContractReturn deployContract;

        try {
            result = ledgerYiApiService.compileContractFromFile(solcSourceFilePath,contractName);
            result.setConstructor(constructor);
            result.setArgs(constructorArg);
            deployContract = ledgerYiApiService.deployContract(DecodeUtil.decode(getContractAddress()), DecodeUtil.decode(getPrivateKey()), result);
            contractAddress = deployContract.getContractAddress();
            Thread.sleep(5000);
        } catch (ContractException | CreateContractExecption | InterruptedException e) {
            e.printStackTrace();
            log.error("failure deploy contract.", e);
            return;
        }

        System.out.println("name: " + result.getContractName());
        System.out.println("abi: " + result.getAbi());
        System.out.println("code: " + result.getContractByteCodes());
        System.out.println("contract address: " + deployContract.getContractAddress());
    }

    protected TriggerContractReturn triggerContract(String method, List<Object> args, boolean isConstant) {
        if (Objects.isNull(getContractAddress()) && Objects.isNull(getDeployedOwnerAddress())) {
            throw new RuntimeException("must provide contract address or please re-deploy it");
        }

        TriggerContractParam triggerContractParam = new TriggerContractParam()
                .setContractAddress(DecodeUtil.decode(Objects.nonNull(getDeployedOwnerAddress())?getDeployedOwnerAddress():getContractAddress()))
                .setCallValue(0)
                .setConstant(isConstant)
                .setArgs(args)
                .setTriggerMethod(method);

        TriggerContractReturn result = ledgerYiApiService.triggerContract(DecodeUtil.decode(getOwnerAddress()),
                DecodeUtil.decode(getPrivateKey()), triggerContractParam);

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

    protected String writeObjectToJason(Object obj) {
        JSONObject jsonObject = new JSONObject();

        return jsonObject.toJSONString(obj);
    }
}
