package cn.ledgeryi.framework.core.permission;

import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.chainbase.core.config.args.Master;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.common.utils.Pair;
import cn.ledgeryi.contract.vm.CallTransaction.Function;
import cn.ledgeryi.framework.common.application.Service;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.common.overlay.server.ChannelManager;
import cn.ledgeryi.framework.common.utils.AbiUtil;
import cn.ledgeryi.framework.core.LedgerYi;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.framework.core.exception.HeaderNotFound;
import cn.ledgeryi.framework.core.permission.constant.RoleTypeEnum;
import cn.ledgeryi.framework.core.permission.entity.NewNode;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.sun.org.apache.xpath.internal.Arg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final String PERMISSION_CONFIG = "permission/permission-config.json";
    private final ScheduledExecutorService nodeExecutor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void start() {
        nodeExecutor.scheduleWithFixedDelay(() -> {
            try {
                queryNodeFromLedger();
            } catch (Throwable t) {
                log.error("Exception in permission worker", t);
            }
        }, 60000, 60000, TimeUnit.MILLISECONDS);
    }

    private void queryNodeFromLedger() {
        ChannelManager channelManager = ctx.getBean(ChannelManager.class);
        for (int i = 0; i < getNodeNum(); i++) {
            NewNode newNode = getNodeNetAddress(i);
            byte[] masterAddress = Args.getInstance().getLocalMasters().getMasterAccountAddress();
            if (!DecodeUtil.createReadableString(masterAddress).equals(newNode.getNodeOwner())){
                Node node = new Node(Node.getNodeId(), newNode.getHost(), newNode.getPort());
                channelManager.putNewNode(node);
            }
            if (newNode.isMaster()) {
                Manager dbManager = ctx.getBean(Manager.class);
                Master master = new Master();
                master.setAddress(DecodeUtil.decode(newNode.getNodeOwner()));
                dbManager.addMaster(master);
            }
        }
    }

    private int getNodeNum() {
        if (StringUtils.isEmpty(nodeMgrAddress)) {
            readAddress();
        }
        List args = Collections.EMPTY_LIST;
        byte[] methodDecode = Hex.decode(AbiUtil.parseMethod("numberOfNodes()", args));
        ByteString callResult = callConstantContact(nodeMgrAddress, methodDecode);
        if (callResult == null || callResult.toByteArray().length == 0) {
            return 0;
        }
        return ByteUtil.byteArrayToInt(callResult.toByteArray());
    }

    private NewNode getNodeNetAddress(int index) {
        if (StringUtils.isEmpty(nodeMgrAddress)) {
            readAddress();
        }
        List<Object> args = Arrays.asList(index);
        byte[] methodDecode = Hex.decode(AbiUtil.parseMethod("getNode(uint32)", args));
        ByteString callResult = callConstantContact(nodeMgrAddress, methodDecode);
        if (callResult == null || callResult.toByteArray().length == 0) {
            return null;
        }
        Function function = Function.fromSignature("getNode",
                new String[]{"uint256"}, new String[]{"address", "string", "uint32"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        NewNode newNode = new NewNode();
        for (Object object : objects) {
            if (object instanceof String) {
                String host = (String) object;
                newNode.setHost(host);
            } else if (object instanceof BigInteger) {
                BigInteger port = (BigInteger) object;
                newNode.setPort(port.intValue());
            } else if (object instanceof byte[]) {
                String owner = DecodeUtil.createReadableString((byte[]) object);
                newNode.setNodeOwner(owner);
            }
        }
        //check role
        boolean isMaster = hasRole(newNode.getNodeOwner(), RoleTypeEnum.BLOCK_PRODUCE.getType());
        newNode.setMaster(isMaster);
        return newNode;
    }


    private Map<String,Pair<Integer,Boolean>> roleCache = new ConcurrentHashMap<>();

    // check a user has a specified role.
    public boolean hasRole(String requestAddress, int requestRole) {
        if (StringUtils.isEmpty(roleMgrAddress)) {
            readAddress();
        }

        Pair<Integer, Boolean> role = roleCache.get(requestAddress);
        if (role != null && role.getKey() == requestRole && role.getValue()){
            return true;
        }

        List<Object> args = Arrays.asList(requestAddress, requestRole);
        byte[] methodDecode = Hex.decode(AbiUtil.parseMethod("hasRole(address,uint32)", args));
        ByteString callResult = callConstantContact(roleMgrAddress, methodDecode);
        if (callResult == null) {
            return false;
        }
        boolean permissioned = ByteUtil.byteArrayToInt(callResult.toByteArray()) == 1;
        if (permissioned) {
            Pair<Integer, Boolean> rolePair = new Pair<>(requestRole, true);
            roleCache.put(requestAddress,rolePair);
        } else {
            Pair<Integer, Boolean> rolePair = new Pair<>(requestRole, false);
            roleCache.put(requestAddress,rolePair);
        }
        return permissioned;
    }

    private void readAddress() {
        try {
            String path = System.getProperty("user.dir");
            File file = new File(path + "/" + PERMISSION_CONFIG);
            String json = FileUtils.readFileToString(file, "UTF-8");
            JSONObject jsonObject = JSON.parseObject(json);
            nodeMgrAddress = jsonObject.getString("nodeManagerContractAddress");
            roleMgrAddress = jsonObject.getString("roleManagerContractAddress");
            guardianAccount = jsonObject.getString("guardianAccount");
        } catch (IOException e) {
            log.error("parse contract address fail, error： ", e.getMessage());
            throw new RuntimeException("parse contract address error");
        }
    }

    private ByteString callConstantContact(String contractAddress, byte[] methodDecode) {
        TriggerSmartContract triggerSmartContract = TriggerSmartContract.newBuilder()
                .setContractAddress(ByteString.copyFrom(DecodeUtil.decode(contractAddress)))
                .setData(ByteString.copyFrom(methodDecode))
                .setOwnerAddress(ByteString.copyFrom(DecodeUtil.decode(guardianAccount)))
                .build();

        TransactionCapsule txCap;
        ProgramResult programResult;
        try {
            LedgerYi wallet = ctx.getBean(LedgerYi.class);
            txCap = wallet.createTransactionCapsule(triggerSmartContract, ContractType.TriggerSmartContract);
            programResult = wallet.localCallConstantContract(txCap);
        } catch (ContractValidateException | HeaderNotFound e) {
            log.error("call contract [{}] fail", contractAddress);
            return null;
        }
        return ByteString.copyFrom(programResult.getHReturn());
    }

    public String getGuardianAccount(){
        if (StringUtils.isEmpty(guardianAccount)){
            readAddress();
        }
        return guardianAccount;
    }

    @Override
    public void init() {
    }

    @Override
    public void stop() {
    }
}