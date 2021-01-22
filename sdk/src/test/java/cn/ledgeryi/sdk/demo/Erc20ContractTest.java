package cn.ledgeryi.sdk.demo;

import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractParam;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractReturn;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.Strings;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Erc20ContractTest {

    private static String privateKey = "e8b5177d5a69898dcc437d0e96a9343a37bac001cb9bec7a90b660eee66b4587";
    private static String ownerAddress = "ada95a8734256b797efcd862e0b208529283ac56";

    private LedgerYiApiService ledgerYiApiService;

    @Before
    public void start(){
        ledgerYiApiService = new LedgerYiApiService();
    }

    private static String basicft = "// SPDX-License-Identifier: GPL-3.0\n" +
            "\n" +
            "pragma solidity ^0.6.8;\n" +
            "\n" +
            "contract BasicFT {\n" +
            "\n" +
            "    string public constant name = \"ERC20Basic\";\n" +
            "    string public constant symbol = \"BSC\";\n" +
            "    uint8 public constant decimals = 18;  \n" +
            "    uint256 totalSupply_ = 1000000000;\n" +
            "\n" +
            "    event Transfer(address indexed from, address indexed to, uint tokens); \n" +
            "    event Burn(address indexed account, uint256 amount);\n" +
            "    event Mint(address indexed account, uint256 amount);\n" +
            "\n" +
            "    mapping(address => uint256) balances;\n" +
            "\n" +
            "    constructor() public {\n" +
            "     balances[msg.sender] = totalSupply_;\n" +
            "    }\n" +
            "\n" +
            "    function totalSupply() public view returns (uint256) {\n" +
            "     return totalSupply_;\n" +
            "    }\n" +
            "    \n" +
            "    function balanceOf(address tokenOwner) public view returns (uint) {\n" +
            "        return balances[tokenOwner];\n" +
            "    }\n" +
            "\n" +
            "    function burn(address account, uint256 amount) public {\n" +
            "        require(amount <= balances[account]);\n" +
            "        balances[account] = sub(balances[account], amount);\n" +
            "        totalSupply_ = sub(totalSupply_, amount);\n" +
            "        emit Burn(account, amount);\n" +
            "    }\n" +
            "\n" +
            "    function mint(address account, uint256 amount) public {\n" +
            "        balances[account] = add(balances[account], amount);\n" +
            "        totalSupply_ = add(totalSupply_, amount);\n" +
            "        emit Mint(account, amount);\n" +
            "    }\n" +
            "\n" +
            "    function transfer(address receiver, uint numTokens) public returns (bool) {\n" +
            "        require(numTokens <= balances[msg.sender]);\n" +
            "        balances[msg.sender] = sub(balances[msg.sender] , numTokens);\n" +
            "        balances[receiver] = add(balances[receiver], numTokens);\n" +
            "        emit Transfer(msg.sender, receiver, numTokens);\n" +
            "        return true;\n" +
            "    }\n" +
            "\n" +
            "    function sub(uint256 a, uint256 b) internal pure returns (uint256) {\n" +
            "      assert(b <= a);\n" +
            "      return a - b;\n" +
            "    }\n" +
            "    \n" +
            "    function add(uint256 a, uint256 b) internal pure returns (uint256) {\n" +
            "      uint256 c = a + b;\n" +
            "      assert(c >= a);\n" +
            "      return c;\n" +
            "    }\n" +
            "\n" +
            "}";


    private static String ownedToken = "// SPDX-License-Identifier: GPL-3.0\n" +
            "pragma solidity ^0.6.9;\n" +
            "contract OwnedToken {\n" +
            "    address owner;\n" +
            "    bytes32 name;\n" +
            "    constructor(bytes32 _name) public {\n" +
            "        owner = msg.sender;\n" +
            "        name = _name;\n" +
            "    }\n" +
            "    function changeName(bytes32 newName) public {\n" +
            "        if (msg.sender == owner)\n" +
            "            name = newName;\n" +
            "    }\n" +
            "    function getName() public view returns (bytes32) {\n" +
            "        return name;\n" +
            "    }\n" +
            "}";


    @Test
    public void compileContractTest() {
        DeployContractParam result = null;
        try {
            String contract = ownedToken;
            result = ledgerYiApiService.compileSingleContract(contract);
            result.setConstructor("constructor(bytes32)");
            ArrayList<Object> args = Lists.newArrayList();
            String readableString = Hex.toHexString(("Lili").getBytes());
            args.add(readableString);
            result.setArgs(args);
        } catch (ContractException e) {
            e.printStackTrace();
            System.out.println("contract compile error: " + e.getMessage());
        }
        System.out.println("name: " + result.getContractName());
        System.out.println("abi: " + result.getAbi());
        System.out.println("code: " + result.getContractByteCodes());
    }

    @Test
    public void compileAndDeployContract(){
        DeployContractParam result = null;
        DeployContractReturn deployContract = null;
        try {
            String contract = ownedToken;
            result = ledgerYiApiService.compileSingleContract(contract);
            result.setConstructor("constructor(bytes32)");
            ArrayList<Object> args = Lists.newArrayList();
            String readableString = Hex.toHexString(("Lili").getBytes());
            args.add(readableString);
            result.setArgs(args);
            deployContract = ledgerYiApiService.deployContract(DecodeUtil.decode(ownerAddress), DecodeUtil.decode(privateKey), result);
        } catch (ContractException | CreateContractExecption e) {
            e.printStackTrace();
            System.out.println("contract compile error: " + e.getMessage());
        }
        System.out.println("name: " + result.getContractName());
        System.out.println("abi: " + result.getAbi());
        System.out.println("code: " + result.getContractByteCodes());
        System.out.println("contract address: " + deployContract.getContractAddress());
    }

    // BasicFT address
    //private static String contractAddres = "71355a60f1b22a530e94300ad7809491486eead8";
    //ownedToken
    private static String contractAddres = "e8d4cfced6431d457f5d81fff45965c2956a750a";

    @Test
    public void triggerOwnedTokenContract(){
        getName();
        changeName();
    }

    private void getName(){
        List args = Collections.EMPTY_LIST;
        String method = "getName()";
        triggerContract(method, args,true);
    }

    private void changeName(){
        ArrayList<Object> args = Lists.newArrayList();
        String toHexString = Hex.toHexString(("bob").getBytes());
        args.add(toHexString);
        String method = "changeName(bytes32)";
        triggerContract(method, args,false);
    }

    @Test
    public void getContractFromOnChain(){
        SmartContractOuterClass.SmartContract contract = ledgerYiApiService.getContract(DecodeUtil.decode(contractAddres));
        System.out.println(JsonFormatUtil.printSmartContract(contract));
        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(contract.getAbi()));
        String abi = jsonObject.getString("entrys");
        System.out.println(abi);
    }

    @Test
    public void mint() {
        List args = Arrays.asList(contractAddres,"5");
        String method = "mint(address,uint256)";
        triggerContract(method, args,false);
    }

    @Test
    public void balanceOf() {
        List args = Arrays.asList(contractAddres);
        String method = "balanceOf(address)";
        triggerContract(method, args, true);
    }

    @Test
    public void burn() {
        List args = Arrays.asList(contractAddres,"1");
        String method = "burn(address,uint256)";
        triggerContract(method, args, false);
    }

    @Test
    public void totalSupply() {
        List args = Collections.EMPTY_LIST;
        String method = "totalSupply()";
        triggerContract(method, args, true);
    }

    @Test
    public void transfer() {
        String receiver = contractAddres;
        List args = Arrays.asList(receiver,"4");
        String method = "transfer(address,uint256)";
        triggerContract(method, args, false);
    }

    @Test
    public void clearContractAbi(){
        boolean result = ledgerYiApiService.clearContractABI(DecodeUtil.decode(ownerAddress),
                DecodeUtil.decode(privateKey), DecodeUtil.decode(contractAddres));
        System.out.println("clear result: " +  result);
    }

    private void triggerContract(String method, List<Object> args, boolean isConstant) {
        TriggerContractParam triggerContractParam = new TriggerContractParam()
                .setContractAddress(DecodeUtil.decode(contractAddres))
                .setCallValue(0)
                .setConstant(isConstant)
                .setArgs(args)
                .setTriggerMethod(method);

        TriggerContractReturn result = ledgerYiApiService.triggerContract(DecodeUtil.decode(ownerAddress),
                DecodeUtil.decode(privateKey), triggerContractParam);

        String cmdMethodStr = isConstant ? "TriggerConstantContract" : "TriggerContract";
        if (!isConstant) {
            if (result != null) {
                System.out.println("Broadcast the " + cmdMethodStr + " successful.");
            } else {
                System.out.println("Broadcast the " + cmdMethodStr + " failed");
            }
        } else {
            System.out.println("trigger contract result: " + ByteUtil.bytesToBigInteger(result.getCallResult().toByteArray()));
            System.out.println("trigger contract result: " + Strings.fromByteArray(result.getCallResult().toByteArray()));
        }
    }
}
