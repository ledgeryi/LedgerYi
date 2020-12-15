package cn.ledgeryi.sdk.common.utils;

import cn.ledgeryi.common.utils.Base58;
import cn.ledgeryi.sdk.common.crypto.Sha256Sm3Hash;
import cn.ledgeryi.sdk.core.config.Parameter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class CommonUtils {

    public static byte[] decodeFromBase58Check(String addressBase58) {
        if (StringUtils.isEmpty(addressBase58)) {
            return null;
        }
        byte[] address = decode58Check(addressBase58);
        if (address == null) {
            return null;
        }
        if (!addressValid(address)) {
            return null;
        }
        return address;
    }

    private static boolean addressValid(byte[] address) {
        if (ArrayUtils.isEmpty(address)) {
            return false;
        }
        if (address.length != Parameter.CommonConstant.ADDRESS_SIZE) {
            return false;
        }
        byte preFixbyte = address[0];
        if (preFixbyte != Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET) {
            return false;
        }
        return true;
    }

    public static byte[] decode58Check(String input) {
        byte[] decodeCheck = Base58.decode(input);
        if (decodeCheck.length <= 4) {
            return null;
        }
        byte[] decodeData = new byte[decodeCheck.length - 4];
        System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
        byte[] hash0 = Sha256Sm3Hash.hash(decodeData);
        byte[] hash1 = Sha256Sm3Hash.hash(hash0);
        if (hash1[0] == decodeCheck[decodeData.length] &&
                hash1[1] == decodeCheck[decodeData.length + 1] &&
                hash1[2] == decodeCheck[decodeData.length + 2] &&
                hash1[3] == decodeCheck[decodeData.length + 3]) {
            return decodeData;
        }
        return null;
    }

    public static String encode58Check(byte[] input) {
        byte[] hash0 = Sha256Sm3Hash.hash(input);
        byte[] hash1 = Sha256Sm3Hash.hash(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }
}
