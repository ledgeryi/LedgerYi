package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.utils.ByteUtil;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.contract.compiler.exception.ContractException;
import cn.ledgeryi.sdk.event.CallTransaction;
import cn.ledgeryi.sdk.exception.AddressException;
import cn.ledgeryi.sdk.exception.CallContractExecption;
import cn.ledgeryi.sdk.exception.CreateContractExecption;
import cn.ledgeryi.sdk.serverapi.data.*;
import cn.ledgeryi.sdk.serverapi.data.stcc.ContractBaseInfo;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

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
     * @param args 合约参数：创建人、合约中文名称、合约英文名称
     */
    public DeployContractReturn deployWitnessContract(String ownerAddress, String privateKey, List<Object> args)
            throws CreateContractExecption, ContractException, AddressException {
        verifyAddress(ownerAddress);
        if (args.size() != 3){
            throw new CreateContractExecption("Data storage failed, data length is inconsistent");
        }
        Path source = Paths.get("src","main/resources/contract","Witness.sol");
        DeployContractParam param = compileContractFromFile(source,"Witness");
        param.setConstructor("constructor(string,string,string)");
        param.setArgs(args);
        return deployContract(DecodeUtil.decode(ownerAddress), DecodeUtil.decode(privateKey), param);
    }


    /**
     * 部署溯源代理合约
     * @param ownerAddress 部署者地址
     * @param privateKey 部署者私钥
     * @param args 合约参数：创建人、合约中文名称、合约英文名称、唯一溯源标识
     */
    public DeployContractReturn deployTracingProxyContract(String ownerAddress, String privateKey, List<Object> args)
            throws CreateContractExecption, ContractException, AddressException {
        verifyAddress(ownerAddress);
        if (args.size() != 4){
            throw new CreateContractExecption("Data storage failed, data length is inconsistent");
        }
        Path source = Paths.get("src","main/resources/contract","TracingProxy.sol");
        DeployContractParam param = compileContractFromFile(source,"TracingProxy");
        param.setConstructor("constructor(string,string,string,string)");
        param.setArgs(args);
        return deployContract(DecodeUtil.decode(ownerAddress), DecodeUtil.decode(privateKey), param);
    }

    /**
     * 部署溯源合约
     * @param ownerAddress 部署者地址
     * @param privateKey 部署者私钥
     * @param args 合约参数：唯一溯源标识、溯源登记信息组名称
     */
    public DeployContractReturn deployTracingContract(String ownerAddress, String privateKey, List<Object> args)
            throws CreateContractExecption, ContractException, AddressException {
        verifyAddress(ownerAddress);
        if (args.size() != 2){
            throw new CreateContractExecption("Data storage failed, data length is inconsistent");
        }
        Path source = Paths.get("src","main/resources/contract","Tracing.sol");
        DeployContractParam param = compileContractFromFile(source,"Tracing");
        param.setConstructor("constructor(string,string)");
        param.setArgs(args);
        return deployContract(DecodeUtil.decode(ownerAddress), DecodeUtil.decode(privateKey), param);
    }

    /**
     * 溯源合约基础信息：溯源登记信息组名称、唯一溯源标识、创建时间、合约拥有者地址
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public ContractBaseInfo getTracingBaseInfo(String callAddress, String contractAddress) throws AddressException {
        String method = "getBaseInfo()";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getBaseInfo",
                new String[]{}, new String[]{"string","string","uint", "address"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return ContractBaseInfo.builder()
                .groupName((String) objects[0])
                .uid((String) objects[1])
                .createTime(formatter.format(((BigInteger)objects[2]).multiply(new BigInteger("1000"))))
                .owner(DecodeUtil.createReadableString((byte[]) objects[3]))
                .build();
    }

    /**
     * 存证合约基础信息：合约创建者、合约英文名称、合约中文名称、创建时间、合约拥有者地址
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public ContractBaseInfo getWitnessBaseInfo(String callAddress, String contractAddress) throws AddressException {
        String method = "getBaseInfo()";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getBaseInfo",
                new String[]{}, new String[]{"string","string","string", "uint", "address"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return ContractBaseInfo.builder()
                .creator((String) objects[0])
                .nameEn((String) objects[1])
                .nameZn((String) objects[2])
                .createTime(formatter.format(((BigInteger)objects[3]).multiply(new BigInteger("1000"))))
                .owner(DecodeUtil.createReadableString((byte[]) objects[4]))
                .build();
    }

    /**
     * 存证合约基础信息：合约创建者、合约英文名称、合约中文名称、创建时间、合约拥有者地址
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public ContractBaseInfo getTracingProxyBaseInfo(String callAddress, String contractAddress) throws AddressException {
        String method = "getBaseInfo()";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getBaseInfo",
                new String[]{}, new String[]{"string","string","string", "string", "uint", "address"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return ContractBaseInfo.builder()
                .creator((String) objects[0])
                .nameEn((String) objects[1])
                .nameZn((String) objects[2])
                .uid((String) objects[3])
                .createTime(formatter.format(objects[4]))
                .owner(DecodeUtil.createReadableString((byte[]) objects[5]))
                .build();
    }

    /**
     * 存证合约、溯源合约：新增存证信息名称
     * @param callAddress 合约调用者
     * @param privateKey 调用者私钥
     * @param contractAddress 合约地址
     */
    public boolean addWitnessInfo(String callAddress, String privateKey,
                                  String contractAddress, List<Object> args) throws AddressException {
        String method = "addDataKey(string[])";
        List<Object> params = Collections.singletonList(JSONObject.toJSON(args));
        TriggerContractReturn triggerContractReturn = triggerContract(callAddress, privateKey, contractAddress, method, params);
        return triggerContractReturn != null;
    }

    /**
     * 存证合约、溯源合约：获取存证信息名称
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public List<String> getWitnessInfo(String callAddress, String contractAddress) throws AddressException {
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
     * 存证合约：数据上链
     * @param callAddress 合约调用者
     * @param privateKey 调用者私钥
     * @param contractAddress 合约地址
     * @param args 存证数据
     * @return 存证数据链上索引，从0开始
     * @throws CallContractExecption
     */
    public String saveDataInfo(String callAddress, String privateKey, String contractAddress, List<Object> args)
            throws CallContractExecption {
        try {
            String method = "saveDataInfo(string[])";
            List<Object> params = Collections.singletonList(JSONObject.toJSON(args));
            TriggerContractReturn triggerContractReturn = triggerContract(callAddress, privateKey, contractAddress, method, params);
            ByteString contractResult = triggerContractReturn.getCallResult();
            return String.valueOf(ByteUtil.byteArrayToLong(contractResult.toByteArray()));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CallContractExecption("Data storage failed, error: " + e.getMessage());
        }
    }

    /**
     * 存证合约：数据上链（对存证数据对齐验证）
     * @param callAddress 合约调用者
     * @param privateKey 调用者私钥
     * @param contractAddress 合约地址
     * @param args 存证数据
     * @return 存证数据链上索引，从0开始
     */
    public String saveDataInfo(String callAddress, String privateKey, String contractAddress, Map<String,String> args)
            throws CallContractExecption, AddressException {
        List<Object> params = new ArrayList<>();
        List<String> keys = getWitnessInfo(callAddress, contractAddress);
        if (args.size() != keys.size()){
            throw new CallContractExecption("Data storage failed, data length is inconsistent");
        }
        Set<String> keySets = args.keySet();
        for (String key : keySets) {
            if (keys.contains(key)) {
                params.add(args.get(key));
            } else {
                throw new CallContractExecption("Data storage failed, data keys not include key:" + key);
            }
        }
        return saveDataInfo(callAddress,privateKey,contractAddress,params);
    }

    /**
     * 溯源合约：数据上链
     * @param callAddress 合约调用者
     * @param privateKey 调用者私钥
     * @param contractAddress 合约地址
     * @param traceId 溯源ID
     * @param args 存证数据
     * @return 存证数据链上索引，从0开始
     * @throws CallContractExecption
     */
    public String saveDataInfo(String callAddress, String privateKey,
                               String contractAddress, String traceId,
                               List<Object> args) throws CallContractExecption {
        try {
            String method = "saveDataInfo(string,string[])";
            List<Object> params = Arrays.asList(traceId, JSONObject.toJSON(args));
            TriggerContractReturn triggerContractReturn = triggerContract(callAddress,
                    privateKey, contractAddress, method, params);
            ByteString contractResult = triggerContractReturn.getCallResult();
            return String.valueOf(ByteUtil.byteArrayToLong(contractResult.toByteArray()));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CallContractExecption("Data storage failed, error: " + e.getMessage());
        }
    }

    /**
     * 溯源合约：数据上链（对存证数据对齐验证）
     * @param callAddress 合约调用者
     * @param privateKey 调用者私钥
     * @param traceId 溯源ID
     * @param contractAddress 合约地址
     * @param args 存证数据
     * @return 存证数据链上索引，从0开始
     */
    public String saveDataInfo(String callAddress, String privateKey,
                               String contractAddress, String traceId,
                               Map<String,String> args) throws CallContractExecption, AddressException {
        List<Object> params = new ArrayList<>();
        List<String> keys = getWitnessInfo(callAddress, contractAddress);
        if (args.size() != keys.size()){
            throw new CallContractExecption("Data storage failed, data length is inconsistent");
        }
        Set<String> keySets = args.keySet();
        for (String key : keySets) {
            if (keys.contains(key)) {
                params.add(args.get(key));
            } else {
                throw new CallContractExecption("Data storage failed, data keys not include key:" + key);
            }
        }
        return saveDataInfo(callAddress,privateKey,contractAddress,traceId,params);
    }

    /**
     * 溯源合约：存证数据串改验证（需要验证权限）
     * @param callAddress 调用者，需要有数据获取权限
     * @param contractAddress 合约地址
     * @param traceId 溯源ID
     * @param dataVersion 被验证的数据版本（数据链上索引）
     * @param data 被验证的数据
     * @return 返回验证结果
     */
    public boolean traceDataVerify(String callAddress, String contractAddress,
                                     String traceId, long dataVersion, Map<String,String> data) throws AddressException {
        Map<String, String> dataInfo = getDataInfo(callAddress, contractAddress, traceId, dataVersion);
        return data != null && data.equals(dataInfo);
    }

    /**
     * 溯源合约：存证数据串改验证（不需要验证权限）
     * @param callAddress 调用者，不需要有数据获取权限
     * @param contractAddress 合约地址
     * @param traceId 溯源ID
     * @param dataVersion 被验证的数据版本（数据链上索引）
     * @param data 被验证的数据
     * @return 返回验证结果
     */
    public boolean traceDataVerifyPermissionless(String callAddress, String contractAddress,
                                   String traceId, long dataVersion, Map<String,String> data) throws CallContractExecption, AddressException {
        String method = "dataVerify(string,uint256,string[])";
        List<Object> params = new ArrayList<>();
        List<String> keys = getWitnessInfo(callAddress, contractAddress);
        if (data.size() != keys.size()){
            throw new CallContractExecption("Data storage failed, data length is inconsistent");
        }
        Set<String> keySets = data.keySet();
        for (String key : keySets) {
            if (keys.contains(key)) {
                params.add(data.get(key));
            } else {
                throw new CallContractExecption("Data storage failed, data keys not include key:" + key);
            }
        }
        List<Object> args = Arrays.asList(traceId,dataVersion,params);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("dataVerify",
                new String[]{"string","uint256","string[]"}, new String[]{"bool"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        if (objects.length == 1 && objects[0] instanceof Boolean) {
            return (Boolean)objects[0];
        }
        return false;
    }

    /**
     * 存证合约：存证数据串改验证（需要验证权限）
     * @param callAddress 调用者，需要有数据获取权限
     * @param contractAddress 合约地址
     * @param dataVersion 被验证的数据版本（数据链上索引）
     * @param data 被验证的数据
     * @return 返回验证结果
     */
    public boolean witnessDataVerify(String callAddress, String contractAddress,
                                     long dataVersion, Map<String,String> data) throws AddressException {
        Map<String, String> dataInfo = getDataInfo(callAddress, contractAddress, dataVersion);
        return data != null && data.equals(dataInfo);
    }

    /**
     * 存证合约：存证数据串改验证，不需要有数据获取权限
     * @param callAddress 调用者，不需要有数据获取权限
     * @param contractAddress 合约地址
     * @param dataVersion 被验证的数据版本（数据链上索引）
     * @param data 被验证的数据
     * @return 返回验证结果
     */
    public boolean witnessDataVerifyPermissionless(String callAddress, String contractAddress,
                                   long dataVersion, Map<String,String> data) throws CallContractExecption, AddressException {
        String method = "dataVerify(uint256,string[])";
        List<Object> params = new ArrayList<>();
        List<String> keys = getWitnessInfo(callAddress, contractAddress);
        if (data.size() != keys.size()){
            throw new CallContractExecption("Data storage failed, data length is inconsistent");
        }
        Set<String> keySets = data.keySet();
        for (String key : keySets) {
            if (keys.contains(key)) {
                params.add(data.get(key));
            } else {
                throw new CallContractExecption("Data storage failed, data keys not include key:" + key);
            }
        }
        List<Object> args = Arrays.asList(dataVersion,params);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("dataVerify",
                new String[]{"uint256","string[]"}, new String[]{"bool"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        if (objects.length == 1 && objects[0] instanceof Boolean) {
            return (Boolean)objects[0];
        }
        return false;
    }

    /**
     * 存证合约：获取最新的存证数据
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public Map<String,String> getLatestDataInfo(String callAddress, String contractAddress) throws AddressException {
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
     * 溯源合约：获取最新的存证数据
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     * @param traceId 溯源ID
     */
    public Map<String,String> getLatestDataInfo(String callAddress, String contractAddress,
                                                String traceId) throws AddressException {
        String method = "getDataInfo(string)";
        List<Object> args = Collections.singletonList(traceId);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getDataInfo",
                new String[]{"string"}, new String[]{"string[]","string[]"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        return dataIntegration(objects);
    }

    /**
     * 存证合约：获取某一个版本的存证数据
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     * @param dataIndex 数据对应的链上索引（数据版本号），从0开始
     */
    public Map<String,String> getDataInfo(String callAddress, String contractAddress,
                                          long dataIndex) throws AddressException {
        String method = "getDataInfo(uint256)";
        List<Object> args = Collections.singletonList(dataIndex);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getDataInfo",
                new String[]{"uint256"}, new String[]{"string[]","string[]"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        return dataIntegration(objects);
    }

    /**
     * 溯源合约：获取某一个版本的存证数据
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     * @param traceId 溯源ID
     * @param dataIndex 数据对应的链上索引（数据版本号），从0开始
     */
    public Map<String,String> getDataInfo(String callAddress, String contractAddress,
                                          String traceId, long dataIndex) throws AddressException {
        String method = "getDataInfo(string,uint256)";
        List<Object> args = Arrays.asList(traceId, dataIndex);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress, contractAddress, method, args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getDataInfo",
                new String[]{"string","uint256"}, new String[]{"string[]","string[]"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        return dataIntegration(objects);
    }

    /**
     * 存证合约、溯源合约：获取合约白名单启动状态
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     */
    public boolean getStatusOfContractWhite(String callAddress, String contractAddress) throws AddressException {
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
    public boolean disableStatusOfContractWhite(String callAddress, String privateKey,
                                                String contractAddress) throws AddressException {
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
    public boolean enableStatusOfContractWhite(String callAddress, String privateKey,
                                               String contractAddress) throws AddressException {
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
    public boolean addUserToContractWhiteList(String callAddress, String privateKey,
                                              String contractAddress, String user) throws AddressException {
        verifyAddress(user);
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
    public boolean removeUserToContractWhiteList(String callAddress, String privateKey,
                                                 String contractAddress, String user) throws AddressException {
        verifyAddress(user);
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
    public List<String> getUsersFromContractWhiteList(String callAddress,
                                                      String contractAddress) throws AddressException {
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
     * 存证合约：向某次存证数据白名单添加用户
     * @param callAddress 数据拥有者地址
     * @param privateKey 数据拥有者私钥
     * @param contractAddress 合约地址
     * @param dataIndex 数据索引（版本号）
     * @param user 被添加的用户
     */
    public boolean addUserToDataWhiteList(String callAddress, String privateKey,
                                          String contractAddress, long dataIndex, String user) throws AddressException {
        verifyAddress(user);
        String method = "addUserToDataWhiteList(uint256,address)";
        List<Object> args = Arrays.asList(dataIndex,user);
        TriggerContractReturn contractReturn= triggerContract(callAddress,privateKey,contractAddress,method,args);
        ByteString contractResult = contractReturn.getCallResult();
        return ByteUtil.bytesToBigInteger(contractResult.toByteArray()).equals(BigInteger.ONE);
    }

    /**
     * 溯源合约：向某次存证数据白名单添加用户
     * @param callAddress 数据拥有者地址
     * @param privateKey 数据拥有者私钥
     * @param contractAddress 合约地址
     * @param dataIndex 数据索引（版本号）
     * @param traceId 溯源ID
     * @param user 被添加的用户
     */
    public boolean addUserToDataWhiteList(String callAddress, String privateKey,
                                          String contractAddress, String traceId,
                                          long dataIndex, String user) throws AddressException {
        verifyAddress(user);
        String method = "addUserToDataWhiteList(string,uint256,address)";
        List<Object> args = Arrays.asList(traceId,dataIndex,user);
        TriggerContractReturn contractReturn= triggerContract(callAddress,privateKey,contractAddress,method,args);
        ByteString contractResult = contractReturn.getCallResult();
        return ByteUtil.bytesToBigInteger(contractResult.toByteArray()).equals(BigInteger.ONE);
    }

    /**
     * 存证合约：向某次存证数据白名单移除用户
     * @param callAddress 数据拥有者地址
     * @param privateKey 数据拥有者私钥
     * @param contractAddress 合约地址
     * @param dataIndex 数据索引（版本号）
     * @param user 被移除的用户
     */
    public boolean removeUserToDataWhiteList(String callAddress, String privateKey,
                                             String contractAddress,long dataIndex, String user) throws AddressException {
        verifyAddress(user);
        String method = "removeUserFromDataWhiteList(uint256,address)";
        List<Object> args = Arrays.asList(dataIndex,user);
        TriggerContractReturn triggerContractReturn = triggerContract(callAddress,privateKey,contractAddress,method,args);
        ByteString contractResult = triggerContractReturn.getCallResult();
        return ByteUtil.bytesToBigInteger(contractResult.toByteArray()).equals(BigInteger.ONE);
    }

    /**
     * 溯源合约：向某次存证数据白名单移除用户
     * @param callAddress 数据拥有者地址
     * @param privateKey 数据拥有者私钥
     * @param contractAddress 合约地址
     * @param dataIndex 数据索引（版本号）
     * @param traceId 溯源ID
     * @param user 被移除的用户
     */
    public boolean removeUserToDataWhiteList(String callAddress, String privateKey,
                                             String contractAddress,String traceId,
                                             long dataIndex, String user) throws AddressException {
        verifyAddress(user);
        String method = "removeUserFromDataWhiteList(string,uint256,address)";
        List<Object> args = Arrays.asList(traceId,dataIndex,user);
        TriggerContractReturn triggerContractReturn = triggerContract(callAddress,privateKey,contractAddress,method,args);
        ByteString contractResult = triggerContractReturn.getCallResult();
        return ByteUtil.bytesToBigInteger(contractResult.toByteArray()).equals(BigInteger.ONE);
    }

    /**
     * 存证合约：获取某次存证数据白名单成员
     * @param callAddress 调用者地址
     * @param contractAddress 合约地址
     * @param dataIndex 数据索引（版本号）
     */
    public List<String> getUsersFromDataWhiteList(String callAddress, String contractAddress,
                                                  long dataIndex) throws AddressException {
        List<String> users = new ArrayList<>();
        String method = "getUserSizeOfDataWhiteList(uint256)";
        List<Object> params = Collections.singletonList(dataIndex);
        long size = getSize(callAddress,contractAddress,method,params);

        method = "getUserFromDataWhiteList(uint256,uint256)";
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
     * 溯源合约：获取某次存证数据白名单成员
     * @param callAddress 调用者地址
     * @param contractAddress 合约地址
     * @param traceId 溯源ID
     * @param dataIndex 数据索引（版本号）
     */
    public List<String> getUsersFromDataWhiteList(String callAddress, String contractAddress,
                                                  String traceId, long dataIndex) throws AddressException {
        List<String> users = new ArrayList<>();
        String method = "getUserSizeOfDataWhiteList(string,uint256)";
        List<Object> params = Arrays.asList(traceId,dataIndex);
        long size = getSize(callAddress,contractAddress,method,params);

        method = "getUserFromDataWhiteList(string,uint256,uint256)";
        for (long i = 0; i < size; i++) {
            List<Object> args = Arrays.asList(traceId, dataIndex, i);
            TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
            ByteString callResult = callReturn.getCallResult();
            String userAddress = DecodeUtil.createReadableString(callResult.toByteArray());
            users.add(userAddress.substring(24,64));
        }
        return users;
    }

    /**
     * 存证合约：获取存证数据白名单启动状态
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     * @param dataIndex 数据索引（版本号）
     */
    public boolean getStatusOfDataWhite(String callAddress, String contractAddress,
                                        long dataIndex) throws AddressException {
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
     * 溯源合约：获取存证数据白名单启动状态
     * @param callAddress 合约调用者
     * @param contractAddress 合约地址
     * @param traceId 溯源ID
     * @param dataIndex 数据索引（版本号）
     */
    public boolean getStatusOfDataWhite(String callAddress, String contractAddress,
                                        String traceId, long dataIndex) throws AddressException {
        String method = "getStatusOfDataWhite(string,uint256)";
        List<Object> args = Arrays.asList(traceId, dataIndex);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("getStatusOfDataWhite",
                new String[]{"string"}, new String[]{"bool"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        if (objects.length == 1 && objects[0] instanceof Boolean) {
            return (Boolean)objects[0];
        }
        return false;
    }

    /**
     * 存证合约：禁用存证数据白名单
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     * @param dataIndex 数据索引（版本号）
     */
    public boolean disableStatusOfDataWhite(String callAddress, String privateKey,
                                            String contractAddress, long dataIndex) throws AddressException {
        String method = "disableDataWhite(uint256)";
        List<Object> args = Collections.singletonList(dataIndex);
        return null == triggerContract(callAddress,privateKey,contractAddress,method,args);
    }

    /**
     * 溯源合约：禁用存证数据白名单
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     * @param traceId 溯源ID
     * @param dataIndex 数据索引（版本号）
     */
    public boolean disableStatusOfDataWhite(String callAddress, String privateKey, String contractAddress,
                                            String traceId, long dataIndex) throws AddressException {
        String method = "disableDataWhite(string,uint256)";
        List<Object> args = Arrays.asList(traceId, dataIndex);
        return null == triggerContract(callAddress,privateKey,contractAddress,method,args);
    }

    /**
     * 存证合约：启用存证数据白名单
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     * @param dataIndex 数据索引（版本号）
     */
    public boolean enableStatusOfDataWhite(String callAddress, String privateKey,
                                           String contractAddress, long dataIndex) throws AddressException {
        String method = "enableDataWhite(uint256)";
        List<Object> args = Collections.singletonList(dataIndex);
        return null != triggerContract(callAddress,privateKey,contractAddress,method,args);
    }

    /**
     * 溯源合约：禁用存证数据白名单
     * @param callAddress 合约拥有者
     * @param privateKey 合约拥有者私钥
     * @param contractAddress 合约地址
     * @param traceId 溯源ID
     * @param dataIndex 数据索引（版本号）
     */
    public boolean enableStatusOfDataWhite(String callAddress, String privateKey, String contractAddress,
                                           String traceId, long dataIndex) throws AddressException {
        String method = "enableDataWhite(string,uint256)";
        List<Object> args = Arrays.asList(traceId, dataIndex);
        return null != triggerContract(callAddress,privateKey,contractAddress,method,args);
    }

    /**
     * 溯源代理合约：新增溯源环节
     * @param callAddress 调用者地址
     * @param privateKey 调用者私钥
     * @param proxyContractAddress 代理合约地址
     * @param args 溯源环节名称
     */
    public boolean addTraceLink(String callAddress, String privateKey,
                                String proxyContractAddress, List<Object> args) throws AddressException {
        String method = "addTraceLink(string[])";
        return null != triggerContract(callAddress,privateKey,proxyContractAddress,method,args);
    }

    /**
     * 溯源代理合约：向某一溯源环节中新增溯源合约
     * @param callAddress 调用者地址
     * @param privateKey 调用者私钥
     * @param proxyContractAddress 代理合约地址
     * @param linkName 溯源环节名称
     * @param traceContract 溯源合约
     */
    public boolean addContractToTraceLink(String callAddress, String privateKey,
                                          String proxyContractAddress, String linkName,
                                          String traceContract) throws AddressException {
        String method = "addContractToTraceLink(string,address)";
        List<Object> args = Arrays.asList(linkName,traceContract);
        triggerContract(callAddress,privateKey,proxyContractAddress,method,args);
        TriggerContractReturn callReturn = triggerContract(callAddress,privateKey,proxyContractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        CallTransaction.Function function = CallTransaction.Function.fromSignature("addContractToTraceLink",
                new String[]{}, new String[]{"bool"});
        Object[] objects = function.decodeResult(callResult.toByteArray());
        if (objects.length == 1 && objects[0] instanceof Boolean) {
            return (Boolean)objects[0];
        }
        return false;
    }

    /**
     * 溯源代理合约：获取环节名称
     * @param callAddress  调用者地址
     * @param proxyContractAddress 代理合约地址
     */
    public ArrayList<String> getTraceLinkNames(String callAddress, String proxyContractAddress) throws AddressException {
        String method = "getTraceLinkLength()";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,proxyContractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        long traceLinkNameSize = ByteUtil.byteArrayToLong(callResult.toByteArray());
        method = "getTraceLinkName(uint256)";
        ArrayList<String> linkNames = new ArrayList<>();
        for (long dataIndex = 0; dataIndex < traceLinkNameSize; dataIndex++) {
            args = Collections.singletonList(dataIndex);
            callReturn = triggerConstantContract(callAddress,proxyContractAddress,method,args);
            String linkName = new String(callReturn.getCallResult().toByteArray()).trim();
            linkNames.add(linkName);
        }
        return linkNames;
    }

    /**
     * 溯源代理合约：某一个溯源环节包含的溯源合约总数
     * @param callAddress  调用者地址
     * @param proxyContractAddress 代理合约地址
     * @param linkName 溯源环节名称
     */
    public long getTraceContractSize(String callAddress, String proxyContractAddress, String linkName) {
        String method = "getTraceContractSize(string)";
        List<Object> args = Collections.singletonList(linkName);
        return getSize(callAddress,proxyContractAddress,method,args);
    }

    /**
     * 溯源代理合约：从某一个溯源环节中获取某一个溯源合约
     * @param callAddress 调用者地址
     * @param proxyContractAddress 代理合约地址
     * @param linkName 溯源环节名称
     * @param contractIndex 合约索引，从0开始
     * @return 溯源合约地址
     */
    public String getContractInTraceLink(String callAddress, String proxyContractAddress,
                                              String linkName, long contractIndex) throws AddressException {
        String method = "getTraceContractAddress(string,uint256)";
        List<Object> args = Arrays.asList(linkName,contractIndex);
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,proxyContractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        String address = DecodeUtil.createReadableString(callResult.toByteArray());
        return address.substring(24,64);
    }

    /**
     * 溯源代理合约：从某一个溯源环节中获取所有溯源合约
     * @param callAddress 调用者地址
     * @param proxyContractAddress 代理合约地址
     * @param linkName 溯源环节名称
     */
    public List<String> getAllContactsOfTraceLink(String callAddress, String proxyContractAddress,
                                                  String linkName) throws AddressException {
        ArrayList<String> contracts = new ArrayList<>();
        long contractSize = getTraceContractSize(callAddress, proxyContractAddress, linkName);
        for (long index = 0; index < contractSize; index++) {
            String contract = getContractInTraceLink(callAddress, proxyContractAddress, linkName, index);
            contracts.add(contract);
        }
        return contracts;
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

    private void verifyAddress(String ...address) throws AddressException {
        String[] addresses = address.clone();
        for (String tmp : addresses) {
            if (!DecodeUtil.validAddress(tmp)) {
                throw new AddressException("Invalid address, address: " + tmp);
            }
        }
    }

    private long getSize(String callAddress, String contractAddress, String method, List<Object> args) {
        try {
            TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
            ByteString callResult = callReturn.getCallResult();
            return ByteUtil.byteArrayToLong(callResult.toByteArray());
        } catch (Exception e) {
            return 0L;
        }
    }

    private long getUserSizeOfContractWhiteList(String callAddress, String contractAddress) throws AddressException {
        String method = "getUserSizeOfContractWhiteList()";
        List<Object> args = Collections.emptyList();
        TriggerContractReturn callReturn = triggerConstantContract(callAddress,contractAddress,method,args);
        ByteString callResult = callReturn.getCallResult();
        return ByteUtil.byteArrayToLong(callResult.toByteArray());
    }

    private TriggerContractReturn triggerConstantContract(String callAddress, String contractAddress,
                                                          String method, List<Object> args) throws AddressException {
        return triggerContract(callAddress,null,contractAddress,method,args,true);
    }

    private TriggerContractReturn triggerContract(String callAddress, String privateKey, String contractAddress,
                                                  String method, List<Object> args) throws AddressException {
        return triggerContract(callAddress,privateKey,contractAddress,method,args,false);
    }

    private TriggerContractReturn triggerContract(String callAddress, String privateKey, String contractAddress,
                                                  String method, List<Object> args, boolean isConstant) throws AddressException {
        verifyAddress(callAddress, callAddress);
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

    /**
     * 查询指定区块的的信息
     * @param number 区块高度
     * @return 区块信息
     */
    public BlockInfo getBlockInfo(long number) {
        BlockInfo.BlockInfoBuilder blockInfoBuilder = BlockInfo.builder();
        GrpcAPI.BlockExtention block = super.getBlock(number);
        if (block == null) {
           return blockInfoBuilder.build();
        }
        return blockInfoBuilder
                .hash(DecodeUtil.encode(block.getBlockid()))
                .number(block.getBlockHeader().getRawData().getNumber())
                .parentHash(DecodeUtil.encode(block.getBlockHeader().getRawData().getParentHash()))
                .txSize(block.getTransactionsCount())
                .timestamp(block.getBlockHeader().getRawData().getTimestamp())
                .size(block.getSerializedSize())
                .build();
    }

    /**
     * 获取指定范围的区块信息
     * @param start 起始区块高度，包括
     * @param end 结束区块高度，不包括
     * @return 区块信息集合
     */
    public List<BlockInfo> getBlocksInfo(long start, long end) {
        ArrayList<BlockInfo> blocksInfo = new ArrayList<>();
        GrpcAPI.BlockListExtention blocks = super.getBlockByLimitNext(start, end);
        blocks.getBlockList().forEach(
                block -> blocksInfo.add(BlockInfo.builder()
                        .hash(DecodeUtil.encode(block.getBlockid()))
                        .number(block.getBlockHeader().getRawData().getNumber())
                        .parentHash(DecodeUtil.encode(block.getBlockHeader().getRawData().getParentHash()))
                        .txSize(block.getTransactionsCount())
                        .timestamp(block.getBlockHeader().getRawData().getTimestamp())
                        .size(block.getSerializedSize())
                        .build())
        );
        return blocksInfo;
    }
}
