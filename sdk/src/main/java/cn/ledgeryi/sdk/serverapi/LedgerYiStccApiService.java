package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.sdk.common.utils.ByteUtil;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.event.CallTransaction;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.serverapi.data.DeployContractParam;
import cn.ledgeryi.sdk.serverapi.data.DeployContractReturn;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractParam;
import cn.ledgeryi.sdk.serverapi.data.TriggerContractReturn;
import cn.ledgeryi.sdk.serverapi.data.stcc.ContractBaseInfo;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.Strings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 国家电网质检链API
 * @author Brian
 * @date 2021/8/2 14:04
 */
@Slf4j
public class LedgerYiStccApiService extends LedgerYiApiService {

    public LedgerYiStccApiService() {
        super();
    }

    /**
     * 部署存证合约
     * @param ownerAddress 部署者地址
     * @param privateKey 部署者私钥
     * @param args 合约参数
     */
    public DeployContractReturn deployWitnessContract(String ownerAddress, String privateKey, List<Object> args) {
        DeployContractReturn deployContract = null;
        Assert.assertTrue("args size unequal to 3",args.size() == 3);
        try {
            Path source = Paths.get("src","main/resources/contract","Witness.sol");
            DeployContractParam param = compileContractFromFile(source,"Witness");
            param.setConstructor("constructor(string,string,string)");
            param.setArgs(args);
            deployContract = deployContract(DecodeUtil.decode(ownerAddress), DecodeUtil.decode(privateKey), param);
        } catch (ContractException | CreateContractExecption e) {
            e.printStackTrace();
            log.error("contract compile error: " + e.getMessage());
        }
        return deployContract;
    }


    /**
     * 部署溯源代理合约
     * @param ownerAddress 部署者地址
     * @param privateKey 部署者私钥
     * @param args 合约参数
     */
    public DeployContractReturn deployTracingProxyContract(String ownerAddress, String privateKey, ArrayList<Object> args) {
        DeployContractReturn deployContract = null;
        try {
            Path source = Paths.get("src","resources","TracingProxy.sol");
            DeployContractParam param = compileContractFromFile(source,"TracingProxy");
            param.setConstructor("constructor(string,string,string)");
            param.setArgs(args);
            deployContract = deployContract(DecodeUtil.decode(ownerAddress), DecodeUtil.decode(privateKey), param);
        } catch (ContractException | CreateContractExecption e) {
            e.printStackTrace();
            log.error("contract compile error: " + e.getMessage());
        }
        return deployContract;
    }

    /**
     * 部署溯源合约
     * @param ownerAddress 部署者地址
     * @param privateKey 部署者私钥
     * @param args 合约参数
     */
    public DeployContractReturn deployTracingContract(String ownerAddress, String privateKey, ArrayList<Object> args) {
        DeployContractReturn deployContract = null;
        try {
            Path source = Paths.get("src","resources","Tracing.sol");
            DeployContractParam param = compileContractFromFile(source,"Tracing");
            param.setConstructor("constructor(string,string)");
            param.setArgs(args);
            deployContract = deployContract(DecodeUtil.decode(ownerAddress), DecodeUtil.decode(privateKey), param);
        } catch (ContractException | CreateContractExecption e) {
            e.printStackTrace();
            log.error("contract compile error: " + e.getMessage());
        }
        return deployContract;
    }

    /**
     * 存证合约基础信息
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public ContractBaseInfo getWitnessBaseInfo(String callAddress, String contractAddress) {
        String method = "getBaseInfo()";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getBaseInfo",
                new String[]{}, new String[]{"string","string", "uint", "address"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return ContractBaseInfo.builder()
                .creator((String) objects[0])
                .nameEn((String) objects[1])
                .nameZn((String) objects[2])
                .createTime(formatter.format(objects[3]))
                .owner(DecodeUtil.createReadableString((byte[]) objects[4]))
                .build();
    }

    /**
     * 新增存证数据
     * @param callAddress 合约调用者
     * @param privateKey 调用者私钥
     * @param contractAddress 合约地址
     * @return 数据对应的链上索引，从0开始
     */
    public long addWitnessInfo(String callAddress, String privateKey, String contractAddress) {
        String method = "addDataKey(string[])";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerContract(callAddress,privateKey,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        return ByteUtil.byteArrayToLong(callResult.toByteArray());
    }

    /**
     * 存证合约的存证信息
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public List<String> getWitnessInfo(String callAddress, String contractAddress) {
        String method = "getDataKey()";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getDataKey",
                new String[]{}, new String[]{"string[]"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        for (Object object : objects) {
            if (object instanceof Object[]) {
                String[] object1 = (String[]) object;
                return Arrays.asList(object1);
            }
        }
        return Collections.emptyList();

    }

    /**
     * 获取存证数据
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     * @param dataIndex 数据对应的链上索引，从0开始
     */
    public Map<String,String> getDataInfo(String callAddress, String contractAddress, long dataIndex) {
        List<String> keys = Collections.emptyList();
        List<String> values = Collections.emptyList();
        String method = "getDataInfo(uint256)";
        List<Object> args = Collections.singletonList(dataIndex);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getDataInfo",
                new String[]{"uint256"}, new String[]{"string[],string[]"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        if (objects.length == 2) {
            if (objects[0] instanceof Object[]) {
                String[] _keys = (String[]) objects[0];
                keys = Arrays.asList(_keys);
            }
            if (objects[1] instanceof Object[]) {
                String[] _values = (String[]) objects[1];
                values = Arrays.asList(_values);
            }
            if (keys.size() == values.size()) {
                List<String> finalValues = values;
                List<String> finalKeys = keys;
                return keys.stream().collect(
                        Collectors.toMap(key->key, key-> finalValues.get(finalKeys.indexOf(key))));
            }
        }
        return Collections.emptyMap();
    }

    /**
     * 获取存证数据的共享列表
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     * @param dataIndex 存证数据索引，从0开始
     */
    public List<String> getContractShareList(String callAddress, String contractAddress, long dataIndex) {
        String method = "getUserSizeOfDataWhiteList(uint256)";
        List<Object> args = Collections.singletonList(dataIndex);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        long shareListLength = ByteUtil.byteArrayToLong(callResult.toByteArray());
        List<String> shares = new ArrayList<>();
        for (long index = 0; index < shareListLength; index++) {
            method = "getUserFromDataWhiteList(uint256,uint256)";
            args = Arrays.asList(dataIndex,index);
            callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
            callResult = callReturn.getCallResult();
            String address = DecodeUtil.createReadableString(callResult.toByteArray());
            shares.add(address);
        }
        return shares;
    }

    /**
     * 获取合约白名单启动状态
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public boolean getStatusOfContractWhite(String callAddress, String contractAddress) {
        String method = "getStatusOfContractWhite()";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getStatusOfContractWhite",
                new String[]{}, new String[]{"bool"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        if (objects.length == 1 && objects[0] instanceof Boolean) {
            return (Boolean)objects[0];
        }
        return false;
    }

    /**
     * 禁用合约白名单
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     */
    public boolean disableStatusOfContractWhite(String callAddress, String privateKey, String contractAddress) {
        String method = "disableContractWhite()";
        List<Object> args = Collections.emptyList();
        return null == triggerContract(callAddress,privateKey,contractAddress,method,args);
    }

    /**
     * 启用合约白名单
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     */
    public boolean enableStatusOfContractWhite(String callAddress, String privateKey, String contractAddress) {
        String method = "enableContractWhite()";
        List<Object> args = Collections.emptyList();
        return null == triggerContract(callAddress,privateKey,contractAddress,method,args);
    }

    /**
     * 向合约白名单添加用户
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     * @param user 被添加的用户地址
     * @return
     */
    public boolean addUserToContractWhiteList(String callAddress, String privateKey, String contractAddress, String user) {
        String method = "addUserToContractWhiteList(address)";
        List<Object> args = Collections.singletonList(user);
        return null == triggerContract(callAddress,privateKey,contractAddress,method,args);
    }

    /**
     * 从合约白名单移除用户
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     * @param user 被移除的用户地址
     */
    public boolean removeUserToContractWhiteList(String callAddress, String privateKey, String contractAddress, String user) {
        String method = "removeUserFromContractWhiteList(address)";
        List<Object> args = Collections.singletonList(user);
        return null == triggerContract(callAddress,privateKey,contractAddress,method,args);
    }

    /**
     * 获取合约白名单成员
     * @param callAddress 调用者地址
     * @param contractAddress 合约地址
     */
    public List<String> getUsersFromContractWhiteList(String callAddress, String contractAddress) {
        List<String> users = Collections.emptyList();
        long size = getUserSizeOfContractWhiteList(callAddress, contractAddress);
        String method = "getUserFromContractWhiteList(uint256)";
        List<Object> args = Collections.emptyList();
        for (long i = 0; i < size; i++) {
            TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
            ByteString callResult = callReturn.getCallResult();
            String userAddress = Strings.fromByteArray(callResult.toByteArray()).trim();
            users.add(userAddress);
        }
        return users;
    }

    /**
     * 向某次存证数据白名单添加用户
     * @param callAddress 数据拥有者地址
     * @param privateKey 数据拥有者私钥
     * @param contractAddress 合约地址
     * @param dataIndex 数据索引
     * @param user 被添加的用户
     */
    public boolean addUserToDataWhiteList(String callAddress, String privateKey,
                                          String contractAddress, long dataIndex, String user){
        String method = "addUserToDataWhiteList(uint256,address)";
        List<Object> args = Arrays.asList(dataIndex,user);
        return null == triggerContract(callAddress,privateKey,contractAddress,method,args);

    }

    /**
     * 向某次存证数据白名单移除用户
     * @param callAddress 数据拥有者地址
     * @param privateKey 数据拥有者私钥
     * @param contractAddress 合约地址
     * @param dataIndex 数据索引
     * @param user 被移除的用户
     */
    public boolean removeUserToDataWhiteList(String callAddress, String privateKey,
                                             String contractAddress,long dataIndex, String user){
        String method = "removeUserFromDataWhiteList(uint256,address)";
        List<Object> args = Arrays.asList(dataIndex,user);
        return null == triggerContract(callAddress,privateKey,contractAddress,method,args);
    }

    /**
     * 获取某次存证数据白名单成员
     * @param callAddress 调用者地址
     * @param contractAddress 合约地址
     */
    public List<String> getUsersFromDataWhiteList(String callAddress, String contractAddress, long dataIndex) {
        List<String> users = Collections.emptyList();
        long size = getUserSizeOfDataWhiteList(callAddress, contractAddress, dataIndex);
        String method = "getUserFromDataWhiteList(uint256,uint256)";
        for (long i = 0; i < size; i++) {
            List<Object> args = Arrays.asList(dataIndex, i);
            TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
            ByteString callResult = callReturn.getCallResult();
            String userAddress = Strings.fromByteArray(callResult.toByteArray()).trim();
            users.add(userAddress);
        }
        return users;
    }

    private long getUserSizeOfDataWhiteList(String callAddress, String contractAddress, long dataIndex) {
        String method = "getUserSizeOfDataWhiteList(uint256)";
        List<Object> args = Collections.singletonList(dataIndex);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        return ByteUtil.byteArrayToLong(callResult.toByteArray());
    }

    private long getUserSizeOfContractWhiteList(String callAddress, String contractAddress) {
        String method = "getUserSizeOfContractWhiteList()";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        return ByteUtil.byteArrayToLong(callResult.toByteArray());
    }

    private TriggerContractReturn triggerConstantContract(String callAddress, String contractAddress,
                                                          String method, List<Object> args) {
        return triggerContract(callAddress,null,contractAddress,method,args,true);
    }

    private TriggerContractReturn triggerContract(String callAddress, String privateKey, String contractAddress,
                                                  String method, List<Object> args) {
        return triggerContract(callAddress,privateKey,contractAddress,method,args,false);
    }

    private TriggerContractReturn triggerContract(String callAddress, String privateKey, String contractAddress,
                                                  String method, List<Object> args, boolean isConstant) {
        TriggerContractParam triggerContractParam = new TriggerContractParam()
                .setContractAddress(DecodeUtil.decode(contractAddress))
                .setCallValue(0)
                .setConstant(isConstant)
                .setArgs(args)
                .setTriggerMethod(method);

        TriggerContractReturn result = triggerContract(DecodeUtil.decode(callAddress),
                DecodeUtil.decode(privateKey), triggerContractParam);

        return result;
    }
}
