package cn.ledgeryi.sdk.tests.contract;

import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.parse.event.DataWord;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractParam;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractReturn;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
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

    private static String basicft = "" +
            "// SPDX-License-Identifier: GPL-3.0\n" +
            "pragma solidity ^0.6.8;\n" +
            "contract BasicFT {\n" +
            "    string public constant name = \"ERC20Basic\";\n" +
            "    string public constant symbol = \"BSC\";\n" +
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

    private static String erc20 =
            "// SPDX-License-Identifier: GPL-3.0\n" +
                    "pragma solidity ^0.6.9;\n" +
                    "contract BasicFT {\n" +
                    "\n" +
                    "    string public name;\n" +
                    "    string public symbol;  \n" +
                    "    uint256 public totalSupply;\n" +
                    "\n" +
                    "    event Transfer(address indexed from, address indexed to, uint tokens);\n" +
                    "    event Burn(address indexed account, uint256 amount);\n" +
                    "    event Mint(address indexed account, uint256 amount);\n" +
                    "\n" +
                    "    mapping(address => uint256) balances;\n" +
                    "\n" +
                    "    constructor(string memory name_, string memory symbol_, uint256 totalSupply_) public {\n" +
                    "        balances[msg.sender] = totalSupply_;\n" +
                    "        name = name_;\n" +
                    "        symbol = symbol_;\n" +
                    "        totalSupply = totalSupply_;\n" +
                    "    }\n" +
                    "    \n" +
                    "    function balanceOf(address tokenOwner) public view returns (uint) {\n" +
                    "        return balances[tokenOwner];\n" +
                    "    }\n" +
                    "\n" +
                    "    function burn(address account, uint256 amount) public {\n" +
                    "        require(amount <= balances[account]);\n" +
                    "        balances[account] = sub(balances[account], amount);\n" +
                    "        totalSupply = sub(totalSupply, amount);\n" +
                    "        emit Burn(account, amount);\n" +
                    "    }\n" +
                    "\n" +
                    "    function mint(address account, uint256 amount) public {\n" +
                    "        balances[account] = add(balances[account], amount);\n" +
                    "        totalSupply = add(totalSupply, amount);\n" +
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

    @Test
    public void compileContractTest() {
        DeployContractParam result = null;
        try {
            String contract = basicft;
            result = ledgerYiApiService.compileSingleContract(contract);
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
            String contract = erc20;
            result = ledgerYiApiService.compileSingleContract(contract);
            result.setConstructor("constructor(string,string,uint256)");
            ArrayList<Object> args = Lists.newArrayList();
            args.add("ERC20Basic");
            args.add("BSC");
            args.add(1000000000);
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
    private static String contractAddres = "c0ebf5c09c46bd2467c63e7bd1f7c8f98a8b074f";

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
        for (int i = 0; i < 1; i++) {
            List args = Arrays.asList(contractAddres,"1");
            String method = "burn(address,uint256)";
            triggerContract(method, args, false);
        }
    }

    @Test
    public void totalSupply() {
        List args = Collections.EMPTY_LIST;
        String method = "totalSupply()";
        triggerContract(method, args, true);
    }

    @Test
    public void name() {
        List args = Collections.EMPTY_LIST;
        String method = "name()";
        triggerContract(method, args, true);
    }

    @Test
    public void symbol() {
        List args = Collections.EMPTY_LIST;
        String method = "symbol()";
        triggerContract(method, args, true);
    }

    @Test
    public void transfer() {
        String receiver = contractAddres;
        List args = Arrays.asList(receiver,"40");
        String method = "transfer(address,uint256)";
        triggerContract(method, args, false);
    }

    @Test
    public void bachTransfer() {
        for (int i = 0; i < 10000; i++) {
            String receiver = contractAddres;
            List args = Arrays.asList(receiver,"1");
            String method = "transfer(address,uint256)";
            triggerContract(method, args, false);
        }
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
            System.out.println("trigger contract result: " + DecodeUtil.createReadableString(result.getCallResult().toByteArray()));
            System.out.println("trigger contract result: " + Strings.fromByteArray(result.getCallResult().toByteArray()).trim());
        }
    }
}
