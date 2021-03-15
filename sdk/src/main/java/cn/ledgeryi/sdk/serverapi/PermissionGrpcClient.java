package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI.*;
import cn.ledgeryi.api.GrpcAPI.GrpcRequest;
import cn.ledgeryi.api.PermissionGrpc;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.AccountYi;
import cn.ledgeryi.sdk.common.utils.ByteUtil;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.common.utils.TransactionUtils;
import cn.ledgeryi.sdk.config.Configuration;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.parse.event.CallTransaction;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.permission.Role;
import cn.ledgeryi.sdk.serverapi.data.permission.RoleTypeEnum;
import cn.ledgeryi.sdk.serverapi.data.permission.User;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.Config;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

/**
 * @author Brian
 * @date 2021/3/15 10:12
 */
@Slf4j
public class PermissionGrpcClient {

    private static final int BROADCAST_TRANSACTION_REPEAT_TIMES = 10;
    private PermissionGrpc.PermissionBlockingStub permissionBlockingStub = null;

    private PermissionGrpcClient(String ledgerYiNode) {
        if (!StringUtils.isEmpty(ledgerYiNode)) {
            ManagedChannel channelFull = ManagedChannelBuilder.forTarget(ledgerYiNode).usePlaintext(true).build();
            permissionBlockingStub = PermissionGrpc.newBlockingStub(channelFull);
        }
    }

    public static PermissionGrpcClient initPermissionGrpcClient() {
        Config config = Configuration.getConfig();
        String ledgerYiNode;
        if (config.hasPath("ledgernode.ip.list") && config.getStringList("ledgernode.ip.list").size() != 0) {
            ledgerYiNode = config.getStringList("ledgernode.ip.list").get(0);
        } else {
            throw new RuntimeException("No connection information is configured!");
        }
        return new PermissionGrpcClient(ledgerYiNode);
    }

    public boolean addRole(RoleTypeEnum roleType, AccountYi caller) {
        GrpcRequest request = createGrpcRequest(roleType, caller);
        TransactionExtention tx = permissionBlockingStub.addNewRole(request);
        return processTransaction(tx, caller);
    }

    public boolean revokeRole(RoleTypeEnum roleType, AccountYi caller){
        GrpcRequest request = createGrpcRequest(roleType, caller);
        TransactionExtention tx = permissionBlockingStub.inactiveRole(request);
        return processTransaction(tx, caller);
    }

    public int getRoleNum(AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam defaultCallParam = ContractCallParam.getDefaultInstance();
        ContractCallParam.Builder builderCallParam = defaultCallParam.toBuilder().setCaller(caller.getAddress());
        builder.setParam(Any.pack(builderCallParam.build()));
        TransactionExtention tx = permissionBlockingStub.getRoleNum(builder.build());
        return ByteUtil.byteArrayToInt(tx.getConstantResult(0).toByteArray());
    }

    public Role getRole(int roleId, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.setArgs(0, roleId+"");
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.getRole(builder.build());
        ByteString constantResult = tx.getConstantResult(0);
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getRole",
                new String[]{"uint256"}, new String[]{"uint32", "bool"});
        Role.RoleBuilder role = Role.builder();
        Object[] objects = function.decodeResult(constantResult.toByteArray());
        for (Object object : objects) {
            if (object instanceof BigInteger) {
                BigInteger _roleId = (BigInteger) object;
                role.roleId(_roleId.intValue());
            } else if (object instanceof Boolean) {
                boolean active = (Boolean) object;
                role.active(active);
            }
        }
        return role.build();
    }

    public boolean revokeRoleOfUser(String userId, RoleTypeEnum roleType,
                                    String user, AccountYi caller){
        GrpcRequest request = createUserOperationRequest(userId, roleType, user, caller);
        TransactionExtention tx = permissionBlockingStub.assignRoleForUser(request);
        return processTransaction(tx, caller);
    }

    public boolean hasRoleOfUser(String userId, RoleTypeEnum roleType,
                                 String user, AccountYi caller){
        GrpcRequest request = createUserOperationRequest(userId, roleType, user, caller);
        TransactionExtention tx = permissionBlockingStub.hasRole(request);
        return processTransaction(tx, caller);
    }

    public boolean assignRoleForUser(RoleTypeEnum roleType, String user, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.setArgs(0, roleType.getType() + "");
        callParam.setArgs(1, user);
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.assignRoleForUser(builder.build());
        return processTransaction(tx, caller);
    }

    public int getUserNum(AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam defaultCallParam = ContractCallParam.getDefaultInstance();
        ContractCallParam.Builder builderCallParam = defaultCallParam.toBuilder().setCaller(caller.getAddress());
        builder.setParam(Any.pack(builderCallParam.build()));
        TransactionExtention tx = permissionBlockingStub.getUserNum(builder.build());
        return ByteUtil.byteArrayToInt(tx.getConstantResult(0).toByteArray());
    }

    //todo check
    public User getUser(int userIndex, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.setArgs(0, userIndex+"");
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.getUser(builder.build());
        ByteString constantResult = tx.getConstantResult(0);
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getUser",
                new String[]{"uint256"}, new String[]{"bytes32", "uint32", "address", "bool"});
        User.UserBuilder user = User.builder();
        Object[] objects = function.decodeResult(constantResult.toByteArray());
        for (Object object : objects) {
            /*if (object instanceof BigInteger) {
                BigInteger id = (BigInteger) object;
                user.userId(id.intValue());
            } else */if (object instanceof byte[]) {
                String address = DecodeUtil.createReadableString((byte[]) object);
                user.address(address);
            } else if (object instanceof Boolean) {
                boolean active = (Boolean) object;
                user.active(active);
            }//todo
        }
        return user.build();
    }

    public boolean addNode(cn.ledgeryi.sdk.serverapi.data.permission.Node newNode, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.setArgs(0, newNode.getOwner());
        callParam.setArgs(1, newNode.getHost());
        callParam.setArgs(2, newNode.getPort()+"");
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.addNewNode(builder.build());
        return processTransaction(tx, caller);
    }

    public boolean updateNode(cn.ledgeryi.sdk.serverapi.data.permission.Node newNode, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.setArgs(0, newNode.getNodeId());
        callParam.setArgs(1, newNode.getOwner());
        callParam.setArgs(2, newNode.getHost());
        callParam.setArgs(3, newNode.getPort()+"");
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.updateNode(builder.build());
        return processTransaction(tx, caller);
    }

    public boolean deleteNode(String nodeId, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.setArgs(0, nodeId);
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.deleteNode(builder.build());
        return processTransaction(tx, caller);
    }

    public int getNodeNum(AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam defaultCallParam = ContractCallParam.getDefaultInstance();
        ContractCallParam.Builder builderCallParam = defaultCallParam.toBuilder().setCaller(caller.getAddress());
        builder.setParam(Any.pack(builderCallParam.build()));
        TransactionExtention tx = permissionBlockingStub.getNodeNum(builder.build());
        return ByteUtil.byteArrayToInt(tx.getConstantResult(0).toByteArray());
    }

    //todo
    public cn.ledgeryi.sdk.serverapi.data.permission.Node getNode(int nodeIndex, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.setArgs(0, nodeIndex+"");
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.getNode(builder.build());
        ByteString constantResult = tx.getConstantResult(0);
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getNode",
                new String[]{"uint32"}, new String[]{"bytes32", "address", "string", "uint32"});
        cn.ledgeryi.sdk.serverapi.data.permission.Node.NodeBuilder nodeBuilder =
                cn.ledgeryi.sdk.serverapi.data.permission.Node.builder();
        Object[] objects = function.decodeResult(constantResult.toByteArray());
        for (Object object : objects) {
            /*if (object instanceof BigInteger) {
                BigInteger id = (BigInteger) object;
                user.userId(id.intValue());
            } else */if (object instanceof byte[]) {
                String address = DecodeUtil.createReadableString((byte[]) object);
                nodeBuilder.owner(address);
            } else if (object instanceof String) {
                String host = (String) object;
                nodeBuilder.host(host);
            }
        }
        return nodeBuilder.build();
    }

    public DeployContractReturn deployContract(DeployContractParam param, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        //SmartContractOuterClass.CreateSmartContract createContract = createContract(DecodeUtil.decode(caller.getAddress()), param);
        TransactionExtention tx = permissionBlockingStub.deployContract(builder.build());
        boolean result = processTransaction(tx, caller);
        if (result) {
            SmartContractOuterClass.CreateSmartContract createSmartContract;
            try {
                Any parameter = tx.getTransaction().getRawData().getContract().getParameter();
                createSmartContract = parameter.unpack(SmartContractOuterClass.CreateSmartContract.class);
            } catch (InvalidProtocolBufferException e) {
                log.error("deploy contract, parameter parse fail");
                return null;
            }
            String contractByteCodes = DecodeUtil.createReadableString(createSmartContract.getNewContract().getBytecode());
            String contractAddress = DecodeUtil.createReadableString(
                    TransactionUtils.generateContractAddress(tx.getTransaction(), DecodeUtil.decode(caller.getAddress())));
            return DeployContractReturn.builder()
                    .transactionId(DecodeUtil.createReadableString(tx.getTxid()))
                    .contractName(createSmartContract.getNewContract().getName())
                    .contractByteCodes(contractByteCodes)
                    .ownerAddress(caller.getAddress())
                    .contractAddress(contractAddress)
                    .contractAbi(createSmartContract.getNewContract().getAbi().toString())
                    .build();
        } else {
            log.error("deploy contract failed.");
            return null;
        }
    }

    private SmartContractOuterClass.CreateSmartContract createContract(byte[] ownerAddress, DeployContractParam contractParam)
            throws CreateContractExecption {
        String contractAbi = contractParam.getAbi();
        if (StringUtils.isEmpty(contractAbi)) {
            log.error("deploy contract, abi is null");
            throw new CreateContractExecption("deploy contract, abi is null");
        }
        SmartContractOuterClass.SmartContract.ABI abi = JsonFormatUtil.jsonStr2ABI(contractAbi);
        SmartContractOuterClass.SmartContract.Builder builder = SmartContractOuterClass.SmartContract.newBuilder();
        builder.setAbi(abi);
        builder.setName(contractParam.getContractName());
        //set contract owner
        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        byte[] byteCode = Hex.decode(contractParam.getContractByteCodes());
        builder.setBytecode(ByteString.copyFrom(byteCode));
        SmartContractOuterClass.CreateSmartContract.Builder createSmartContractBuilder = SmartContractOuterClass.CreateSmartContract.newBuilder();
        //set call address
        createSmartContractBuilder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        createSmartContractBuilder.setNewContract(builder.build());
        return createSmartContractBuilder.build();
    }

    private GrpcRequest createUserOperationRequest(String userId, RoleTypeEnum roleType,
                                                   String user, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.setArgs(0, userId);
        callParam.setArgs(1, roleType.getType()+"");
        callParam.setArgs(2, user);
        builder.setParam(Any.pack(callParam.build()));
        return builder.build();
    }

    private GrpcRequest createGrpcRequest(RoleTypeEnum roleType, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.setArgs(0, roleType.getType() + "");
        builder.setParam(Any.pack(callParam.build()));
        return builder.build();
    }

    private boolean processTransaction(TransactionExtention transactionExtention, AccountYi caller) {
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            log.error("result is false, code:{}, message:{}", ret.getCode(), ret.getMessage().toStringUtf8());
            return false;
        }
        Protocol.Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContract() == null) {
            log.error("transaction or contract is null");
            return false;
        }
        transaction = TransactionUtils.sign(transaction, DecodeUtil.decode(caller.getPrivateKeyStr()));
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        builder.setParam(Any.pack(transaction));
        int repeatTimes = BROADCAST_TRANSACTION_REPEAT_TIMES;
        Return response = permissionBlockingStub.broadcastTransaction(builder.build());
        while (!response.getResult() && response.getCode() == Return.response_code.SERVER_BUSY && repeatTimes > 0) {
            repeatTimes--;
            response = permissionBlockingStub.broadcastTransaction(builder.build());
            log.info("broadcast tx, repeat times: " + (BROADCAST_TRANSACTION_REPEAT_TIMES - repeatTimes + 1));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!response.getResult()) {
            log.error("broadcast tx, code: {}, message: {}" + response.getCode(), response.getMessage().toStringUtf8());
        }
        return response.getResult();
    }
}
