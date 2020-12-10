package cn.ledgeryi.framework.common.utils;

import com.google.common.primitives.Longs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

@Slf4j(topic = "utils")
public class TypeConversion {

  public static byte[] longToBytes(long x) {
    return Longs.toByteArray(x);
  }

  public static long bytesToLong(byte[] bytes) {
    return Longs.fromByteArray(bytes);
  }

  public static String bytesToHexString(byte[] src) {
    return Hex.encodeHexString(src);
  }

  public static byte[] hexStringToBytes(String hexString) {
    try {
      return Hex.decodeHex(hexString);
    } catch (DecoderException e) {
      log.debug(e.getMessage(), e);
      return null;
    }
  }

  public static boolean increment(byte[] bytes) {
    final int startIndex = 0;
    int i;
    for (i = bytes.length - 1; i >= startIndex; i--) {
      bytes[i]++;
      if (bytes[i] != 0) {
        break;
      }
    }

    return (i >= startIndex || bytes[startIndex] != 0);
  }
}
