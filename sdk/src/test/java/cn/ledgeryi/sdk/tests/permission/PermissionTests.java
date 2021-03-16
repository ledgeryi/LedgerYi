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

        //String privateKey = "ec19148056c4cfc5fc1b1923b8bb657e1e481a8f092415d5af96dd60f3e6806d";
        //String ownerAddress = "42979c83d087b612fdc82c560b3131b9c7f34a76";
        this.caller = new AccountYi(ownerAddress, null, privateKey, null);
    }

    @Test
    public void addRole() {
        boolean addRole = permissionClient.addRole(RoleTypeEnum.READ_ONLY, caller);
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

    private int roleNum = 4;

    @Test
    public void getRole() {
        for (int roleId = 0; roleId < roleNum; roleId++) {
            Role role = permissionClient.getRole(roleId, caller);
            System.out.println(role.toString());
        }
    }

    @Test
    public void revokeRoleOfUser() {
        String userId = "";
        String user = "ada95a8734256b797efcd862e0b208529283ac56";
        permissionClient.revokeRoleOfUser(userId, RoleTypeEnum.READ_ONLY, user, caller);
    }

    @Test
    public void hasRoleOfUser() {
        String userId = "b10e2d527612073b26eecdfd717e6a320cf44b4afac2b0732d9fcbe2b7fa0cf6";
        String user = "ada95a8734256b797efcd862e0b208529283ac54";
        boolean hasRoleOfUser = permissionClient.hasRoleOfUser(userId, RoleTypeEnum.READ_ONLY, user, caller);
        System.out.println(hasRoleOfUser);
    }

    @Test
    public void assignRoleForUser() {
        String user = "ada95a8734256b797efcd862e0b208529283ac54";
        boolean assignRoleForUser = permissionClient.assignRoleForUser(RoleTypeEnum.READ_ONLY, user, caller);
        System.out.println(assignRoleForUser);
    }

    @Test
    public void getUserNum() {
        userNum = permissionClient.getUserNum(caller);
        System.out.println(userNum);
    }

    private int userNum = 2;

    @Test
    public void getUser() {
        for (int userIndex = 0; userIndex < userNum; userIndex++) {
            User user = permissionClient.getUser(userIndex, caller);
            System.out.println(user.toString());
        }
    }

    /**
     * 角色BLOCK_PRODUCE：
     *
     * address:    ada95a8734256b797efcd862e0b208529283ac56
     * privateKey: e8b5177d5a69898dcc437d0e96a9343a37bac001cb9bec7a90b660eee66b4587
     *
     * address:    e506af507e6cb873036826db5c123e0f362fe8a1
     * privateKey: dbcef69812d842ffa2e0575e39b61543771fcb59bae9ebe155f043bf149f27b9
     *
     * address:    9226b046377d70896bb3468864258feffdc956a1
     * privateKey: a8eb4340bf444b378494253d16ab7536a2dfc81d44dadeeb259be74fd131482a
     */
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

    private String storageManagerAddress = "192e6a87f217411a124b331b1309280dabd29362";

    @Test
    public void deployStorageManagerContract(){
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
        //53e69a964eb57e45c2d1892ef3f6a87378817539
    }

    @Test
    public void getContract(){
        String contractAddress = "53e69a964eb57e45c2d1892ef3f6a87378817539";
        SmartContract contract = permissionClient.getContract(contractAddress, caller);
        System.out.println(JsonFormatUtil.printSmartContract(contract));
        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(contract.getAbi()));
        String abi = jsonObject.getString("entrys");
        System.out.println(abi);
    }
}
