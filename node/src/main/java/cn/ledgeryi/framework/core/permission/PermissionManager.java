package cn.ledgeryi.framework.core.permission;

import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.framework.common.utils.AbiUtil;
import cn.ledgeryi.framework.core.Wallet;
import cn.ledgeryi.framework.core.exception.HeaderNotFound;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j(topic = "permission")
@Component
public class PermissionManager {

    @Autowired
    private ApplicationContext ctx;

    private String nodeMgrAddress;

    private String roleMgrAddress;

    private String guardianAccount;

    private static final String PERMISSION_CONFIG = "permission/permission-config.json";

    private void getContractAddress(){
        try {
            if (StringUtils.isEmpty(roleMgrAddress) || StringUtils.isEmpty(nodeMgrAddress)){
                String path = System.getProperty("user.dir");
                File file = new File(path + "/" + PERMISSION_CONFIG);
                String json = FileUtils.readFileToString(file, "UTF-8");
                JSONObject jsonObject = JSON.parseObject(json);
                nodeMgrAddress = jsonObject.getString("nodeManagerAddress");
                roleMgrAddress = jsonObject.getString("roleManagerAddress");
                guardianAccount = jsonObject.getString("guardianAccount");
            }
        } catch (IOException e) {
            log.error("parse contract address fail, error： ", e.getMessage());
            throw new RuntimeException("parse contract address error");
        }
    }

    // check a user has a specified role.
    public boolean hasRole(String requestAddress, String requestRole) {
        getContractAddress();
        List<Object> args = Arrays.asList(requestAddress, requestRole);
        byte[] methodDecode = Hex.decode(AbiUtil.parseMethod("hasRole(address,uint8)", args));
        ByteString callResult = callConstantContact(roleMgrAddress, methodDecode);
        if (callResult == null) {
            return false;
        }
        return ByteUtil.byteArrayToInt(callResult.toByteArray()) == 1;
    }

    private ByteString callConstantContact(String contractAddress, byte[] methodDecode){
        TriggerSmartContract triggerSmartContract = TriggerSmartContract.newBuilder()
                .setContractAddress(ByteString.copyFromUtf8(contractAddress))
                .setData(ByteString.copyFrom(methodDecode))
                .setOwnerAddress(ByteString.copyFromUtf8(guardianAccount))
                .build();

        TransactionCapsule txCap;
        ProgramResult programResult;
        try {
            Wallet wallet = ctx.getBean(Wallet.class);
            txCap = wallet.createTransactionCapsule(triggerSmartContract, ContractType.TriggerSmartContract);
            programResult = wallet.localCallConstantContract(txCap);
        } catch (ContractValidateException | HeaderNotFound e) {
            log.error("call contract [{}] fail", contractAddress);
            return null;
        }
        return ByteString.copyFrom(programResult.getHReturn());
    }
}
