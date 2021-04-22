package cn.ledgeryi.framework.core.db.common.iterator;

import java.io.IOException;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DBIterator;

@Slf4j(topic = "DB")
public final class StoreIterator implements cn.ledgeryi.framework.core.db.common.iterator.DBIterator {

  private DBIterator dbIterator;
  private boolean first = true;

  public StoreIterator(DBIterator dbIterator) {
    this.dbIterator = dbIterator;
  }

  @Override
  public void close() throws IOException {
    dbIterator.close();
  }

  @Override
  public boolean hasNext() {
    boolean hasNext = false;
    // true is first item
    try {
      if (first) {
        dbIterator.seekToFirst();
        first = false;
      }

      if (!(hasNext = dbIterator.hasNext())) { // false is last item
        dbIterator.close();
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return hasNext;
  }

  @Override
  public Entry<byte[], byte[]> next() {
    return dbIterator.next();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
