package cn.ledgeryi.sdk.demo;

import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.ecdsa.ECKey;
import cn.ledgeryi.crypto.sm2.SM2;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.AbiUtil;
import cn.ledgeryi.sdk.common.utils.Utils;
import cn.ledgeryi.sdk.config.Configuration;
import cn.ledgeryi.sdk.execption.CipherException;
import cn.ledgeryi.sdk.serverapi.RequestNodeApi;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContractTest {

    private static String privateKey = "e8b5177d5a69898dcc437d0e96a9343a37bac001cb9bec7a90b660eee66b4587";
    private static String address = "ada95a8734256b797efcd862e0b208529283ac56";


    @Before
    public void createOwner(){
        SignInterface signEngine = createSignEngine();
        if (StringUtils.isEmpty(privateKey)){
            privateKey = DecodeUtil.createReadableString(signEngine.getPrivateKey());
            System.out.println("privateKey: " + privateKey);
        }
        if (StringUtils.isEmpty(address)){
            address = DecodeUtil.createReadableString(signEngine.getAddress());
            System.out.println("address: " + address);
        }
    }

    @Test
    public void createAddressAndPrivateKey(){
        SignInterface signEngine = createSignEngine();
        byte[] address = signEngine.getAddress();
        System.out.println("AddressStr: " + DecodeUtil.createReadableString(address));
        System.out.println("privateStr: " + DecodeUtil.createReadableString(signEngine.getPrivateKey()));
        if (!Arrays.equals(address, DecodeUtil.decode(DecodeUtil.createReadableString(address)))) {
            System.out.println("address decode error!");
        }
        if (!Arrays.equals(signEngine.getPrivateKey(),
                DecodeUtil.decode(DecodeUtil.createReadableString(signEngine.getPrivateKey())))) {
            System.out.println("private key decode error!");
        }
    }

    private SignInterface createSignEngine(){
        if (Configuration.isEckey()) {
            return new ECKey();
        } else {
            return new SM2();
        }
    }

    @Test
    public void deployContract() {
        String input = address + " Storage [{\"inputs\":[],\"name\":\"retrieve\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"num\",\"type\":\"uint256\"}],\"name\":\"store\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}] 608060405234801561001057600080fd5b5060c78061001f6000396000f3fe6080604052348015600f57600080fd5b506004361060325760003560e01c80632e64cec11460375780636057361d146053575b600080fd5b603d607e565b6040518082815260200191505060405180910390f35b607c60048036036020811015606757600080fd5b81019080803590602001909291905050506087565b005b60008054905090565b806000819055505056fea2646970667358221220e0d62b2700e3afba6e87729d482239a5322dc2bdc290f9b75029586f2e2b115864736f6c63430007040033 # # false 100000 75 0";
        String[] parameter = input.split(" ");
        boolean result = deployContract(parameter);
        System.out.println("deploy contract result: " + result);
    }

    private static String contractAddres = "9c4e74e9558d497c71b5d58b84d6e38653763974";

    @Test
    public void getContract(){
        SmartContractOuterClass.SmartContract contract = RequestNodeApi.getContract(DecodeUtil.decode(contractAddres));
        System.out.println(Utils.printSmartContract(contract));
    }

    @Test
    public void storage() {
        String args = "7";
        String params = address + " " + contractAddres + " store(uint256) " + args + " false 0 0";
        String[] parameters = params.split(" ");
        /**
         * 【1】 constant function和非constant function 函数调用从对链上属性是否有更改可分为两种：constant function 和 非constant function。
         * Constant function 是指用 view/pure/constant 修饰的函数。会在调用的节点上直接返回结果，并不以一笔交易的形式广播出去。
         * 非constant function是指需要依托一笔交易的形式被广播的方法调用。函数会改变链上数据的内容，比如转账，改变合约内部变量的值等等。
         * 注意: 如果在合约内部使用create指令（CREATE instruction），即使用view/pure/constant来修饰这个动态创建的合约合约方法，
         * 这个合约方法仍会被当作非constant function，以交易的形式来处理。
         */
        triggerContract(parameters,false);
    }

    @Test
    public void retrieve() {
        String params = address + " " + contractAddres + " retrieve() # false";
        String[] parameters = params.split(" ");
        triggerContract(parameters,true);
    }

    /**
     *
     * @param parameters: "TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz TQiGeuGLriLFJozjChi3WLznpsop4gtXgz retrieve # false"
     * @param isConstant true:调用常量合约，产生的交易不上链；false：调用非常量合约，产生的交易会上链
     * @throws IOException
     * @throws CipherException
     */
    private void triggerContract(String[] parameters, boolean isConstant) {
        String cmdMethodStr = isConstant ? "TriggerConstantContract" : "TriggerContract";

        /**
         * ownerAddress contractAddrStr methodStr argsStr isHex feeLimit callValue tokenCallValue tokenId
         */
        int index = 0;
        byte[] ownerAddress = null;
        if (parameters.length == 5 || parameters.length == 7) {
            ownerAddress = DecodeUtil.decode(parameters[index++]);
            if (ownerAddress == null) {
                System.out.println("Invalid OwnerAddress.");
                return;
            }
        }

        String contractAddrStr = parameters[index++];
        String methodStr = parameters[index++];
        String argsStr = parameters[index++];
        boolean isHex = Boolean.valueOf(parameters[index++]);
        long feeLimit = 0;
        long callValue = 0;

        if (!isConstant) {
            feeLimit = Long.valueOf(parameters[index++]);
            callValue = Long.valueOf(parameters[index++]);
        }
        if ("#".equalsIgnoreCase(argsStr)) {
            argsStr = "";
        }
        byte[] input = Hex.decode(AbiUtil.parseMethod(methodStr, argsStr, isHex));
        byte[] contractAddress = DecodeUtil.decode(contractAddrStr);

        boolean result = RequestNodeApi.triggerContract(ownerAddress, contractAddress, callValue,
                input, feeLimit, isConstant, DecodeUtil.decode(privateKey));
        if (!isConstant) {
            if (result) {
                System.out.println("Broadcast the " + cmdMethodStr + " successful.\n"
                        + "Please check the given transaction id to get the result on "
                        + "blockchain using getTransactionInfoById command");
            } else {
                System.out.println("Broadcast the " + cmdMethodStr + " failed");
            }
        }
    }

    private boolean deployContract(String[] parameter) {
        String[] parameters = getParas(parameter);
        if (parameters == null || parameters.length < 11) {
            System.out.println("Using deployContract needs at least 11 parameters like: ");
        }
        int idx = 0;
        String address = parameters[idx];
        System.out.println("=======address: " + address);
        byte[] ownerAddress = getAddressBytes(address);
        if (ownerAddress != null) {
            idx++;
        }

        String contractName = parameters[idx++];
        System.out.println("=======contractName: " + contractName);
        String abiStr = parameters[idx++];
        System.out.println("=======abiStr: " + abiStr);
        String codeStr = parameters[idx++];
        System.out.println("=======codeStr: " + codeStr);
        String constructorStr = parameters[idx++];
        System.out.println("=======constructorStr: " + constructorStr);
        String argsStr = parameters[idx++];
        System.out.println("=======argsStr: " + argsStr);
        String hex = parameters[idx++];
        System.out.println("=======hex: " + hex);
        boolean isHex = Boolean.parseBoolean(hex);
        String free = parameters[idx++];
        System.out.println("=======free: " + free);
        long feeLimit = Long.parseLong(free);
        long consumeUserResourcePercent = Long.parseLong(parameters[idx++]);
        if (consumeUserResourcePercent > 100 || consumeUserResourcePercent < 0) {
            System.out.println("consume_user_resource_percent should be >= 0 and <= 100");
            return false;
        }
        if (!constructorStr.equals("#")) {
            if (isHex) {
                codeStr += argsStr;
            } else {
                codeStr += Hex.toHexString(AbiUtil.encodeInput(constructorStr, argsStr));
            }
            System.out.println("=======codeStr: " + codeStr);
        }
        long value;
        value = Long.valueOf(parameters[idx++]);
        System.out.println("=======callValue: " + value);

        /**
         * origin_address: 合约创建者地址
         * contract_address: 合约地址
         * abi:合约所有函数的接口信息
         * bytecode：合约字节码
         * call_value：随合约调用传入的trx金额
         * consume_user_resource_percent：开发者设置的调用者的资源扣费百分比
         * name：合约名称
         */
        boolean result = RequestNodeApi.deployContract(ownerAddress, contractName, abiStr, codeStr, feeLimit, value,
                consumeUserResourcePercent, DecodeUtil.decode(privateKey));
        if (result) {
            System.out.println("Broadcast the createSmartContract successful.\n"
                    + "Please check the given transaction id to confirm deploy status on blockchain using getTransactionInfoById command.");
        } else {
            System.out.println("Broadcast the createSmartContract failed !!!");
        }
        return result;
    }

    private String[] getParas(String[] para) {
        String paras = String.join(" ", para);
        Pattern pattern = Pattern.compile(" (\\[.*?\\]) ");
        Matcher matcher = pattern.matcher(paras);

        if (matcher.find()) {
            String ABI = matcher.group(1);
            List<String> tempList = new ArrayList<String>();

            paras = paras.replaceAll("(\\[.*?\\]) ", "");

            String[] parts = paras.split(" ");
            int abiIndex = 1;
            if (getAddressBytes(parts[0]) != null) {
                abiIndex = 2;
            }

            for (int i = 0; i < parts.length; i++) {
                if (abiIndex == i) {
                    tempList.add(ABI);
                }
                tempList.add(parts[i]);
            }
            return tempList.toArray(new String[0]);

        } else {
            return null;
        }
    }

    private byte[] getAddressBytes(final String address) {
        return DecodeUtil.decode(address);
    }
}
