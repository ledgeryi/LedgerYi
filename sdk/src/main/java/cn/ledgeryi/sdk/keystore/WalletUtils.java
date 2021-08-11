package cn.ledgeryi.sdk.keystore;

import cn.ledgeryi.crypto.ecdsa.ECKey;
import cn.ledgeryi.crypto.sm2.SM2;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.Utils;
import cn.ledgeryi.sdk.config.Configuration;
import cn.ledgeryi.sdk.exception.CipherException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;

/**
 * Utility functions for working with Wallet files.
 */
@Slf4j
public class WalletUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String filePath = "Wallet";

  static {
    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static WalletFile createWalletFile(String password)
          throws CipherException {
    WalletFile walletFile;
    if (!passwordValid(password.toCharArray())) {
      return null;
    }
    byte[] passwd = StringUtils.char2Byte(password.toCharArray());
    if (Configuration.isEcc()) {
      ECKey ecKey = new ECKey(Utils.getRandom());
      walletFile = Wallet.createStandard(passwd, ecKey);
    } else {
      SM2 sm2 = new SM2(Utils.getRandom());
      walletFile = Wallet.createStandard(passwd, sm2);
    }
    return walletFile;
  }

  private static String createKeyStoreFile(String password)
          throws CipherException, IOException {
    WalletFile walletFile = createWalletFile(password);
    return store2Keystore(walletFile);
  }

//  public static void main(String[] args) throws CipherException, IOException {
////    String keyStoreFile = createKeyStoreFile("1qaz2wsx@");
////    System.out.println(keyStoreFile);
//    File file = new File("./Wallet/c1fdf6f30dfc8eaeba1bf2161e3b59db3e8e3f02.json");
//    String address = exportAddress(file);
//    System.out.println(address);
//    byte[] bytes = exportPrivateKey(file, "1qaz2wsx@");
//    System.out.println(DecodeUtil.createReadableString(bytes));
//  }

  private static String store2Keystore(WalletFile walletFile) throws IOException {
    if (walletFile == null) {
      log.warn("Warning: Store wallet failed, walletFile is null !!");
      return null;
    }
    File file = new File(filePath);
    if (!file.exists()) {
      if (!file.mkdir()) {
        throw new IOException("Make directory failed!");
      }
    } else {
      if (!file.isDirectory()) {
        if (file.delete()) {
          if (!file.mkdir()) {
            throw new IOException("Make directory failed!");
          }
        } else {
          throw new IOException("File exists and can not be deleted!");
        }
      }
    }
    return WalletUtils.generateWalletFile(walletFile, file);
  }

  private static String generateWalletFile(WalletFile walletFile, File destinationDirectory)
          throws IOException {
    String fileName = getWalletFileName(walletFile);
    File destination = new File(destinationDirectory, fileName);
    objectMapper.writeValue(destination, walletFile);
    return fileName;
  }

  public static WalletFile importWalletFile(File walletFile, String password)
          throws IOException, CipherException {
    WalletFile wallet = loadWalletFile(walletFile);
    byte[] passwd = StringUtils.char2Byte(password.toCharArray());
    boolean validPassword = Wallet.validPassword(passwd, wallet);
    if (validPassword) {
      throw new CipherException("password check fail");
    }
    return wallet;
  }

  public static String exportAddress(File walletFile) throws IOException {
    WalletFile wallet = loadWalletFile(walletFile);
    return wallet.getAddress();
  }

  public static byte[] exportPrivateKey(File walletFile, String password)
          throws CipherException, IOException {
    byte[] pwd = StringUtils.char2Byte(password.toCharArray());
    WalletFile wallet = loadWalletFile(walletFile);
    return Wallet.decrypt2PrivateBytes(pwd, wallet);
  }

  public static WalletFile loadWalletFile(File source) throws IOException {
    return objectMapper.readValue(source, WalletFile.class);
  }

  public static boolean passwordValid(char[] password) {
    if (ArrayUtils.isEmpty(password)) {
      throw new IllegalArgumentException("password is empty");
    }
    if (password.length < 6) {
      log.warn("Warning: Password is too short !!");
      return false;
    }
    // Other rule;
    int level = CheckStrength.checkPasswordStrength(password);
    if (level <= 4) {
      log.warn("Your password is too weak!");
      log.warn("The password should be at least 8 characters.");
      log.warn("The password should contains uppercase, lowercase, numeric and other.");
      log.warn("The password should not contain more than 3 duplicate numbers or letters; For example: 1111.");
      log.warn("The password should not contain more than 3 consecutive Numbers or letters; For example: 1234.");
      log.warn("The password should not contain weak password combination; For example:");
      log.warn("ababab, abcabc, password, passw0rd, p@ssw0rd, admin1234, etc.");
      return false;
    }
    return true;
  }

  private static String getWalletFileName(WalletFile walletFile) {
    return walletFile.getAddress() + ".json";
  }
}
