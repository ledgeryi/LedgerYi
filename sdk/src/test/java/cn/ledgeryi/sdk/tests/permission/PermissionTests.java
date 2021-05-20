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
import cn.ledgeryi.sdk.tests.AbstractContractTest;
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
public class PermissionTests extends AbstractContractTest {

    private PermissionGrpcClient permissionClient;
    private AccountYi caller;
    private String ownerAddress = "c179d10bfb49ea9ae525c8b1113a914073b394cf";
    private String privateKey = "c48ced226af4e7b9eebdba8acea64f67736e44ec51bada54ca5b41ec18aac448";
    private String storageManagerAddress = "1316660d178c70c3bf44f625f85be0a16a73f08e";

    @Override
    protected String getPrivateKey() {
        return privateKey;
    }

    @Override
    protected String getOwnerAddress() {
        return ownerAddress;
    }

    @Override
    protected String getDeployedOwnerAddress() {
        return null;
    }

    @Before
    public void start() {
        this.permissionClient = PermissionGrpcClient.initPermissionGrpcClient();
        this.caller = new AccountYi(ownerAddress, null, privateKey, null);
        super.init();
    }

    @Test
    public void test_deployStorageContract() {
        Path source = Paths.get("src","test","resources","permission","AdminStorage.sol");
//        waitFiveSecondToCompileAndDeployPermissionContract(source, "AdminStorage");//70f0509945d13e5eed0f741590da0e3e41e98414

//        source = Paths.get("src","test","resources","permission","NodeStorage.sol");
//        waitFiveSecondToCompileAndDeployPermissionContract(source, "NodeStorage");//816b1b84cded5437ad2097c0c1089290d8ca820f

//        source = Paths.get("src","test","resources","permission","PermissionStorage.sol");
//        waitFiveSecondToCompileAndDeployPermissionContract(source, "PermissionStorage");//f22afb9b9c578f8e5e9c3da6a6d50ea9f689a6b5

        source = Paths.get("src","test","resources","permission","ProposalStorage.sol");
        waitFiveSecondToCompileAndDeployPermissionContract(source, "ProposalStorage");//bd44f769fc8b86f88170061e5752ed9e162b7a18
    }

    @Test
    public void test_deployManagerContract() {
        Path source = Paths.get("src","test","resources","permission","AdminManager.sol");
        String constructor = "constructor(address)";
        ArrayList<Object> args = Lists.newArrayList("70f0509945d13e5eed0f741590da0e3e41e98414");

//        waitFiveSecondToCompileAndDeployPermissionContract(source, "AdminManager", constructor, args);//5b8325e9f86caf6a7644c332cb8c5411dff76679

//        args = Lists.newArrayList("0054606666fb862930a9e307efabb4c7a8b39f20");
//        source = Paths.get("src","test","resources","permission","NodeManager.sol");
//        waitFiveSecondToCompileAndDeployPermissionContract(source, "NodeManager", constructor, args);//54a63e9c4b78898d56dd7264bc9d492cf47ae3a9

//        args = Lists.newArrayList("03173b77883fd77bc396f2063bfa0e375bc60ba0");
//        source = Paths.get("src","test","resources","permission","PermissionManager.sol");
//        waitFiveSecondToCompileAndDeployPermissionContract(source, "PermissionManager", constructor, args);//49117130cb2a685a636a05a75e2a13b92e526ede

        constructor = "constructor(address,address,address,address)";
        args = Lists.newArrayList("bd44f769fc8b86f88170061e5752ed9e162b7a18",
                "49117130cb2a685a636a05a75e2a13b92e526ede",
                "5b8325e9f86caf6a7644c332cb8c5411dff76679",
                "54a63e9c4b78898d56dd7264bc9d492cf47ae3a9");
        source = Paths.get("src","test","resources","permission","ProposalManager.sol");
        waitFiveSecondToCompileAndDeployPermissionContract(source, "ProposalManager", constructor, args);//a528779e818526d5b4c1fd73d4979ead0264712d
    }

    @Test
    public void test_init() {
        permissionClient.init(caller, "a528779e818526d5b4c1fd73d4979ead0264712d");
    }

    @Test
    public void test_addDefaultAdmin() {
        permissionClient.addDefaultAdmin(caller, caller.getAddress());
    }

    @Test
    public void test_addNode() {
        boolean result = permissionClient.addNewNode(caller,"ada95a8734256b797efcd862e0b208529283ac56", "127.0.0.1", 6668);
        System.out.println(result);
    }
    @Test
    public void test_findNode() {
        System.out.println(permissionClient.queryNodeInfo(caller, "ada95a8734256b797efcd862e0b208529283ac56"));
    }
    @Test
    public void deployPermissionStorageManagerContract(){//5ada8657660e9c2c31553691ca6c128dca7ccb6a
        Path source = Paths.get("src","test","resources","permission","PermissionStorage.sol");
        waitFiveSecondToCompileAndDeployPermissionContract(source, "PermissionStorageManager");
    }

    @Test
    public void deployPermissionManagerContract(){//973181cf5bbb221abae919cbac13abd3024de300
        ArrayList<Object> args = Lists.newArrayList("5ada8657660e9c2c31553691ca6c128dca7ccb6a");
        Path source = Paths.get("src","test","resources","permission","PermissionManager.sol");
        waitFiveSecondToCompileAndDeployPermissionContract(source, "PermissionManager", "constructor(address)", args);
    }

    @Test
    public void addRole() {
//        boolean addRole = permissionClient.init(RoleTypeEnum.CONTRACT_DEPLOY, caller);
//        System.out.println(addRole);
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
    public void addNode() {
        Node node = Node.builder().host("127.0.0.1").port(6668).owner("ada95a8734256b797efcd862e0b208529283ac56").build();
        boolean addNode = permissionClient.addNode(node, caller);
        System.out.println(addNode);
    }

    @Test
    public void updateNode() {
        String owner = "e506af507e6cb873036826db5c123e0f362fe8a1";
        String nodeId = "b10e2d527612073b26eecdfd717e6a320cf44b4afac2b0732d9fcbe2b7fa0cf6";
        Node node = Node.builder().nodeId(nodeId).host("127.0.0.2").port(50053).owner(owner).build();
        boolean updateNode = permissionClient.removeNode(node, caller);
        System.out.println(updateNode);
    }


    private int nodeNum = 2;

    @Test
    public void getNode() {
        Node node = permissionClient.queryNodeInfo(caller, ownerAddress);
        System.out.println(node.toString());
    }

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
            Path source = Paths.get("src","test","resources","permission","NodeManager.sol.sol");
            param = ContactCompileUtil.compileContractFromFile(source,"NodeManager.sol");
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
        //27899c4253c13c09659dd63fa84dbd05ab9a8a72
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
        String contractAddress = "1316660d178c70c3bf44f625f85be0a16a73f08e";
        SmartContract contract = permissionClient.getContract(contractAddress, caller);
        System.out.println(JsonFormatUtil.printSmartContract(contract));
        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(contract.getAbi()));
        String abi = jsonObject.getString("entrys");
        System.out.println(abi);
    }
}
