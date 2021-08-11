package cn.ledgeryi.sdk.common.utils;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j(topic = "Commons")
public class DecodeUtil {

  public static final int ADDRESS_SIZE = 40;

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
    return true;
  }

//  public static void main(String[] args) {
//    String ownerAddress = "42979c83d087b612fdc82c560b3131b9c7f34a76";
//    System.out.println(validAddress(ownerAddress));
//  }

  public static boolean validAddress(String input) {
    byte[] address = null;
    boolean result = true;
    try {
      if (input.length() == ADDRESS_SIZE) {
        address = ByteArray.fromHexString(input);
      } else {
        result = false;
      }
      if (result) {
        result = addressValid(address);
      }
    } catch (Exception e) {
      result = false;
    }
    return result;
  }

  @Deprecated
  public static String createReadableString(byte[] bytes) {
    return ByteArray.toHexString(bytes);
  }

  @Deprecated
  public static String createReadableString(ByteString string) {
    return createReadableString(string.toByteArray());
  }

  public static String encode(byte[] bytes) {
    return ByteArray.toHexString(bytes);
  }

  public static String encode(ByteString string) {
    return ByteArray.toHexString(string.toByteArray());
  }

  public static byte[] decode(String data){
    return ByteArray.fromHexString(data);
  }

}
