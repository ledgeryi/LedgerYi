package cn.ledgeryi.sdk.tests.stcc;

import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.exception.AddressException;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
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
    private static String proxyContractAddress = "8ace3e86a2a850c4d1aaed837176127638e6fb92";

    @Before
    public void start(){
        ledgerYiStccApiService = new LedgerYiStccApiService();
    }

    @Test//部署溯源代理合约
    public void deployProxyTracingContract() throws CreateContractExecption, ContractException, AddressException {
        List<Object> params = Arrays.asList("创建人","合约中文名称","合约英文名称","fwerfwejg8u387t38");
        DeployContractReturn deployContractReturn = ledgerYiStccApiService.deployTracingProxyContract(ownerAddress, privateKey, params);
        String contractAddress = deployContractReturn.getContractAddress();
        System.out.println("合约地址： " + contractAddress);
    }

    @Test//部署溯源合约
    public void deployTracingContract() throws CreateContractExecption, ContractException, AddressException {
        List<Object> args = Arrays.asList("fwerfwejg8u387t38","溯源登记信息组壹");
        DeployContractReturn deployContractReturn = ledgerYiStccApiService.deployTracingContract(ownerAddress,privateKey,args);
        String contractAddress = deployContractReturn.getContractAddress();
        System.out.println("合约地址： " + contractAddress);
    }

    @Test
    public void getContractFromOnChain(){
        SmartContractOuterClass.SmartContract contract = ledgerYiStccApiService.getContract(DecodeUtil.decode(proxyContractAddress));
        System.out.println(JsonFormatUtil.printSmartContract(contract));
        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(contract.getAbi()));
        String abi = jsonObject.getString("entrys");
        System.out.println(abi);
    }

    @Test
    public void TracingProxyBaseInfo() throws AddressException {
        ContractBaseInfo witnessBaseInfo = ledgerYiStccApiService.getTracingProxyBaseInfo(ownerAddress, proxyContractAddress);
        System.out.println(witnessBaseInfo.toString());
    }

    @Test
    public void addTraceLink() throws AddressException {
        List<Object> args = Arrays.asList("[\"第1个溯源环节","第2个溯源环节\"]");
        ledgerYiStccApiService.addTraceLink(ownerAddress,privateKey,proxyContractAddress,args);
    }

    @Test
    public void getTraceLinkNames() throws AddressException {
        List<String> traceLinkNames = ledgerYiStccApiService.getTraceLinkNames(ownerAddress, proxyContractAddress);
        System.out.println(traceLinkNames.toString());
    }

    @Test
    public void addContractToTraceLink() throws AddressException {
        String linkName = "第2个溯源环节";
        String traceContract = "48b6b7317970886f639b5a0a398f4368c709884e";
        traceContract = "3bd3097884bacbc1b45b6a3ec82f3cef9cedf7bd";
        boolean link = ledgerYiStccApiService.addContractToTraceLink(ownerAddress,
                privateKey, proxyContractAddress, linkName, traceContract);
        System.out.println(link);
    }

    @Test
    public void getAllContactsOfTracLink() throws AddressException {
        String linkName = "第2个溯源环节";
        List<String> allContacts = ledgerYiStccApiService.getAllContactsOfTraceLink(ownerAddress, proxyContractAddress, linkName);
        System.out.println(allContacts.toString());
    }

}
