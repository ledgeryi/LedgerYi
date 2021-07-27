package cn.ledgeryi.sdk.keystore;

import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.ecdsa.ECKey;
import cn.ledgeryi.crypto.sm2.SM2;
import cn.ledgeryi.sdk.common.utils.Utils;
import cn.ledgeryi.sdk.config.Configuration;
import cn.ledgeryi.sdk.exception.CipherException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;

import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility functions for working with Wallet files.
 */
public class WalletUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static boolean isEckey = true;

  static {
    Config config = Configuration.getByPath("config.conf");
    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    if (config.hasPath("crypto.engine")) {
      isEckey = config.getString("crypto.engine").equalsIgnoreCase("eckey");
      System.out.println("WalletUtils getConfig isEckey: " + isEckey);
    }
  }

  public static String generateFullNewWalletFile(byte[] password, File destinationDirectory)
      throws CipherException, IOException {

    return generateNewWalletFile(password, destinationDirectory, true);
  }

  public static String generateLightNewWalletFile(byte[] password, File destinationDirectory)
      throws CipherException, IOException {

    return generateNewWalletFile(password, destinationDirectory, false);
  }

  public static String generateNewWalletFile(
      byte[] password, File destinationDirectory, boolean useFullScrypt)
      throws CipherException, IOException {
    SignInterface ecKeySm2Pair = null;
    if (isEckey) {
      ecKeySm2Pair = new ECKey(Utils.getRandom());
    } else {
      ecKeySm2Pair = new SM2(Utils.getRandom());
    }
    return generateWalletFile(password, ecKeySm2Pair, destinationDirectory, useFullScrypt);
  }

  public static String generateWalletFile(
          byte[] password, SignInterface ecKeySm2Pair, File destinationDirectory, boolean useFullScrypt)
      throws CipherException, IOException {

    WalletFile walletFile;
    if (useFullScrypt) {
      walletFile = Wallet.createStandard(password, ecKeySm2Pair);
    } else {
      walletFile = Wallet.createLight(password, ecKeySm2Pair);
    }

    String fileName = getWalletFileName(walletFile);
    File destination = new File(destinationDirectory, fileName);

    objectMapper.writeValue(destination, walletFile);

    return fileName;
  }

  public static void updateWalletFile(
      byte[] password, SignInterface ecKeySm2Pair, File source, boolean useFullScrypt)
      throws CipherException, IOException {

    WalletFile walletFile = objectMapper.readValue(source, WalletFile.class);
    if (useFullScrypt) {
      walletFile = Wallet.createStandard(password, ecKeySm2Pair);
    } else {
      walletFile = Wallet.createLight(password, ecKeySm2Pair);
    }

    objectMapper.writeValue(source, walletFile);
  }

  public static String generateWalletFile(WalletFile walletFile, File destinationDirectory)
      throws IOException {
    String fileName = getWalletFileName(walletFile);
    File destination = new File(destinationDirectory, fileName);

    objectMapper.writeValue(destination, walletFile);
    return fileName;
  }

  public static Credentials loadCredentials(byte[] password, File source)
      throws IOException, CipherException {
    WalletFile walletFile = objectMapper.readValue(source, WalletFile.class);

    if (isEckey) {
      return CredentialsEckey.create(Wallet.decrypt(password, walletFile));
    }
    return CredentialsSM2.create(Wallet.decryptSM2(password, walletFile));
  }

  public static WalletFile loadWalletFile(File source) throws IOException {
    return objectMapper.readValue(source, WalletFile.class);
  }

  private static String getWalletFileName(WalletFile walletFile) {
    DateTimeFormatter format = DateTimeFormatter.ofPattern(
        "'UTC--'yyyy-MM-dd'T'HH-mm-ss.nVV'--'");
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

    return now.format(format) + walletFile.getAddress() + ".json";
  }

  public static void generateSkeyFile(SKeyCapsule skey, File file)
      throws IOException {
    objectMapper.writeValue(file, skey);
  }

  public static SKeyCapsule loadSkeyFile(File source) throws IOException {
    return objectMapper.readValue(source, SKeyCapsule.class);
  }
}
