package cn.ledgeryi.sdk.tests.stcc;

import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.CallContractExecption;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
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
 * 溯源合约测试
 * @author Brian
 * @date 2021/8/5 17:09
 */
public class TracingTests {
    private static String privateKey = "ec19148056c4cfc5fc1b1923b8bb657e1e481a8f092415d5af96dd60f3e6806d";
    private static String ownerAddress = "42979c83d087b612fdc82c560b3131b9c7f34a76";

    private static String contractAddress = "8d55a69ffb8919fc3fb8f874e12c74304d7afa60";;

    private LedgerYiStccApiService ledgerYiStccApiService;

    @Before
    public void start(){
        ledgerYiStccApiService = new LedgerYiStccApiService();
    }

    @Test//部署溯源合约
    public void deployTracingContract() throws CreateContractExecption, ContractException {
        List<Object> params = Arrays.asList("fwerfwejg8u387t38","溯源登记信息组名称");
        DeployContractReturn deployContractReturn = ledgerYiStccApiService.deployTracingContract(ownerAddress, privateKey, params);
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
    public void getTracingBaseInfos() {
        ContractBaseInfo witnessBaseInfo = ledgerYiStccApiService.getTracingBaseInfo(ownerAddress, contractAddress);
        System.out.println(witnessBaseInfo.toString());
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
        // address: fbb859ffc4a0a2274fd35b121cd5a22d8946bf72
        // privateKey: 7d25da08a45bc9a0841171fbf2048e41a9840fcca14184aba06f7769fff89fa0

        // address: 309ae8dc03a1ff131cc75bf5d0f91eee67d8eff0
        // privateKey: 8751b33e109daca1737415ccb3b34a6bb7eef98bbd0949311d3b3faeaa3c08db
        String user = "309ae8dc03a1ff131cc75bf5d0f91eee67d8eff0";
        boolean result = ledgerYiStccApiService.addUserToContractWhiteList(ownerAddress, privateKey, contractAddress, user);
        System.out.println(result);
    }

    @Test
    public void removeUserFromContractWhiteList() {
        String user = "309ae8dc03a1ff131cc75bf5d0f91eee67d8eff0";
        boolean result = ledgerYiStccApiService.removeUserToContractWhiteList(ownerAddress, privateKey, contractAddress, user);
        System.out.println(result);
    }

    @Test
    public void getUsersFromContractWhiteList() {
        List<String> usersFromContractWhiteList = ledgerYiStccApiService.getUsersFromContractWhiteList(ownerAddress, contractAddress);
        System.out.println(usersFromContractWhiteList);
    }

    @Test
    public void addKey() {
        List<Object> keys = Arrays.asList("k5");
        boolean result = ledgerYiStccApiService.addWitnessInfo(ownerAddress, privateKey, contractAddress, keys);
        System.out.println(result);
    }

    @Test
    public void getKey(){
        List<String> witnessInfo = ledgerYiStccApiService.getWitnessInfo(ownerAddress, contractAddress);
        System.out.println(witnessInfo.toString());
    }

    @Test
    public void addDataToTracing() throws CallContractExecption {
        List<Object> args = Arrays.asList("1","2","3","4");
        String traceId = "001";
        String storeID = ledgerYiStccApiService.saveDataInfo(ownerAddress, privateKey, contractAddress, traceId, args);
        System.out.println(storeID);
    }

    @Test
    public void getData(){
        String traceId = "001";
        long dataIndex = 0;
        Map<String, String> dataInfo = ledgerYiStccApiService.getDataInfo(ownerAddress, contractAddress, traceId, dataIndex);
        System.out.println(dataInfo.toString());
    }

    @Test
    public void getLatestData(){
        String traceId = "001";
        ownerAddress = "338f60e4d99feea0764ad49264dfd6dc3ed1d724";
        Map<String, String> dataInfo = ledgerYiStccApiService.getLatestDataInfo(ownerAddress, contractAddress, traceId);
        System.out.println(dataInfo.toString());
    }

    @Test
    public void addUserToDataWhiteList() {
//        ownerAddress = "fbb859ffc4a0a2274fd35b121cd5a22d8946bf72";
//        privateKey = "7d25da08a45bc9a0841171fbf2048e41a9840fcca14184aba06f7769fff89fa0";

//        ownerAddress = "338f60e4d99feea0764ad49264dfd6dc3ed1d724";
//        privateKey = "3f177d8a0f3725b34b7866f080e43696b4612c4f295cf277fae7a9721ed770d1";
        String traceId = "001";
        long dataIndex = 0;
        String user = "338f60e4d99feea0764ad49264dfd6dc3ed1d724";
        boolean result = ledgerYiStccApiService.addUserToDataWhiteList(ownerAddress,
                privateKey, contractAddress, traceId, dataIndex, user);
        System.out.println(result);
    }

    @Test
    public void getUsersFromDataWhiteList(){
        String traceId = "001";
        long dataIndex = 0;
        List<String> users = ledgerYiStccApiService.getUsersFromDataWhiteList(ownerAddress, contractAddress, traceId, dataIndex);
        System.out.println(users.toString());
    }

    @Test
    public void removeUserFromDataWhiteList(){
        String traceId = "001";
        long dataIndex = 0;
        String user = "338f60e4d99feea0764ad49264dfd6dc3ed1d724";
        ownerAddress = "fbb859ffc4a0a2274fd35b121cd5a22d8946bf72";
        privateKey = "7d25da08a45bc9a0841171fbf2048e41a9840fcca14184aba06f7769fff89fa0";
        boolean result = ledgerYiStccApiService.removeUserToDataWhiteList(ownerAddress,
                privateKey, contractAddress, traceId, dataIndex, user);
        System.out.println(result);
    }

    @Test
    public void getStatusOfDataWhite(){
        String traceId = "001";
        long dataIndex = 0;
        boolean statusOfDataWhite = ledgerYiStccApiService.getStatusOfDataWhite(ownerAddress,
                contractAddress, traceId, dataIndex);
        System.out.println(statusOfDataWhite);
    }

    @Test
    public void disableStatusOfDataWhite(){
        String traceId = "001";
        long dataIndex = 0;
        ledgerYiStccApiService.disableStatusOfDataWhite(ownerAddress, privateKey, contractAddress, traceId, dataIndex);
    }

    @Test
    public void enableStatusOfDataWhite(){
        String traceId = "001";
        long dataIndex = 0;
        ledgerYiStccApiService.enableStatusOfDataWhite(ownerAddress, privateKey, contractAddress, traceId, dataIndex);
    }
}
