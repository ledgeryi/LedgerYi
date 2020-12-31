package cn.ledgeryi.common.utils;

import com.google.protobuf.ByteString;

public class StringUtil {

  /**
   * n-bits hex string.
   *
   * @param str target string
   * @param bits string bits
   */
  public static boolean isHexString(String str, int bits) {
    String regex = String.format("^[A-Fa-f0-9]{%d}$", bits);
    return str.matches(regex);
  }


  public static byte[] createDbKey(ByteString string) {
    return string.toByteArray();
  }

  public static String createReadableString(byte[] bytes) {
    return ByteArray.toHexString(bytes);
  }

  public static String createReadableString(ByteString string) {
    return createReadableString(string.toByteArray());
  }



  public static ByteString hexString2ByteString(String hexString) {
    return ByteString.copyFrom(ByteArray.fromHexString(hexString));
  }
}
