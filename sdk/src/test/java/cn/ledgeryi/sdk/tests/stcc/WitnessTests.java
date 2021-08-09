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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存证合约测试
 * @author Brian
 * @date 2021/8/2 18:43
 */
public class WitnessTests {

    private static String privateKey = "ec19148056c4cfc5fc1b1923b8bb657e1e481a8f092415d5af96dd60f3e6806d";
    private static String ownerAddress = "42979c83d087b612fdc82c560b3131b9c7f34a76";

    private LedgerYiStccApiService ledgerYiStccApiService;

    private static String contractAddress = "0e94bab5376a345e2e1f8121dae64a8733088807";


    @Before
    public void start(){
        ledgerYiStccApiService = new LedgerYiStccApiService();
    }

    @Test//部署存证合约
    public void deployWitnessContract() throws CreateContractExecption, ContractException {
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
    public void getWitnessBaseInfos() {
        ContractBaseInfo witnessBaseInfo = ledgerYiStccApiService.getWitnessBaseInfo(ownerAddress, contractAddress);
        System.out.println(witnessBaseInfo.toString());
    }

    @Test
    public void addKey() {
        List<Object> keys = Arrays.asList("k1","k2");
        boolean result = ledgerYiStccApiService.addWitnessInfo(ownerAddress, privateKey, contractAddress, keys);
        System.out.println(result);
    }

    @Test
    public void getKey(){
        List<String> witnessInfo = ledgerYiStccApiService.getWitnessInfo(ownerAddress, contractAddress);
        System.out.println(witnessInfo.toString());
    }

    @Test
    public void addDataToWitness() throws CallContractExecption {
        List<Object> args = Arrays.asList("g","h");
        String storeID = ledgerYiStccApiService.saveDataInfo(ownerAddress, privateKey, contractAddress, args);
        System.out.println(storeID);
    }

    @Test
    public void addMapDataToWitness() throws CallContractExecption {
        HashMap<String, String> map = new HashMap<>();
        List<String> witnessInfo = ledgerYiStccApiService.getWitnessInfo(ownerAddress, contractAddress);
        long value = 0;
        for (String key : witnessInfo) {
            value++;
            map.put(key, String.valueOf(value));
        }
        String storeId = ledgerYiStccApiService.saveDataInfo(ownerAddress, privateKey, contractAddress, map);
        System.out.println(storeId);
    }

    @Test
    public void getData(){
        long dataIndex = 1;
        Map<String, String> dataInfo = ledgerYiStccApiService.getDataInfo(ownerAddress, contractAddress, dataIndex);
        System.out.println(dataInfo.toString());
    }

    @Test
    public void getLatestData(){
        ownerAddress = "338f60e4d99feea0764ad49264dfd6dc3ed1d724";
        Map<String, String> dataInfo = ledgerYiStccApiService.getLatestDataInfo(ownerAddress, contractAddress);
        System.out.println(dataInfo.toString());
    }

    @Test
    public void witnessDataVerify() {
        Map data = new HashMap();
        data.put("k2","2");
        data.put("k1","1");
        long dataVersion = 1;
        boolean contains = ledgerYiStccApiService.witnessDataVerify(ownerAddress,contractAddress,dataVersion,data);
        if(contains){
            System.out.println("两个Map对象相同");
        }else{
            System.out.println("这不是两个相同的Map对象");
        }
    }

    @Test
    public void witnessDataVerifyPermissionless() {
        Map data = new HashMap();
        data.put("k1","g");
        data.put("k2","h");
        long dataVersion = 0;
        try {
            boolean contains = ledgerYiStccApiService.witnessDataVerifyPermissionless(ownerAddress,
                    contractAddress,dataVersion,data);
            System.out.println(contains);
        } catch (CallContractExecption callContractExecption) {
            callContractExecption.printStackTrace();
        }
    }

    @Test
    public void addUserToDataWhiteList() {
//        ownerAddress = "fbb859ffc4a0a2274fd35b121cd5a22d8946bf72";
//        privateKey = "7d25da08a45bc9a0841171fbf2048e41a9840fcca14184aba06f7769fff89fa0";

//        ownerAddress = "338f60e4d99feea0764ad49264dfd6dc3ed1d724";
//        privateKey = "3f177d8a0f3725b34b7866f080e43696b4612c4f295cf277fae7a9721ed770d1";
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
        String user = "338f60e4d99feea0764ad49264dfd6dc3ed1d724";
//        ownerAddress = "fbb859ffc4a0a2274fd35b121cd5a22d8946bf72";
//        privateKey = "7d25da08a45bc9a0841171fbf2048e41a9840fcca14184aba06f7769fff89fa0";
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
