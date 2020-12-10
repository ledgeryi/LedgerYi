package cn.ledgeryi.chainbase.common.utils;

import cn.ledgeryi.common.utils.Base58;
import cn.ledgeryi.common.utils.Sha256Hash;
import com.google.protobuf.ByteString;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class WalletUtil {

public static String encode58Check(byte[] input) {
  byte[] hash0 = Sha256Hash.hash(DBConfig.isECKeyCryptoEngine(), input);
  byte[] hash1 = Sha256Hash.hash(DBConfig.isECKeyCryptoEngine(), hash0);
  byte[] inputCheck = new byte[input.length + 4];
  System.arraycopy(input, 0, inputCheck, 0, input.length);
  System.arraycopy(hash1, 0, inputCheck, input.length, 4);
  return Base58.encode(inputCheck);
}

public static List<String> getAddressStringList(Collection<ByteString> collection) {
  return collection.stream()
    .map(bytes -> encode58Check(bytes.toByteArray()))
    .collect(Collectors.toList());
}

}
