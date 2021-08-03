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

import java.math.BigInteger;
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
    public DeployContractReturn deployTracingProxyContract(String ownerAddress, String privateKey, List<Object> args) {
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
    public DeployContractReturn deployTracingContract(String ownerAddress, String privateKey, List<Object> args) {
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
     * 溯源合约基础信息：创建者、溯源登记信息组名称、唯一溯源标识、创建时间、合约拥有者地址
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public ContractBaseInfo getTracingBaseInfo(String callAddress, String contractAddress) {
        Object[] objects = getBaseInfo(callAddress,contractAddress);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return ContractBaseInfo.builder()
                .creator((String) objects[0])
                .groupName((String) objects[1])
                .uid((String) objects[2])
                .createTime(formatter.format(objects[3]))
                .owner(DecodeUtil.createReadableString((byte[]) objects[4]))
                .build();
    }

    /**
     * 存证合约基础信息：合约创建者、合约英文名称、合约中文名称、创建时间、合约拥有者地址
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public ContractBaseInfo getWitnessBaseInfo(String callAddress, String contractAddress) {
        Object[] objects = getBaseInfo(callAddress,contractAddress);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return ContractBaseInfo.builder()
                .creator((String) objects[0])
                .nameEn((String) objects[1])
                .nameZn((String) objects[2])
                .createTime(formatter.format(objects[3]))
                .owner(DecodeUtil.createReadableString((byte[]) objects[4]))
                .build();
    }

    private Object[] getBaseInfo(String callAddress, String contractAddress) {
        String method = "getBaseInfo()";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getBaseInfo",
                new String[]{}, new String[]{"string","string","string", "uint", "address"});
        return function.decodeResult(callResult.toByteArray());
    }

    /**
     * 存证合约、溯源合约：新增存证信息名称
     * @param callAddress 合约调用者
     * @param privateKey 调用者私钥
     * @param contractAddress 合约地址
     * @return 数据对应的链上索引，从0开始
     */
    public boolean addWitnessInfo(String callAddress, String privateKey, String contractAddress, List<Object> args) {
        String method = "addDataKey(string[])";
        TriggerContractReturn triggerContractReturn =
                triggerContract(callAddress, privateKey, contractAddress, method, args);
        ByteString contractResult = triggerContractReturn.getCallResult();
        return ByteUtil.byteArrayToLong(contractResult.toByteArray()) == 0;
    }

    /**
     * 存证合约、溯源合约：获取存证信息名称
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
                Object[] temp = (Object[])object;
                ArrayList<String> arrayList = new ArrayList<>(temp.length);
                for (Object o : temp) {
                    arrayList.add((String)o);
                }
                return arrayList;
            }
        }
        return Collections.emptyList();

    }

    /**
     * 存证合约、溯源合约：数据上链
     * @param callAddress 合约调用者
     * @param privateKey 调用者私钥
     * @param contractAddress 合约地址
     * @param args 存证数据
     * @return 存证数据链上索引
     */
    public long saveDataInfo(String callAddress, String privateKey, String contractAddress, List<Object> args) {
        String method = "saveDataInfo(string[])";
        TriggerContractReturn triggerContractReturn =
                triggerContract(callAddress, privateKey, contractAddress, method, args);
        ByteString contractResult = triggerContractReturn.getCallResult();
        return ByteUtil.byteArrayToLong(contractResult.toByteArray());
    }

    /**
     * 存证合约、溯源合约：获取最新的存证数据
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public Map<String,String> getLatestDataInfo(String callAddress, String contractAddress) {
        String method = "getDataInfo()";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getDataInfo",
                new String[]{}, new String[]{"string[]","string[]"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        return dataIntegration(objects);
    }

    /**
     * 存证合约、溯源合约：获取存证数据
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     * @param dataIndex 数据对应的链上索引，从0开始
     */
    public Map<String,String> getDataInfo(String callAddress, String contractAddress, long dataIndex) {
        String method = "getDataInfo(uint256)";
        List<Object> args = Collections.singletonList(dataIndex);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getDataInfo",
                new String[]{"uint256"}, new String[]{"string[]","string[]"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        return dataIntegration(objects);
    }

    private Map<String,String> dataIntegration(Object[] objects){
        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        if (objects.length == 2) {
            if (objects[0] instanceof Object[]) {
                Object[] temp = (Object[])objects[0];
                for (Object o : temp) {
                    keys.add((String)o);
                }
            }
            if (objects[1] instanceof Object[]) {
                Object[] temp = (Object[])objects[1];
                for (Object o : temp) {
                    values.add((String)o);
                }
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
     * 存证合约、溯源合约：获取合约白名单启动状态
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
     * 存证合约、溯源合约：禁用合约白名单
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
     * 存证合约、溯源合约：启用合约白名单
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     */
    public boolean enableStatusOfContractWhite(String callAddress, String privateKey, String contractAddress) {
        String method = "enableContractWhite()";
        List<Object> args = Collections.emptyList();
        return null != triggerContract(callAddress,privateKey,contractAddress,method,args);
    }

    /**
     * 存证合约、溯源合约：向合约白名单添加用户
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     * @param user 被添加的用户地址
     * @return
     */
    public boolean addUserToContractWhiteList(String callAddress, String privateKey, String contractAddress, String user) {
        String method = "addUserToContractWhiteList(address)";
        List<Object> args = Collections.singletonList(user);
        TriggerContractReturn contractReturn = triggerContract(callAddress, privateKey, contractAddress, method, args);
        ByteString contractResult = contractReturn.getCallResult();
        return ByteUtil.bytesToBigInteger(contractResult.toByteArray()).equals(BigInteger.ONE);
    }

    /**
     * 存证合约、溯源合约：从合约白名单移除用户
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     * @param user 被移除的用户地址
     */
    public boolean removeUserToContractWhiteList(String callAddress, String privateKey, String contractAddress, String user) {
        String method = "removeUserFromContractWhiteList(address)";
        List<Object> args = Collections.singletonList(user);
        TriggerContractReturn contractReturn = triggerContract(callAddress,privateKey,contractAddress,method,args);
        ByteString contractResult = contractReturn.getCallResult();
        return ByteUtil.bytesToBigInteger(contractResult.toByteArray()).equals(BigInteger.ONE);
}

    /**
     * 存证合约、溯源合约：获取合约白名单成员
     * @param callAddress 调用者地址
     * @param contractAddress 合约地址
     */
    public List<String> getUsersFromContractWhiteList(String callAddress, String contractAddress) {
        List<String> users = new ArrayList<>();
        long size = getUserSizeOfContractWhiteList(callAddress, contractAddress);
        String method = "getUserFromContractWhiteList(uint256)";
        for (long i = 0; i < size; i++) {
            List<Object> args = Collections.singletonList(i);
            TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
            ByteString callResult = callReturn.getCallResult();
            String userAddress = DecodeUtil.createReadableString(callResult.toByteArray());
            users.add(userAddress.substring(24,64));
        }
        return users;
    }

    /**
     * 存证合约、溯源合约：向某次存证数据白名单添加用户
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
        TriggerContractReturn contractReturn= triggerContract(callAddress,privateKey,contractAddress,method,args);
        ByteString contractResult = contractReturn.getCallResult();
        return ByteUtil.bytesToBigInteger(contractResult.toByteArray()).equals(BigInteger.ONE);

    }

    /**
     * 存证合约、溯源合约：向某次存证数据白名单移除用户
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
        TriggerContractReturn triggerContractReturn = triggerContract(callAddress,privateKey,contractAddress,method,args);
        ByteString contractResult = triggerContractReturn.getCallResult();
        return ByteUtil.bytesToBigInteger(contractResult.toByteArray()).equals(BigInteger.ONE);
    }

    /**
     * 存证合约、溯源合约：获取某次存证数据白名单成员
     * @param callAddress 调用者地址
     * @param contractAddress 合约地址
     */
    public List<String> getUsersFromDataWhiteList(String callAddress, String contractAddress, long dataIndex) {
        List<String> users = new ArrayList<>();
        long size = getUserSizeOfDataWhiteList(callAddress, contractAddress, dataIndex);
        String method = "getUserFromDataWhiteList(uint256,uint256)";
        for (long i = 0; i < size; i++) {
            List<Object> args = Arrays.asList(dataIndex, i);
            TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
            ByteString callResult = callReturn.getCallResult();
            String userAddress = DecodeUtil.createReadableString(callResult.toByteArray());
            users.add(userAddress.substring(24,64));
        }
        return users;
    }

    /**
     * 存证合约、溯源合约：获取存证数据白名单启动状态
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public boolean getStatusOfDataWhite(String callAddress, String contractAddress, long dataIndex) {
        String method = "getStatusOfDataWhite(uint256)";
        List<Object> args = Collections.singletonList(dataIndex);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getStatusOfDataWhite",
                new String[]{}, new String[]{"bool"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        if (objects.length == 1 && objects[0] instanceof Boolean) {
            return (Boolean)objects[0];
        }
        return false;
    }

    /**
     * 存证合约、溯源合约：禁用存证数据白名单
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     */
    public boolean disableStatusOfDataWhite(String callAddress, String privateKey, String contractAddress, long dataIndex) {
        String method = "disableDataWhite(uint256)";
        List<Object> args = Collections.singletonList(dataIndex);
        return null == triggerContract(callAddress,privateKey,contractAddress,method,args);
    }

    /**
     * 存证合约、溯源合约：启用存证数据白名单
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     */
    public boolean enableStatusOfDataWhite(String callAddress, String privateKey, String contractAddress, long dataIndex) {
        String method = "enableDataWhite(uint256)";
        List<Object> args = Collections.singletonList(dataIndex);
        return null != triggerContract(callAddress,privateKey,contractAddress,method,args);
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
