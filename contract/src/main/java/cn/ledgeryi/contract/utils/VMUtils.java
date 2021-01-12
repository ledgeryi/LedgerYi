package cn.ledgeryi.contract.utils;

import cn.ledgeryi.chainbase.core.capsule.AccountCapsule;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.contract.vm.config.VmConfig;
import cn.ledgeryi.contract.vm.repository.Repository;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import static java.lang.String.format;

@Slf4j(topic = "VM")
public class VMUtils {

    private VMUtils() {
    }

    public static boolean validateForSmartContract(Repository deposit, byte[] ownerAddress,
                                                   byte[] toAddress) throws ContractValidateException {
        if (deposit == null) {
            throw new ContractValidateException("No deposit!");
        }
        if (!DecodeUtil.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid ownerAddress");
        }
        if (!DecodeUtil.addressValid(toAddress)) {
            throw new ContractValidateException("Invalid toAddress");
        }
        if (Arrays.equals(ownerAddress, toAddress)) {
            throw new ContractValidateException("Cannot transfer asset to yourself.");
        }
        AccountCapsule ownerAccount = deposit.getAccount(ownerAddress);
        if (ownerAccount == null) {
            throw new ContractValidateException("No owner account!");
        }
        return true;
    }

    public static String align(String s, char fillChar, int targetLen, boolean alignRight) {
        if (targetLen <= s.length()) {
            return s;
        }
        String alignString = repeat("" + fillChar, targetLen - s.length());
        return alignRight ? alignString + s : s + alignString;
    }

    private static String repeat(String s, int n) {
        if (s.length() == 1) {
            byte[] bb = new byte[n];
            Arrays.fill(bb, s.getBytes()[0]);
            return new String(bb);
        } else {
            StringBuilder ret = new StringBuilder();
            for (int i = 0; i < n; i++) {
                ret.append(s);
            }
            return ret.toString();
        }
    }
}
