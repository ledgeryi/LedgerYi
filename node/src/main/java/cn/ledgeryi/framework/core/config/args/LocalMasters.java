package cn.ledgeryi.framework.core.config.args;

import cn.ledgeryi.chainbase.core.config.Parameter;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignUtils;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j(topic = "app")
public class LocalMasters {

  @Getter
  private List<String> privateKeys = Lists.newArrayList();

  private byte[] masterAccountAddress;

  public LocalMasters() {
  }

  public LocalMasters(String privateKey) {
    addPrivateKeys(privateKey);
  }

  public LocalMasters(List<String> privateKeys) {
    setPrivateKeys(privateKeys);
  }

  public byte[] getMasterAccountAddress(boolean isEccCryptoEngine) {
    if (masterAccountAddress == null) {
      byte[] privateKey = ByteArray.fromHexString(getPrivateKey());
      final SignInterface cryptoEngine = SignUtils
          .fromPrivate(privateKey, isEccCryptoEngine);
      this.masterAccountAddress = cryptoEngine.getAddress();
    }
    return masterAccountAddress;
  }

  public void setMasterAccountAddress(final byte[] localMasterAccountAddress) {
    this.masterAccountAddress = localMasterAccountAddress;
  }

  public void initMasterAccountAddress(boolean isEccCryptoEngine) {
    if (masterAccountAddress == null) {
      byte[] privateKey = ByteArray.fromHexString(getPrivateKey());
      final SignInterface ecKey = SignUtils.fromPrivate(privateKey,
          isEccCryptoEngine);
      this.masterAccountAddress = ecKey.getAddress();
    }
  }

  /**
   * Private key of ECKey.
   */
  public void setPrivateKeys(final List<String> privateKeys) {
    if (CollectionUtils.isEmpty(privateKeys)) {
      return;
    }
    for (String privateKey : privateKeys) {
      validate(privateKey);
    }
    this.privateKeys = privateKeys;
  }

  private void validate(String privateKey) {
    if (StringUtils.startsWithIgnoreCase(privateKey, "0X")) {
      privateKey = privateKey.substring(2);
    }

    if (StringUtils.isNotBlank(privateKey)
        && privateKey.length() != Parameter.ChainConstant.PRIVATE_KEY_LENGTH) {
      throw new IllegalArgumentException(
          "Private key(" + privateKey + ") must be " + Parameter.ChainConstant.PRIVATE_KEY_LENGTH
              + "-bits hex string.");
    }
  }

  public void addPrivateKeys(String privateKey) {
    validate(privateKey);
    this.privateKeys.add(privateKey);
  }

  //get the first one recently
  public String getPrivateKey() {
    if (CollectionUtils.isEmpty(privateKeys)) {
      log.warn("privateKey is null");
      return null;
    }
    return privateKeys.get(0);
  }

}
