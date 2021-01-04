package cn.ledgeryi.contract.utils;

import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.contract.vm.repository.Repository;

public class MUtil {
    private MUtil() {
    }

    public static void transfer(Repository deposit, byte[] fromAddress, byte[] toAddress, long amount)
            throws ContractValidateException {
        if (0 == amount) {
            return;
        }
        VMUtils.validateForSmartContract(deposit, fromAddress, toAddress, amount);
        deposit.addBalance(toAddress, amount);
        deposit.addBalance(fromAddress, -amount);
    }

    public static boolean isNullOrEmpty(String str) {
        return (str == null) || str.isEmpty();
    }

    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }
}
