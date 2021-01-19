package cn.ledgeryi.contract.utils;

import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.protos.Protocol;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "capsule")
public class TransactionUtil {

  public static Sha256Hash getTransactionId(Protocol.Transaction transaction) {
    return Sha256Hash.of(DBConfig.isEccCryptoEngine(),
        transaction.getRawData().toByteArray());
  }
}
