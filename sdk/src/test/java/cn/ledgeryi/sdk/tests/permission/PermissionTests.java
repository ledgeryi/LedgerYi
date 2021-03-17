package cn.ledgeryi.sdk.tests.permission;

import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract;
import cn.ledgeryi.sdk.common.AccountYi;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.contract.ContactCompileUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.serverapi.PermissionGrpcClient;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.permission.Node;
import cn.ledgeryi.sdk.serverapi.data.permission.Role;
import cn.ledgeryi.sdk.serverapi.data.permission.RoleTypeEnum;
import cn.ledgeryi.sdk.serverapi.data.permission.User;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * @author Brian
 * @date 2021/3/15 14:48
 */
public class PermissionTests {

    private PermissionGrpcClient permissionClient;
    private AccountYi caller;

    @Before
    public void start() {
        this.permissionClient = PermissionGrpcClient.initPermissionGrpcClient();
        String ownerAddress = "99b8466efe9f05cee87d4e167cdfaec0432d90fc";
        String privateKey = "dfbf32c6cd4cbbb69d4a6d8c547636eaa4ba9fe28db3dec1272f03755111f7d7";
        this.caller = new AccountYi(ownerAddress, null, privateKey, null);
    }

    @Test
    public void addRole() {
        boolean addRole = permissionClient.addRole(RoleTypeEnum.CONTRACT_DEPLOY, caller);
        System.out.println(addRole);
    }

    @Test
    public void revokeRole() {
        boolean revokeRole = permissionClient.revokeRole(RoleTypeEnum.READ_ONLY, caller);
        System.out.println(revokeRole);
    }

    @Test
    public void getRoleNum() {
        roleNum = permissionClient.getRoleNum(caller);
        System.out.println(roleNum);
    }

    private int roleNum = 2;

    @Test
    public void getRole() {
        for (int roleId = 0; roleId < roleNum; roleId++) {
            Role role = permissionClient.getRole(roleId, caller);
            System.out.println(role.toString());
        }
    }

    @Test
    public void revokeRoleOfUser() {
        String userId = "290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e563";
        String user = "ada95a8734256b797efcd862e0b208529283ac50";
        boolean revokeUser = permissionClient.revokeRoleOfUser(userId, RoleTypeEnum.CONTRACT_DEPLOY, user, caller);
        System.out.println(revokeUser);
    }

    @Test
    public void hasRoleOfUser() {
        String userId = "290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e563";
        String user = "ada95a8734256b797efcd862e0b208529283ac50";
        boolean hasRoleOfUser = permissionClient.hasRoleOfUser(userId, RoleTypeEnum.CONTRACT_DEPLOY, user, caller);
        System.out.println(hasRoleOfUser);
    }

    @Test
    public void assignRoleForUser() {
        String user = "ada95a8734256b797efcd862e0b208529283ac50";
        boolean assignRoleForUser = permissionClient.assignRoleForUser(RoleTypeEnum.CONTRACT_DEPLOY, user, caller);
        System.out.println(assignRoleForUser);
    }

    @Test
    public void getUserNum() {
        userNum = permissionClient.getUserNum(caller);
        System.out.println(userNum);
    }

    private int userNum = 1;

    @Test
    public void getUser() {
        for (int userIndex = 0; userIndex < userNum; userIndex++) {
            User user = permissionClient.getUser(userIndex, caller);
            System.out.println(user.toString());
        }
    }

    @Test
    public void addNode() {
        String owner = "e506af507e6cb873036826db5c123e0f362fe8a1";
        Node node = Node.builder().host("127.0.0.1").port(50054).owner(owner).build();
        boolean addNode = permissionClient.addNode(node, caller);
        System.out.println(addNode);
    }

    @Test
    public void updateNode() {
        String owner = "e506af507e6cb873036826db5c123e0f362fe8a1";
        String nodeId = "b10e2d527612073b26eecdfd717e6a320cf44b4afac2b0732d9fcbe2b7fa0cf6";
        Node node = Node.builder().nodeId(nodeId).host("127.0.0.2").port(50053).owner(owner).build();
        boolean updateNode = permissionClient.updateNode(node, caller);
        System.out.println(updateNode);
    }

    @Test
    public void deleteNode() {
        String nodeId = "290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e563";
        boolean deleteNode = permissionClient.deleteNode(nodeId, caller);
        System.out.println(deleteNode);
    }

    @Test
    public void getNodeNum() {
        nodeNum = permissionClient.getNodeNum(caller);
        System.out.println(nodeNum);
    }

    private int nodeNum = 2;

    @Test
    public void getNode() {
        for (int nodeIndex = 0; nodeIndex < nodeNum; nodeIndex++) {
            Node node = permissionClient.getNode(nodeIndex, caller);
            System.out.println(node.toString());
        }
    }

    private String storageManagerAddress = "5e5943d8e74a68ef7768f7a4d403f253d2f4b3f9";

    @Test
    public void deployStorageManagerContract(){//361abef90199245a6847768e5915d612ce43acf0
        DeployContractParam param = null;
        DeployContractReturn deployContract = null;
        try {
            Path source = Paths.get("src","test","resources","permission","StorageManager.sol");
            param = ContactCompileUtil.compileContractFromFile(source,"StorageManager");
            deployContract = permissionClient.deployContract(param, caller);
        } catch (ContractException e) {
            System.out.println("contract compile error: " + e.getMessage());
        }
        System.out.println("name: " + param.getContractName());
        System.out.println("abi: " + param.getAbi());
        System.out.println("code: " + param.getContractByteCodes());
        System.out.println("contract address: " + deployContract.getContractAddress());
    }

    @Test
    public void deployNodeManagerContract(){
        DeployContractParam param = null;
        DeployContractReturn deployContract = null;
        try {
            Path source = Paths.get("src","test","resources","permission","NodeManager.sol");
            param = ContactCompileUtil.compileContractFromFile(source,"NodeManager");
            param.setConstructor("constructor(address)");
            ArrayList<Object> args = Lists.newArrayList();
            args.add(storageManagerAddress);
            param.setArgs(args);
            deployContract = permissionClient.deployContract(param, caller);
        } catch (ContractException e) {
            System.out.println("contract compile error: " + e.getMessage());
        }
        System.out.println("name: " + param.getContractName());
        System.out.println("abi: " + param.getAbi());
        System.out.println("code: " + param.getContractByteCodes());
        System.out.println("contract address: " + deployContract.getContractAddress());
        //ad15d34a0473a5ae18aa5877207c316bd7225ea1
    }

    @Test
    public void deployRoleManagerContract(){
        DeployContractParam param = null;
        DeployContractReturn deployContract = null;
        try {
            Path source = Paths.get("src","test","resources","permission","RoleManager.sol");
            param = ContactCompileUtil.compileContractFromFile(source,"RoleManager");
            param.setConstructor("constructor(address)");
            ArrayList<Object> args = Lists.newArrayList();
            args.add(storageManagerAddress);
            param.setArgs(args);
            deployContract = permissionClient.deployContract(param, caller);
        } catch (ContractException e) {
            System.out.println("contract compile error: " + e.getMessage());
        }
        System.out.println("name: " + param.getContractName());
        System.out.println("abi: " + param.getAbi());
        System.out.println("code: " + param.getContractByteCodes());
        System.out.println("contract address: " + deployContract.getContractAddress());
        //a9be8b8c4e12557e96f29369df2dede1a324eaf8
    }

    @Test
    public void getContract(){
        String contractAddress = "a9be8b8c4e12557e96f29369df2dede1a324eaf8";
        SmartContract contract = permissionClient.getContract(contractAddress, caller);
        System.out.println(JsonFormatUtil.printSmartContract(contract));
        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(contract.getAbi()));
        String abi = jsonObject.getString("entrys");
        System.out.println(abi);
    }
}
