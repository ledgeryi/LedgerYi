package cn.ledgeryi.chainbase.core.db;

import cn.ledgeryi.chainbase.common.storage.leveldb.LevelDbDataSourceImpl;
import cn.ledgeryi.chainbase.common.storage.rocksdb.RocksDbDataSourceImpl;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.db.common.DbSourceInter;
import cn.ledgeryi.chainbase.core.db2.core.ILedgerYiBase;
import cn.ledgeryi.common.core.exception.BadItemException;
import cn.ledgeryi.common.core.exception.ItemNotFoundException;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.WriteOptions;

import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map.Entry;

@Slf4j(topic = "DB")
public abstract class LedgerYiDatabase<T> implements ILedgerYiBase<T> {

  protected DbSourceInter<byte[]> dbSource;
  @Getter
  private String dbName;

  protected LedgerYiDatabase(String dbName) {
    this.dbName = dbName;

    if ("LEVELDB".equals(DBConfig.getDbEngine().toUpperCase())) {
      dbSource =
          new LevelDbDataSourceImpl(DBConfig.getOutputDirectoryByDbName(dbName),
              dbName,
              DBConfig.getOptionsByDbName(dbName),
              new WriteOptions().sync(DBConfig.isDbSync()));
    } else if ("ROCKSDB".equals(DBConfig.getDbEngine().toUpperCase())) {
      String parentName = Paths.get(DBConfig.getOutputDirectoryByDbName(dbName),
          DBConfig.getDbDirectory()).toString();
      dbSource =
          new RocksDbDataSourceImpl(parentName, dbName, DBConfig.getRocksDbSettings());
    }

    dbSource.initDB();
  }

  protected LedgerYiDatabase() {
  }

  public DbSourceInter<byte[]> getDbSource() {
    return dbSource;
  }

  /**
   * reset the database.
   */
  public void reset() {
    dbSource.resetDb();
  }

  /**
   * close the database.
   */
  @Override
  public void close() {
    dbSource.closeDB();
  }

  public abstract void put(byte[] key, T item);

  public abstract void delete(byte[] key);

  public abstract T get(byte[] key)
      throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException;

  public T getUnchecked(byte[] key) {
    return null;
  }

  public abstract boolean has(byte[] key);

  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Iterator<Entry<byte[], T>> iterator() {
    throw new UnsupportedOperationException();
  }
}
