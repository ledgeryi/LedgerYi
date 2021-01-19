package cn.ledgeryi.crypto;

import cn.ledgeryi.crypto.ecdsa.ECKey;
import cn.ledgeryi.crypto.ecdsa.ECKey.ECDSASignature;
import cn.ledgeryi.crypto.sm2.SM2;
import cn.ledgeryi.crypto.sm2.SM2.SM2Signature;

import java.security.SecureRandom;
import java.security.SignatureException;

public class SignUtils {

  public static SignInterface getGeneratedRandomSign(
      SecureRandom secureRandom, boolean isEccCryptoEngine) {
    if (isEccCryptoEngine) {
      return new ECKey(secureRandom);
    }
    return new SM2(secureRandom);
  }

  public static SignInterface fromPrivate(byte[] privKeyBytes, boolean isEccCryptoEngine) {
    if (isEccCryptoEngine) {
      return ECKey.fromPrivate(privKeyBytes);
    }
    return SM2.fromPrivate(privKeyBytes);
  }

  public static byte[] signatureToAddress(byte[] messageHash, String signatureBase64, boolean isEccCryptoEngine)
      throws SignatureException {
    if (isEccCryptoEngine) {
      return ECKey.signatureToAddress(messageHash, signatureBase64);
    }
    return SM2.signatureToAddress(messageHash, signatureBase64);
  }

  public static SignatureInterface fromComponents(byte[] r, byte[] s, byte v, boolean isEccCryptoEngine) {
    if (isEccCryptoEngine) {
      return ECDSASignature.fromComponents(r, s, v);
    }
    return SM2Signature.fromComponents(r, s, v);
  }

  public static byte[] signatureToAddress(byte[] messageHash, SignatureInterface signatureInterface,
                                          boolean isEccCryptoEngine) throws SignatureException {
    if (isEccCryptoEngine) {
      return ECKey.signatureToAddress(messageHash, (ECDSASignature) signatureInterface);
    }
    return SM2.signatureToAddress(messageHash, (SM2Signature) signatureInterface);
  }
}
