package cn.ledgeryi.sdk.tests.stcc;

import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import cn.ledgeryi.sdk.serverapi.LedgerYiStccApiService;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.stcc.ContractBaseInfo;
import com.alibaba.fastjson.JSONObject;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Brian
 * @date 2021/8/2 18:43
 */
public class WitnessTests {

    private static String privateKey = "ec19148056c4cfc5fc1b1923b8bb657e1e481a8f092415d5af96dd60f3e6806d";
    private static String ownerAddress = "42979c83d087b612fdc82c560b3131b9c7f34a76";
    private static String contractAddress = "f51c609f6a65b37c36d9d3d8ea927c35e2d6cacd";


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
    public void getBaseInfos() {
        ContractBaseInfo witnessBaseInfo = ledgerYiStccApiService.getWitnessBaseInfo(ownerAddress, contractAddress);
        System.out.println(witnessBaseInfo.toString());
    }

}
