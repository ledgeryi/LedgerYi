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

/**
 * 溯源代理合约测试
 * @author Brian
 * @date 2021/8/3 18:28
 */
public class TracingProxyTests {
    private static String privateKey = "ec19148056c4cfc5fc1b1923b8bb657e1e481a8f092415d5af96dd60f3e6806d";
    private static String ownerAddress = "42979c83d087b612fdc82c560b3131b9c7f34a76";
    private LedgerYiStccApiService ledgerYiStccApiService;
    private static String contractAddress = "2ba39017f09720f63533b44f4d1c3dfe7a608ff4";

    @Before
    public void start(){
        ledgerYiStccApiService = new LedgerYiStccApiService();
    }

    @Test//部署溯源代理合约
    public void deployProxyTracingContract(){
        List<Object> params = Arrays.asList("创建人","合约中文名称","合约英文名称","fwerfwejg8u387t38");
        DeployContractReturn deployContractReturn = ledgerYiStccApiService.deployTracingProxyContract(ownerAddress, privateKey, params);
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
        ContractBaseInfo witnessBaseInfo = ledgerYiStccApiService.getTracingProxyBaseInfo(ownerAddress, contractAddress);
        System.out.println(witnessBaseInfo.toString());
    }

    @Test
    public void addTraceLink() {
//        ledgerYiStccApiService
    }
}
