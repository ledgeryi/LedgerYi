package cn.ledgeryi.framework.core.permission;

import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.contract.vm.CallTransaction;
import cn.ledgeryi.framework.common.application.Service;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.common.overlay.server.ChannelManager;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "permission")
@Component
public class PermissionService implements Service {

    @Autowired
    private ApplicationContext ctx;

    private String nodeMgrAddress;
    private String roleMgrAddress;
    private String guardianAccount;

    private static final String SPLIT_CHAR = ":";
    private static final String PERMISSION_CONFIG = "permission/permission-config.json";
    private final ScheduledExecutorService nodeExecutor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void start(){
        nodeExecutor.scheduleWithFixedDelay(() -> {
            try {
                queryNodeFromLedger();
            } catch (Throwable t) {
                log.error("Exception in permission worker", t);
            }
        }, 30000, 3600, TimeUnit.MILLISECONDS);//todo
    }

    private void queryNodeFromLedger(){
        ChannelManager channelManager = ctx.getBean(ChannelManager.class);
        for (int i = 0; i < getNodeNum(); i++) {
            String nodeNetAddress = getNodeNetAddress(i);
            if (StringUtils.isEmpty(nodeNetAddress)) {
                continue;
            }
            String host = nodeNetAddress.split(SPLIT_CHAR)[0];
            int port = Integer.valueOf(nodeNetAddress.split(SPLIT_CHAR)[1]);
            Node node = new Node(Node.getNodeId(), host, port);
            channelManager.putNewNode(node);
        }
    }

    private int getNodeNum(){
        if (StringUtils.isEmpty(nodeMgrAddress)){
            readContractAddress();
        }
        List args = Collections.EMPTY_LIST;
        byte[] methodDecode = Hex.decode(AbiUtil.parseMethod("nodeNum()", args));
        ByteString callResult = callConstantContact(roleMgrAddress, methodDecode);
        if (callResult == null || callResult.toByteArray().length == 0) {
            return 0;
        }
        return ByteUtil.byteArrayToInt(callResult.toByteArray());
    }

    private String getNodeNetAddress(int index){
        if (StringUtils.isEmpty(nodeMgrAddress)){
            readContractAddress();
        }
        List<Object> args = Arrays.asList(index);
        byte[] methodDecode = Hex.decode(AbiUtil.parseMethod("getNodeDetails()", args));
        ByteString callResult = callConstantContact(roleMgrAddress, methodDecode);
        if (callResult == null || callResult.toByteArray().length == 0) {
            return null;
        }
        //todo
        CallTransaction.Function function = CallTransaction.Function.fromSignature(
                "getNodeDetails",new String[]{"uint256"},new String[]{"address","string","uint256"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        return objects[0] + SPLIT_CHAR + objects[1];
    }

    // check a user has a specified role.
    public boolean hasRole(String requestAddress, int requestRole) {
        if (StringUtils.isEmpty(roleMgrAddress)){
            readContractAddress();
        }
        List<Object> args = Arrays.asList(requestAddress, requestRole);
        byte[] methodDecode = Hex.decode(AbiUtil.parseMethod("hasRole(address,uint8)", args));
        ByteString callResult = callConstantContact(roleMgrAddress, methodDecode);
        if (callResult == null) {
            return false;
        }
        return ByteUtil.byteArrayToInt(callResult.toByteArray()) == 1;
    }

    private void readContractAddress(){
        try {
            String path = System.getProperty("user.dir");
            File file = new File(path + "/" + PERMISSION_CONFIG);
            String json = FileUtils.readFileToString(file, "UTF-8");
            JSONObject jsonObject = JSON.parseObject(json);
            nodeMgrAddress = jsonObject.getString("nodeManagerAddress");
            roleMgrAddress = jsonObject.getString("roleManagerAddress");
            guardianAccount = jsonObject.getString("guardianAccount");
        } catch (IOException e) {
            log.error("parse contract address fail, error： ", e.getMessage());
            throw new RuntimeException("parse contract address error");
        }
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

    @Override
    public void init () {
    }

    @Override
    public void stop() {
    }

}
