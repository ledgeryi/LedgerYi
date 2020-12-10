package cn.ledgeryi.framework.core.db.common.iterator;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.common.core.exception.BadItemException;

import java.util.Iterator;
import java.util.Map.Entry;

public class BlockIterator extends AbstractIterator<BlockCapsule> {

  public BlockIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected BlockCapsule of(byte[] value) {
    try {
      return new BlockCapsule(value);
    } catch (BadItemException e) {
      throw new RuntimeException(e);
    }
  }
}
