package cn.ledgeryi.chainbase.common.utils;

import cn.ledgeryi.chainbase.core.capsule.AccountCapsule;
import cn.ledgeryi.chainbase.core.store.AccountStore;
import cn.ledgeryi.common.core.exception.BalanceInsufficientException;
import cn.ledgeryi.common.utils.Base58;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.Sha256Hash;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j(topic = "Commons")
public class Commons {

  public static final int ADDRESS_SIZE = 42;

  public static byte[] clone(byte[] value) {
    byte[] clone = new byte[value.length];
    System.arraycopy(value, 0, clone, 0, value.length);
    return clone;
  }

  private static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(DBConfig.isECKeyCryptoEngine(), decodeData);
    byte[] hash1 = Sha256Hash.hash(DBConfig.isECKeyCryptoEngine(), hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
        hash1[1] == decodeCheck[decodeData.length + 1] &&
        hash1[2] == decodeCheck[decodeData.length + 2] &&
        hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      log.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != ADDRESS_SIZE / 2) {
      log.warn(
          "Warning: Address length need " + ADDRESS_SIZE + " but " + address.length
              + " !!");
      return false;
    }

    //Other rule;
    return true;
  }

  public static String encode58Check(byte[] input) {
    byte[] hash0 = Sha256Hash.hash(DBConfig.isECKeyCryptoEngine(), input);
    byte[] hash1 = Sha256Hash.hash(DBConfig.isECKeyCryptoEngine(), hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }

  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      log.warn("Warning: Address is empty !!");
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

  public static String createReadableString(byte[] bytes) {
    return ByteArray.toHexString(bytes);
  }

  public static void adjustBalance(AccountStore accountStore, byte[] accountAddress, long amount) throws BalanceInsufficientException {
    AccountCapsule account = accountStore.getUnchecked(accountAddress);
    adjustBalance(accountStore, account, amount);
  }

  private static void adjustBalance(AccountStore accountStore, AccountCapsule account, long amount) throws BalanceInsufficientException {
    long balance = account.getBalance();
    if (amount == 0) {
      return;
    }
    if (amount < 0 && balance < -amount) {
      throw new BalanceInsufficientException(createReadableString(account.createDbKey()) + " insufficient balance");
    }
    account.setBalance(Math.addExact(balance, amount));
    accountStore.put(account.getAddress().toByteArray(), account);
  }
}
