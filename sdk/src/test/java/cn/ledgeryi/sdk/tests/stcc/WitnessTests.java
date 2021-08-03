package cn.ledgeryi.sdk.tests.stcc;

import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.serverapi.LedgerYiStccApiService;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.stcc.ContractBaseInfo;
import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Brian
 * @date 2021/8/2 18:43
 */
public class WitnessTests {

    private static String privateKey = "ec19148056c4cfc5fc1b1923b8bb657e1e481a8f092415d5af96dd60f3e6806d";
    private static String ownerAddress = "42979c83d087b612fdc82c560b3131b9c7f34a76";
    //private static String contractAddress = "4e8d5a71e03cac9cc40c2db169130d9cd1a1de08";
    private static String contractAddress = "cbee1a51865e9da945d40a4d15a55e573d3f3d9b";

    private LedgerYiStccApiService ledgerYiStccApiService;

    @Before
    public void start(){
        ledgerYiStccApiService = new LedgerYiStccApiService();
    }

    @Test
    public void deploy(){
        List<Object> params = Arrays.asList("创建者HQ","合约中文名称","合约英文名称");
        DeployContractReturn deployContractReturn = ledgerYiStccApiService.deployWitnessContract(ownerAddress, privateKey, params);
        String contractAddress = deployContractReturn.getContractAddress();
        System.out.println("合约地址： " + contractAddress);
    }

    @Test
    public void getContractFromOnChain(){
        SmartContractOuterClass.SmartContract contract = ledgerYiStccApiService.getContract(DecodeUtil.decode(contractAddress));
        System.out.println(JsonFormatUtil.printSmartContract(contract));
        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(contract.getAbi()));
        String abi = jsonObject.getString("entrys");
        System.out.println(abi);
    }

    @Test
    public void getStatusOfContractWhite() {
        boolean status = ledgerYiStccApiService.getStatusOfContractWhite(ownerAddress, contractAddress);
        System.out.println(status);
    }

    @Test
    public void disableStatusOfContractWhite() {
        ledgerYiStccApiService.disableStatusOfContractWhite(ownerAddress, privateKey, contractAddress);
    }

    @Test
    public void enableStatusOfContractWhite() {
        ledgerYiStccApiService.enableStatusOfContractWhite(ownerAddress, privateKey, contractAddress);
    }

    @Test
    public void addUserToContractWhiteList() {
//        address: fbb859ffc4a0a2274fd35b121cd5a22d8946bf72
//        privateKey: 7d25da08a45bc9a0841171fbf2048e41a9840fcca14184aba06f7769fff89fa0

//        address: 338f60e4d99feea0764ad49264dfd6dc3ed1d724
//        privateKey: 3f177d8a0f3725b34b7866f080e43696b4612c4f295cf277fae7a9721ed770d1
        String user = "fbb859ffc4a0a2274fd35b121cd5a22d8946bf72";
        boolean result = ledgerYiStccApiService.addUserToContractWhiteList(ownerAddress, privateKey, contractAddress, user);
        System.out.println(result);
    }

    @Test
    public void removeUserFromContractWhiteList() {
        String user = "338f60e4d99feea0764ad49264dfd6dc3ed1d724";
        boolean result = ledgerYiStccApiService.removeUserToContractWhiteList(ownerAddress, privateKey, contractAddress, user);
        System.out.println(result);
    }

    @Test
    public void getUsersFromContractWhiteList() {
        List<String> usersFromContractWhiteList = ledgerYiStccApiService.getUsersFromContractWhiteList(ownerAddress, contractAddress);
        System.out.println(usersFromContractWhiteList);
    }

    @Test
    public void getBaseInfos() {
        ContractBaseInfo witnessBaseInfo = ledgerYiStccApiService.getWitnessBaseInfo(ownerAddress, contractAddress);
        System.out.println(witnessBaseInfo.toString());
    }

    @Test
    public void addKey() {
        List<Object> params = Arrays.asList("[\"k1","k2\"]}");
        boolean result = ledgerYiStccApiService.addWitnessInfo(ownerAddress, privateKey, contractAddress, params);
        System.out.println(result);
    }

    @Test
    public void getKey(){
        List<String> witnessInfo = ledgerYiStccApiService.getWitnessInfo(ownerAddress, contractAddress);
        System.out.println(witnessInfo.toString());
    }

    @Test
    public void addData(){
        List<Object> args = Arrays.asList("[\"c","d\"]");
        long result = ledgerYiStccApiService.saveDataInfo(ownerAddress, privateKey, contractAddress, args);
        System.out.println(result);
    }

    @Test
    public void getData(){
        long dataIndex = 0;
        Map<String, String> dataInfo = ledgerYiStccApiService.getDataInfo(ownerAddress, contractAddress, dataIndex);
        System.out.println(dataInfo.toString());
    }

    @Test
    public void addUserToDataWhiteList() {
//        address: fbb859ffc4a0a2274fd35b121cd5a22d8946bf72
//        privateKey: 7d25da08a45bc9a0841171fbf2048e41a9840fcca14184aba06f7769fff89fa0

//        address: 338f60e4d99feea0764ad49264dfd6dc3ed1d724
//        privateKey: 3f177d8a0f3725b34b7866f080e43696b4612c4f295cf277fae7a9721ed770d1
        long dataIndex = 0;
        String user = "338f60e4d99feea0764ad49264dfd6dc3ed1d724";
        boolean result = ledgerYiStccApiService.addUserToDataWhiteList(ownerAddress, privateKey, contractAddress, dataIndex, user);
        System.out.println(result);
    }

    @Test
    public void getUsersFromDataWhiteList(){
        long dataIndex = 0;
        List<String> users = ledgerYiStccApiService.getUsersFromDataWhiteList(ownerAddress, contractAddress, dataIndex);
        System.out.println(users.toString());
    }

    @Test
    public void removeUserFromDataWhiteList(){
        long dataIndex = 0;
        String user = "fbb859ffc4a0a2274fd35b121cd5a22d8946bf72";
        boolean result = ledgerYiStccApiService.removeUserToDataWhiteList(ownerAddress, privateKey, contractAddress, dataIndex, user);
        System.out.println(result);
    }

    @Test
    public void getStatusOfDataWhite(){
        long dataIndex = 0;
        boolean statusOfDataWhite = ledgerYiStccApiService.getStatusOfDataWhite(ownerAddress, contractAddress, dataIndex);
        System.out.println(statusOfDataWhite);
    }

    @Test
    public void disableStatusOfDataWhite(){
        long dataIndex = 0;
        ledgerYiStccApiService.disableStatusOfDataWhite(ownerAddress, privateKey, contractAddress, dataIndex);
    }

    @Test
    public void enableStatusOfDataWhite(){
        long dataIndex = 0;
        ledgerYiStccApiService.enableStatusOfDataWhite(ownerAddress, privateKey, contractAddress, dataIndex);
    }

}
