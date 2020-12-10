package cn.ledgeryi.crypto.jce;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import cn.ledgeryi.crypto.cryptohash.Keccak256;
import cn.ledgeryi.crypto.cryptohash.Keccak512;

import java.security.Provider;
import java.security.Security;

public final class LedgerYiCastleProvider {

  public static Provider getInstance() {
    return Holder.INSTANCE;
  }

  private static class Holder {

    private static final Provider INSTANCE;

    static {
      Provider p = Security.getProvider("SC");

      INSTANCE = (p != null) ? p : new BouncyCastleProvider();
      INSTANCE.put("MessageDigest.JING-CHAIN-KECCAK-256", Keccak256.class.getName());
      INSTANCE.put("MessageDigest.JING-CHAIN-KECCAK-512", Keccak512.class.getName());
    }
  }
}
