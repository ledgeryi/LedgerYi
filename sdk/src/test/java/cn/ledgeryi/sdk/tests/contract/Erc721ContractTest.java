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
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.Strings;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Erc721ContractTest {

    private static String privateKey = "e8b5177d5a69898dcc437d0e96a9343a37bac001cb9bec7a90b660eee66b4587";
    private static String ownerAddress = "ada95a8734256b797efcd862e0b208529283ac56";
    private LedgerYiApiService ledgerYiApiService;

    @Before
    public void start(){
        ledgerYiApiService = new LedgerYiApiService();
    }

    private static final String erc721 = "// SPDX-License-Identifier: GPL-3.0\n" +
            "pragma solidity ^0.6.8;\n" +
            "\n" +
            "contract ERC721Enumerable {\n" +
            "\n" +
            "    string internal nftName;\n" +
            "    string internal nftSymbol;\n" +
            "    address internal creator;\n" +
            "\n" +
            "    mapping(uint256 => bool) internal burned;\n" +
            "    mapping(uint256 => address) internal owners;\n" +
            "    mapping(address => uint256) internal balances;\n" +
            "\n" +
            "    uint256 internal maxId;\n" +
            "    uint[] internal tokenIndexes;\n" +
            "    mapping(uint => uint) internal indexTokens;\n" +
            "    mapping(uint => uint) internal tokenTokenIndexes;\n" +
            "    mapping(address => uint[]) internal ownerTokenIndexes;\n" +
            "\n" +
            "    constructor(string memory name_, string memory symbol_, uint _initialSupply) public {\n" +
            "        creator = msg.sender;\n" +
            "        nftName = name_;\n" +
            "        nftSymbol = symbol_;\n" +
            "        maxId = _initialSupply;\n" +
            "        balances[msg.sender] = _initialSupply;\n" +
            "        for(uint i = 0; i < _initialSupply; i++){\n" +
            "            tokenTokenIndexes[i+1] = i;\n" +
            "            ownerTokenIndexes[creator].push(i+1);\n" +
            "            tokenIndexes.push(i+1);\n" +
            "            indexTokens[i+1] = i;\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    function isValidToken(uint256 _tokenId) public view returns (bool){\n" +
            "        return _tokenId != 0 && _tokenId <= maxId && !burned[_tokenId];\n" +
            "    }\n" +
            "\n" +
            "    function balanceOf(address _owner) public view returns (uint256){\n" +
            "        return balances[_owner];\n" +
            "    }\n" +
            "\n" +
            "    function ownerOf(uint256 _tokenId) public view returns (address){\n" +
            "        require(isValidToken(_tokenId));\n" +
            "        if(owners[_tokenId] != address(0) ){\n" +
            "            return owners[_tokenId];\n" +
            "        }else{\n" +
            "            return creator;\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    function totalSupply() public view returns (uint256){\n" +
            "        return tokenIndexes.length;\n" +
            "    }\n" +
            "\n" +
            "    function tokenByIndex(uint256 _index) public view returns(uint256){\n" +
            "        require(_index < tokenIndexes.length);\n" +
            "        return tokenIndexes[_index];\n" +
            "    }\n" +
            "\n" +
            "    function tokenOfOwnerByIndex(address _owner, uint256 _index) public view returns (uint256){\n" +
            "        require(_index < balances[_owner]);\n" +
            "        return ownerTokenIndexes[_owner][_index];\n" +
            "    }\n" +
            "\n" +
            "    function transferFrom(address _from, address _to, uint256 _tokenId) public {\n" +
            "        address owner = ownerOf(_tokenId);\n" +
            "        require (owner == msg.sender);\n" +
            "        require(owner == _from);\n" +
            "        require(_to != address(0));\n" +
            "        require(isValidToken(_tokenId));\n" +
            "        owners[_tokenId] = _to;\n" +
            "        balances[_from]--;\n" +
            "        balances[_to]++;\n" +
            "        uint oldIndex = tokenTokenIndexes[_tokenId];\n" +
            "        if(oldIndex != ownerTokenIndexes[_from].length - 1){\n" +
            "            ownerTokenIndexes[_from][oldIndex] = ownerTokenIndexes[_from][ownerTokenIndexes[_from].length - 1];\n" +
            "            tokenTokenIndexes[ownerTokenIndexes[_from][oldIndex]] = oldIndex;\n" +
            "        }\n" +
            "        ownerTokenIndexes[_from].pop();\n" +
            "        tokenTokenIndexes[_tokenId] = ownerTokenIndexes[_to].length;\n" +
            "        ownerTokenIndexes[_to].push(_tokenId);\n" +
            "    }\n" +
            "\n" +
            "    function issueTokens(uint256 _extraTokens) public{\n" +
            "        require(msg.sender == creator);\n" +
            "        balances[msg.sender] = balances[msg.sender] + (_extraTokens);\n" +
            "        uint thisId;\n" +
            "        for(uint i = 0; i < _extraTokens; i++){\n" +
            "            thisId = maxId + i + 1;\n" +
            "            tokenTokenIndexes[thisId] = ownerTokenIndexes[creator].length;\n" +
            "            ownerTokenIndexes[creator].push(thisId);\n" +
            "            indexTokens[thisId] = tokenIndexes.length;\n" +
            "            tokenIndexes.push(thisId);\n" +
            "        }\n" +
            "        maxId = maxId + _extraTokens;\n" +
            "    }\n" +
            "\n" +
            "    function burnToken(uint256 _tokenId) public{\n" +
            "        address owner = ownerOf(_tokenId);\n" +
            "        require(owner == msg.sender);\n" +
            "        burned[_tokenId] = true;\n" +
            "        balances[owner]--;\n" +
            "        uint oldIndex = tokenTokenIndexes[_tokenId];\n" +
            "        if(oldIndex != ownerTokenIndexes[owner].length - 1){\n" +
            "            ownerTokenIndexes[owner][oldIndex] = ownerTokenIndexes[owner][ownerTokenIndexes[owner].length - 1];\n" +
            "            tokenTokenIndexes[ownerTokenIndexes[owner][oldIndex]] = oldIndex;\n" +
            "        }\n" +
            "        ownerTokenIndexes[owner].pop();\n" +
            "        delete tokenTokenIndexes[_tokenId];\n" +
            "        oldIndex = indexTokens[_tokenId];\n" +
            "        if(oldIndex != tokenIndexes.length - 1){\n" +
            "            tokenIndexes[oldIndex] = tokenIndexes[tokenIndexes.length - 1];\n" +
            "            indexTokens[ tokenIndexes[oldIndex] ] = oldIndex;\n" +
            "        }\n" +
            "        tokenIndexes.pop();\n" +
            "        delete indexTokens[_tokenId];\n" +
            "    }\n" +
            "\n" +
            "    function name() external view returns (string memory _name) {\n" +
            "        _name = nftName;\n" +
            "    }\n" +
            "\n" +
            "    function symbol() external view returns (string memory _symbol){\n" +
            "        _symbol = nftSymbol;\n" +
            "    }\n" +
            "}";

    @Test
    public void compileContractTest() {
        DeployContractParam result = null;
        try {
            String contract = erc721;
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
            String contract = erc721;
            result = ledgerYiApiService.compileSingleContract(contract);
            result.setConstructor("constructor(string,string,uint256)");
            ArrayList<Object> args = Lists.newArrayList();
            args.add("ERC721Basic");
            args.add("BSC");
            args.add(1000);
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
    private static String contractAddres = "52d08783ff98194356dda20f57f24d49293dc104";

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
        List args = Arrays.asList(5);
        String method = "issueTokens(uint256)";
        triggerContract(method, args,false);
    }

    @Test
    public void balanceOf() {
        List args = Arrays.asList(contractAddres);
        String method = "balanceOf(address)";
        triggerContract(method, args, true);
    }

    @Test
    public void tokenByIndex(){
        List args = Arrays.asList(40);
        String method = "tokenByIndex(uint256)";
        triggerContract(method, args, true);
    }

    @Test
    public void ownerOf(){
        List args = Arrays.asList(4);
        String method = "ownerOf(uint256)";
        triggerContract(method, args, true);
    }

    @Test
    public void tokenOfOwnerByIndex(){
        List args = Arrays.asList(contractAddres,0);
        String method = "tokenOfOwnerByIndex(address,uint256)";
        triggerContract(method, args, true);
    }

    @Test
    public void burn() {
        List args = Arrays.asList(4);
        String method = "burnToken(uint256)";
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
        List args = Arrays.asList(ownerAddress,receiver,4);
        String method = "transferFrom(address,address,uint256)";
        triggerContract(method, args, false);
    }

    @Test
    public void name() {
        List args = Collections.EMPTY_LIST;
        String method = "name()";
        triggerContract(method, args, true);
        System.out.println(Hex.toHexString(method.getBytes()));
    }

    @Test
    public void symbol() {
        List args = Collections.EMPTY_LIST;
        String method = "symbol()";
        triggerContract(method, args, true);
        System.out.println(Hex.toHexString(method.getBytes()));
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
            System.out.println("trigger contract result: " + Strings.fromByteArray(result.getCallResult().toByteArray()));
        }
    }
}
