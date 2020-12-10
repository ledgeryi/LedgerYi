package cn.ledgeryi.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j(topic = "Commons")
public class DecodeUtil {

  public static final int ADDRESS_SIZE = 42;

  public static byte[] clone(byte[] value) {
    byte[] clone = new byte[value.length];
    System.arraycopy(value, 0, clone, 0, value.length);
    return clone;
  }

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      log.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != ADDRESS_SIZE / 2) {
      log.warn("Warning: Address length need " + ADDRESS_SIZE + " but " + address.length + " !!");
      return false;
    }

    //Other rule;
    return true;
  }

  public static String createReadableString(byte[] bytes) {
    return ByteArray.toHexString(bytes);
  }

}
