package cn.ledgeryi.sdk.tests.stcc;

import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.AccountYi;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.AddressException;
import cn.ledgeryi.sdk.exception.CallContractExecption;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.serverapi.LedgerYiStccApiService;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.stcc.ContractBaseInfo;
import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 * 溯源合约测试
 * @author Brian
 * @date 2021/8/5 17:09
 */
public class TracingTests {
    private static String privateKey = "ec19148056c4cfc5fc1b1923b8bb657e1e481a8f092415d5af96dd60f3e6806d";
    private static String ownerAddress = "42979c83d087b612fdc82c560b3131b9c7f34a76";

    private static String contractAddress = "10054e338a80172adbc61e8c524cd4f3ba67390a";

    private LedgerYiStccApiService ledgerYiStccApiService;

    @Before
    public void start(){
        ledgerYiStccApiService = new LedgerYiStccApiService();
    }

    @Test//部署溯源合约
    public void deployTracingContract() throws CreateContractExecption, ContractException, AddressException {
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
    public void getTracingBaseInfos() throws AddressException {
        ContractBaseInfo witnessBaseInfo = ledgerYiStccApiService.getTracingBaseInfo(ownerAddress, contractAddress);
        System.out.println(witnessBaseInfo.toString());
    }

    @Test
    public void getStatusOfContractWhite() throws AddressException {
        boolean status = ledgerYiStccApiService.getStatusOfContractWhite(ownerAddress, contractAddress);
        System.out.println(status);
    }

    @Test
    public void disableStatusOfContractWhite() throws AddressException {
        ledgerYiStccApiService.disableStatusOfContractWhite(ownerAddress, privateKey, contractAddress);
    }

    @Test
    public void enableStatusOfContractWhite() throws AddressException {
        ledgerYiStccApiService.enableStatusOfContractWhite(ownerAddress, privateKey, contractAddress);
    }

    @Test
    public void addUserToContractWhiteList() throws AddressException {
        // address: fbb859ffc4a0a2274fd35b121cd5a22d8946bf72
        // privateKey: 7d25da08a45bc9a0841171fbf2048e41a9840fcca14184aba06f7769fff89fa0

        // address: 309ae8dc03a1ff131cc75bf5d0f91eee67d8eff0
        // privateKey: 8751b33e109daca1737415ccb3b34a6bb7eef98bbd0949311d3b3faeaa3c08db
        String user = "309ae8dc03a1ff131cc75bf5d0f91eee67d8eff0";
        boolean result = ledgerYiStccApiService.addUserToContractWhiteList(ownerAddress, privateKey, contractAddress, user);
        System.out.println(result);
    }

    @Test
    public void addUsersToContractWhiteList() throws AddressException {
        ArrayList<String> users = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            AccountYi account = ledgerYiStccApiService.createDefaultAccount();
            users.add(account.getAddress());
        }

        boolean result = ledgerYiStccApiService.addUsersToContractWhiteList(ownerAddress, privateKey, contractAddress, users);
        System.out.println(result);
    }

    @Test
    public void removeUserFromContractWhiteList() throws AddressException {
        String user = "309ae8dc03a1ff131cc75bf5d0f91eee67d8eff0";
        boolean result = ledgerYiStccApiService.removeUserToContractWhiteList(ownerAddress, privateKey, contractAddress, user);
        System.out.println(result);
    }

    @Test
    public void getUsersFromContractWhiteList() throws AddressException {
        List<String> users = ledgerYiStccApiService.getUsersFromContractWhiteList(ownerAddress, contractAddress);
        System.out.println(users.size());
        System.out.println(users);
    }

    @Test
    public void getBatchUsersFromContractWhiteList() throws AddressException, CallContractExecption {
        int start = 49;
        int size = 100;
        List<String> users = ledgerYiStccApiService.getUsersFromContractWhiteList(ownerAddress, contractAddress,start,size);
        System.out.println(users.size());
        System.out.println(users);
    }

    @Test
    public void addKey() throws AddressException {
        List<Object> keys = Arrays.asList("k1","k2");
        boolean result = ledgerYiStccApiService.addWitnessInfo(ownerAddress, privateKey, contractAddress, keys);
        System.out.println(result);
    }

    @Test
    public void getKey() throws AddressException {
        List<String> witnessInfo = ledgerYiStccApiService.getWitnessInfo(ownerAddress, contractAddress);
        System.out.println(witnessInfo.toString());
    }

    @Test
    public void addDataToTracing() throws CallContractExecption {
        List<Object> args = Arrays.asList("3","4");
        String traceId = "001";
        String storeID = ledgerYiStccApiService.saveDataInfo(ownerAddress, privateKey, contractAddress, traceId, args);
        System.out.println(storeID);
    }

    @Test
    public void addMapDataToTracing() throws CallContractExecption, AddressException {
        HashMap<String, String> map = new HashMap<>();
        List<String> witnessInfo = ledgerYiStccApiService.getWitnessInfo(ownerAddress, contractAddress);
        long value = 5;
        for (String key : witnessInfo) {
            value++;
            map.put(key, String.valueOf(value));
        }
        String traceId = "001";
        String storeId = ledgerYiStccApiService.saveDataInfo(ownerAddress, privateKey, contractAddress, traceId, map);
        System.out.println(storeId);
    }

    @Test
    public void getData() throws AddressException {
        String traceId = "001";
        long dataIndex = 0;
        Map<String, String> dataInfo = ledgerYiStccApiService.getDataInfo(ownerAddress, contractAddress, traceId, dataIndex);
        System.out.println(dataInfo.toString());
    }

    @Test
    public void getLatestData() throws AddressException {
        String traceId = "001";
        //ownerAddress = "338f60e4d99feea0764ad49264dfd6dc3ed1d724";
        Map<String, String> dataInfo = ledgerYiStccApiService.getLatestDataInfo(ownerAddress, contractAddress, traceId);
        System.out.println(dataInfo.toString());
    }

    @Test
    public void traceDataVerify() throws AddressException {
        Map data = new HashMap();
        data.put("k1","6");
        data.put("k2","7");
        String traceId = "001";
        long dataVersion = 2;
        boolean contains = ledgerYiStccApiService.traceDataVerify(ownerAddress,contractAddress,traceId,dataVersion,data);
        if(contains){
            System.out.println("两个Map对象相同");
        }else{
            System.out.println("这不是两个相同的Map对象");
        }
    }

    @Test
    public void traceDataVerifyPermissionless() {
        Map data = new HashMap();
        data.put("k1","6");
        data.put("k2","7");
        String traceId = "001";
        long dataVersion = 1;
        boolean verify = false;
        try {
            verify = ledgerYiStccApiService.traceDataVerifyPermissionless(ownerAddress,
                    contractAddress, traceId, dataVersion, data);
        } catch (CallContractExecption callContractExecption) {
            callContractExecption.printStackTrace();
        } catch (AddressException e) {
            e.printStackTrace();
        }
        System.out.println(verify);
    }

    @Test
    public void addUserToDataWhiteList() throws AddressException {
        String traceId = "001";
        long dataIndex = 0;
        String user = "338f60e4d99feea0764ad49264dfd6dc3ed1d724";
        boolean result = ledgerYiStccApiService.addUserToDataWhiteList(ownerAddress,
                privateKey, contractAddress, traceId, dataIndex, user);
        System.out.println(result);
    }

    @Test
    public void addUsersToDataWhiteList() throws AddressException, CallContractExecption {
        String traceId = "001";
        long dataIndex = 0;
        ArrayList<String> users = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            AccountYi account = ledgerYiStccApiService.createDefaultAccount();
            users.add(account.getAddress());
        }
        boolean result = ledgerYiStccApiService.addUsersToDataWhiteList(ownerAddress,
                privateKey, contractAddress, traceId, dataIndex, users);
        System.out.println(result);
    }

    @Test
    public void getUsersFromDataWhiteList() throws AddressException {
        String traceId = "001";
        long dataIndex = 0;
        long start = System.currentTimeMillis();
        List<String> users = ledgerYiStccApiService.getUsersFromDataWhiteList(ownerAddress, contractAddress, traceId, dataIndex);
        System.out.println(System.currentTimeMillis() - start);
        System.out.println(users.size());
        System.out.println(users.toString());
    }

    @Test
    public void getBatchUsersFromDataWhiteList() throws AddressException, CallContractExecption {
        String traceId = "001";
        long dataIndex = 0;
        int start = 0;
        int size = 100;
        long startTime = System.currentTimeMillis();
        List<String> users = ledgerYiStccApiService.getUsersFromDataWhiteList(ownerAddress, contractAddress, traceId, dataIndex, start, size);
        System.out.println(System.currentTimeMillis() - startTime);
        System.out.println(users.size());
        System.out.println(users.toString());
    }

    @Test
    public void removeUserFromDataWhiteList() throws AddressException {
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
    public void getStatusOfDataWhite() throws AddressException {
        String traceId = "001";
        long dataIndex = 1;
        boolean statusOfDataWhite = ledgerYiStccApiService.getStatusOfDataWhite(ownerAddress,
                contractAddress, traceId, dataIndex);
        System.out.println(statusOfDataWhite);
    }

    @Test
    public void disableStatusOfDataWhite() throws AddressException {
        String traceId = "001";
        long dataIndex = 1;
        ledgerYiStccApiService.disableStatusOfDataWhite(ownerAddress, privateKey, contractAddress, traceId, dataIndex);
    }

    @Test
    public void enableStatusOfDataWhite() throws AddressException {
        String traceId = "001";
        long dataIndex = 1;
        ledgerYiStccApiService.enableStatusOfDataWhite(ownerAddress, privateKey, contractAddress, traceId, dataIndex);
    }
}
