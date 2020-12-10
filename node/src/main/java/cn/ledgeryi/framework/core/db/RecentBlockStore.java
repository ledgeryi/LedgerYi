package cn.ledgeryi.framework.core.db;

import cn.ledgeryi.chainbase.core.capsule.BytesCapsule;
import cn.ledgeryi.chainbase.core.db.LedgerYiStoreWithRevoking;
import cn.ledgeryi.common.core.exception.ItemNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RecentBlockStore extends LedgerYiStoreWithRevoking<BytesCapsule> {

  @Autowired
  private RecentBlockStore(@Value("recent-block") String dbName) {
    super(dbName);
  }

  @Override
  public BytesCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);

    return new BytesCapsule(value);
  }
}
