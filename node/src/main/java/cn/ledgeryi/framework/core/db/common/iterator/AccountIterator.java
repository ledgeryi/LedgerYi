package cn.ledgeryi.framework.core.db.common.iterator;

import cn.ledgeryi.chainbase.core.capsule.AccountCapsule;

import java.util.Iterator;
import java.util.Map.Entry;

public class AccountIterator extends AbstractIterator<AccountCapsule> {

  public AccountIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected AccountCapsule of(byte[] value) {
    return new AccountCapsule(value);
  }
}
