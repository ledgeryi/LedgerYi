package cn.ledgeryi.sdk.keystore;

import cn.ledgeryi.crypto.SignInterface;

public interface Credentials {
  SignInterface getPair();

  String getAddress();
}
