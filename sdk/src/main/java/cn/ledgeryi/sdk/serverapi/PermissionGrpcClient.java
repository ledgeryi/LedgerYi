package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI.ContractCallParam;
import cn.ledgeryi.api.GrpcAPI.GrpcRequest;
import cn.ledgeryi.api.GrpcAPI.Return;
import cn.ledgeryi.api.GrpcAPI.TransactionExtention;
import cn.ledgeryi.api.PermissionGrpc;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.CreateSmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract;
import cn.ledgeryi.sdk.common.AccountYi;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.common.utils.TransactionUtils;
import cn.ledgeryi.sdk.config.Configuration;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.parse.event.CallTransaction;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.permission.Node;
import cn.ledgeryi.sdk.serverapi.data.permission.Role;
import cn.ledgeryi.sdk.serverapi.data.permission.RoleTypeEnum;
import cn.ledgeryi.sdk.serverapi.data.permission.User;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
        String ledgerYiNode = Configuration.getLedgerYiNet();
        return new PermissionGrpcClient(ledgerYiNode);
    }

    public boolean assignRoleForUser(RoleTypeEnum roleType, AccountYi caller) {
        GrpcRequest request = createGrpcRequest(roleType, caller);
        TransactionExtention tx = permissionBlockingStub.assignRoleForUser(request);
        return processTransaction(tx, caller);
    }

    /**
     * set proposal manager address for node/admin manager
     */
    public void init(AccountYi caller, String proposalMgrAddress) {
        GrpcRequest request = createGrpcRequest(caller, proposalMgrAddress);
        TransactionExtention tx = permissionBlockingStub.init(request);
         processTransaction(tx, caller);
    }

    /**
     * the default admin address is using for call proposal manager contract. just one can meet the require
     */
    public boolean addDefaultAdmin(AccountYi caller, String defaultAdminAddress) {
        GrpcRequest request = createGrpcRequest(caller, defaultAdminAddress);
        TransactionExtention tx = permissionBlockingStub.addDefaultAdmin(request);
        return processTransaction(tx, caller);
    }

    public boolean addNewNode(AccountYi caller, String proposer, String host, int port) {
        long currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        //todo find it in contract
        int allVoterCounter = 1;

        GrpcRequest request = createGrpcRequest(caller, currentTime,  currentTime + 60, allVoterCounter, proposer, host, port);
        TransactionExtention tx = permissionBlockingStub.createProposalWithCreateNode(request);

        return processTransaction(tx, caller);
    }

    public Node queryNodeInfo(AccountYi caller, String address) {
        GrpcRequest request = createGrpcRequest(caller, address);
        TransactionExtention tx = permissionBlockingStub.queryNodeInfo(request);
        processTransaction(tx, caller);

        CallTransaction.Function function = CallTransaction.Function.fromSignature("queryNodeInfo",
                new String[]{"address"}, new String[]{"address", "string", "uint16", "uint8"});
        ByteString constantResult = tx.getConstantResult(0);

        if(constantResult.isEmpty()) {
            return Node.builder().host("Unknown").port(-1).build();
        }

        Object[] objects = function.decodeResult(constantResult.toByteArray());

        Node.NodeBuilder builder = Node.builder();
        builder.host(objects[1].toString()).port(NumberUtils.toInt(objects[2].toString()));

        return builder.build();
    }

    private GrpcRequest createGrpcRequest(AccountYi caller, Object... params) {
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();

        callParam.setCaller(caller.getAddress());
        for (Object param : params) {
            callParam.addArgs(param.toString());
        }

        builder.setParam(Any.pack(callParam.build()));
        return builder.build();
    }

    public boolean reassignRoleForUser(RoleTypeEnum roleType, AccountYi caller) {
        GrpcRequest request = createGrpcRequest(roleType, caller);
        TransactionExtention tx = permissionBlockingStub.reassignRoleForUser(request);
        return processTransaction(tx, caller);
    }

    public Role getRole(int roleId, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.addArgs(roleId+"");
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

    public boolean hasRoleOfUser(String userId, RoleTypeEnum roleType,
                                 String user, AccountYi caller){
        GrpcRequest request = createUserOperationRequest(userId, roleType, user, caller);
        TransactionExtention tx = permissionBlockingStub.hasRole(request);
        byte[] callResult = tx.getConstantResult(0).toByteArray();
        return cn.ledgeryi.common.utils.ByteUtil.byteArrayToInt(callResult) == 1;
    }

    public boolean assignRoleForUser(RoleTypeEnum roleType, String user, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.addArgs(user);
        callParam.addArgs(roleType.getType() + "");
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.assignRoleForUser(builder.build());
        return processTransaction(tx, caller);
    }

    public User getUser(int userIndex, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.addArgs(String.valueOf(userIndex));
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.queryUserInfo(builder.build());
        ByteString constantResult = tx.getConstantResult(0);
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getUser",
                new String[]{"uint256"}, new String[]{"bytes32", "uint32", "address", "bool"});
        User.UserBuilder user = User.builder();
        Object[] objects = function.decodeResult(constantResult.toByteArray());
        for (Object object : objects) {
            if (object instanceof BigInteger) {
                BigInteger id = (BigInteger) object;
                user.roleId(id.intValue());
            } else if (object instanceof byte[]) {
                String stringData = DecodeUtil.createReadableString((byte[]) object);
                if (((byte[]) object).length == 32) {
                    user.userId(stringData);
                } else if(((byte[]) object).length == 20){
                    user.address(stringData);
                }
            } else if (object instanceof Boolean) {
                boolean active = (Boolean) object;
                user.active(active);
            }
        }
        return user.build();
    }

    public boolean addNode(cn.ledgeryi.sdk.serverapi.data.permission.Node newNode, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        //todo need update param
        callParam.setCaller(caller.getAddress());
        callParam.addArgs(newNode.getOwner());
        callParam.addArgs(newNode.getHost());
        callParam.addArgs(String.valueOf(newNode.getPort()));
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.createProposalWithCreateNode(builder.build());
        return processTransaction(tx, caller);
    }

    public boolean removeNode(cn.ledgeryi.sdk.serverapi.data.permission.Node newNode, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.addArgs(newNode.getNodeId());
        callParam.addArgs(newNode.getOwner());
        callParam.addArgs(newNode.getHost());
        callParam.addArgs(newNode.getPort()+"");
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.createProposalWithRemoveNode(builder.build());
        return processTransaction(tx, caller);
    }

    public cn.ledgeryi.sdk.serverapi.data.permission.Node queryNodeInfo(int nodeIndex, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.addArgs(String.valueOf(nodeIndex));
        builder.setParam(Any.pack(callParam.build()));
        TransactionExtention tx = permissionBlockingStub.queryNodeInfo(builder.build());
        ByteString constantResult = tx.getConstantResult(0);
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getNode",
                new String[]{"uint32"}, new String[]{"bytes32", "address", "string", "uint32"});
        cn.ledgeryi.sdk.serverapi.data.permission.Node.NodeBuilder nodeBuilder =
                cn.ledgeryi.sdk.serverapi.data.permission.Node.builder();
        Object[] objects = function.decodeResult(constantResult.toByteArray());
        for (Object object : objects) {
            if (object instanceof BigInteger) {
                BigInteger id = (BigInteger) object;
                nodeBuilder.port(id.intValue());
            } else if (object instanceof byte[]) {
                String stringData = DecodeUtil.createReadableString((byte[]) object);
                if (((byte[]) object).length == 32) {
                    nodeBuilder.nodeId(stringData);
                } else if(((byte[]) object).length == 20){
                    nodeBuilder.owner(stringData);
                }
            } else if (object instanceof String) {
                String host = (String) object;
                nodeBuilder.host(host);
            }
        }
        return nodeBuilder.build();
    }

    public DeployContractReturn deployContract(DeployContractParam param, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        CreateSmartContract createContract;
        try {
            createContract = createContract(DecodeUtil.decode(caller.getAddress()), param);
        } catch (CreateContractExecption e) {
            log.error(e.getMessage());
            return null;
        }
        builder.setParam(Any.pack(createContract));
        TransactionExtention tx = permissionBlockingStub.deployContract(builder.build());
        boolean result = processTransaction(tx, caller);
        if (result) {
            String contractAddress = DecodeUtil.createReadableString(
                    TransactionUtils.generateContractAddress(tx.getTransaction(),
                            DecodeUtil.decode(caller.getAddress())));
            return DeployContractReturn.builder()
                    .transactionId(DecodeUtil.createReadableString(tx.getTxid()))
                    .contractName(createContract.getNewContract().getName())
                    .contractByteCodes(param.getContractByteCodes())
                    .ownerAddress(caller.getAddress())
                    .contractAddress(contractAddress)
                    .contractAbi(createContract.getNewContract().getAbi().toString())
                    .build();
        } else {
            log.error("deploy contract failed.");
            return null;
        }
    }

    public SmartContract getContract(String contractAddress, AccountYi caller) {
        GrpcRequest.Builder requestBuilder = GrpcRequest.newBuilder();
        ContractCallParam.Builder builder = ContractCallParam.newBuilder();
        builder.setCaller(caller.getAddress());
        builder.addArgs(contractAddress);
        requestBuilder.setParam(Any.pack(builder.build()));
        return permissionBlockingStub.getContract(requestBuilder.build());
    }

    private CreateSmartContract createContract(byte[] ownerAddress, DeployContractParam contractParam)
            throws CreateContractExecption {
        String contractAbi = contractParam.getAbi();
        if (StringUtils.isEmpty(contractAbi)) {
            log.error("deploy contract, abi is null");
            throw new CreateContractExecption("deploy contract, abi is null");
        }
        SmartContract.ABI abi = JsonFormatUtil.jsonStr2ABI(contractAbi);
        SmartContract.Builder builder = SmartContract.newBuilder();
        builder.setAbi(abi);
        builder.setName(contractParam.getContractName());
        //set contract owner
        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        byte[] byteCode = Hex.decode(contractParam.getContractByteCodes());
        builder.setBytecode(ByteString.copyFrom(byteCode));
        CreateSmartContract.Builder createSmartContractBuilder = CreateSmartContract.newBuilder();
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
        callParam.addArgs(userId);
        callParam.addArgs(String.valueOf(roleType.getType()));
        callParam.addArgs(user);
        builder.setParam(Any.pack(callParam.build()));
        return builder.build();
    }

    private GrpcRequest createGrpcRequest(RoleTypeEnum roleType, AccountYi caller){
        GrpcRequest.Builder builder = GrpcRequest.newBuilder();
        ContractCallParam.Builder callParam = ContractCallParam.newBuilder();
        callParam.setCaller(caller.getAddress());
        callParam.addArgs(String.valueOf(roleType.getType()));
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
