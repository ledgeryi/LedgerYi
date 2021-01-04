package cn.ledgeryi.chainbase.common.utils;

import cn.ledgeryi.chainbase.core.capsule.ContractCapsule;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.crypto.utils.Hash;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.CreateSmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import com.google.common.primitives.Longs;

import java.util.Arrays;

public class ContractUtils {

    /**
     * create contract address
     */
    public static byte[] generateContractAddress(Protocol.Transaction tx) {
        CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(tx);
        byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
        TransactionCapsule trxCap = new TransactionCapsule(tx);
        byte[] txRawDataHash = trxCap.getTransactionId().getBytes();
        byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
        System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
        System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);
        return Hash.sha3omit12(combined);
    }

    // for `CREATE2` opcode
    public static byte[] generateContractAddress2(byte[] address, byte[] salt, byte[] code) {
        byte[] mergedData = ByteUtil.merge(address, salt, Hash.sha3(code));
        return Hash.sha3omit12(mergedData);
    }

    // for `CREATE` opcode
    public static byte[] generateContractAddress(byte[] transactionRootId, long nonce) {
        byte[] nonceBytes = Longs.toByteArray(nonce);
        byte[] combined = new byte[transactionRootId.length + nonceBytes.length];
        System.arraycopy(transactionRootId, 0, combined, 0, transactionRootId.length);
        System.arraycopy(nonceBytes, 0, combined, transactionRootId.length, nonceBytes.length);
        return Hash.sha3omit12(combined);
    }

    public static boolean isConstant(ABI abi, TriggerSmartContract triggerSmartContract) {
        boolean constant = isConstant(abi, getSelector(triggerSmartContract.getData().toByteArray()));
        if (constant && !DBConfig.isSupportConstant()) {
            return false;
        }
        return true;
    }

    public static boolean isConstant(ABI abi, byte[] selector) {
        if (selector == null || selector.length != 4 || abi.getEntrysList().size() == 0) {
            return false;
        }
        for (int i = 0; i < abi.getEntrysCount(); i++) {
            ABI.Entry entry = abi.getEntrys(i);
            if (entry.getType() != ABI.Entry.EntryType.Function) {
                continue;
            }
            int inputCount = entry.getInputsCount();
            StringBuffer sb = new StringBuffer();
            sb.append(entry.getName());
            sb.append("(");
            for (int k = 0; k < inputCount; k++) {
                ABI.Entry.Param param = entry.getInputs(k);
                sb.append(param.getType());
                if (k + 1 < inputCount) {
                    sb.append(",");
                }
            }
            sb.append(")");
            byte[] funcSelector = new byte[4];
            System.arraycopy(Hash.sha3(sb.toString().getBytes()), 0, funcSelector, 0, 4);
            if (Arrays.equals(funcSelector, selector)) {
                if (entry.getConstant() == true || entry.getStateMutability().equals(ABI.Entry.StateMutabilityType.View)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private static byte[] getSelector(byte[] data) {
        if (data == null || data.length < 4) {
            return null;
        }
        byte[] ret = new byte[4];
        System.arraycopy(data, 0, ret, 0, 4);
        return ret;
    }
}
