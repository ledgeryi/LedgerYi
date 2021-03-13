package cn.ledgeryi.framework.core.db;

import cn.ledgeryi.chainbase.core.capsule.TransactionRetCapsule;
import cn.ledgeryi.chainbase.core.db.LedgerYiStoreWithRevoking;
import cn.ledgeryi.common.core.exception.BadItemException;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.protos.Protocol;
import com.google.protobuf.ByteString;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import cn.ledgeryi.chainbase.core.capsule.TransactionInfoCapsule;
import cn.ledgeryi.framework.core.config.args.Args;

@Slf4j(topic = "DB")
@Component
public class TransactionRetStore extends LedgerYiStoreWithRevoking<TransactionRetCapsule> {

  @Autowired
  private TransactionStore transactionStore;

  @Autowired
  public TransactionRetStore(@Value("transactionRetStore") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, TransactionRetCapsule item) {
    if (BooleanUtils.toBoolean(Args.getInstance().getStorage().getTransactionHistoreSwitch())) {
      super.put(key, item);
    }
  }

  public TransactionInfoCapsule getTransactionInfo(byte[] key) throws BadItemException {
    long blockNumber = transactionStore.getBlockNumber(key);
    if (blockNumber == -1) {
      return null;
    }
    byte[] value = revokingDB.getUnchecked(ByteArray.fromLong(blockNumber));
    if (Objects.isNull(value)) {
      return null;
    }

    TransactionRetCapsule result = new TransactionRetCapsule(value);
    if (Objects.isNull(result) || Objects.isNull(result.getInstance())) {
      return null;
    }

    for (Protocol.TransactionInfo transactionResultInfo : result.getInstance().getTransactionInfoList()) {
      if (transactionResultInfo.getId().equals(ByteString.copyFrom(key))) {
        return new TransactionInfoCapsule(transactionResultInfo);
      }
    }
    return null;
  }

  public TransactionRetCapsule getTransactionInfoByBlockNum(byte[] key) throws BadItemException {

    byte[] value = revokingDB.getUnchecked(key);
    if (Objects.isNull(value)) {
      return null;
    }

    return new TransactionRetCapsule(value);
  }

}
