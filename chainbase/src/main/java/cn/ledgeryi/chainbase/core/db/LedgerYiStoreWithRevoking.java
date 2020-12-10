package cn.ledgeryi.chainbase.core.db;

import cn.ledgeryi.chainbase.common.storage.leveldb.LevelDbDataSourceImpl;
import cn.ledgeryi.chainbase.common.storage.rocksdb.RocksDbDataSourceImpl;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.capsule.ProtoCapsule;
import cn.ledgeryi.chainbase.core.db2.common.DB;
import cn.ledgeryi.chainbase.core.db2.common.IRevokingDB;
import cn.ledgeryi.chainbase.core.db2.common.LevelDB;
import cn.ledgeryi.chainbase.core.db2.common.RocksDB;
import cn.ledgeryi.chainbase.core.db2.core.Chainbase;
import cn.ledgeryi.chainbase.core.db2.core.ILedgerYiBase;
import cn.ledgeryi.chainbase.core.db2.core.RevokingDBWithCachingOldValue;
import cn.ledgeryi.chainbase.core.db2.core.SnapshotRoot;
import cn.ledgeryi.common.core.exception.BadItemException;
import cn.ledgeryi.common.core.exception.ItemNotFoundException;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.WriteOptions;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;


@Slf4j(topic = "DB")
public abstract class LedgerYiStoreWithRevoking<T extends ProtoCapsule> implements ILedgerYiBase<T> {

  @Getter // only for unit test
  protected IRevokingDB revokingDB;
  private TypeToken<T> token = new TypeToken<T>(getClass()) {
  };

  @Autowired
  private RevokingDatabase revokingDatabase;

  protected LedgerYiStoreWithRevoking(String dbName) {
    int dbVersion = DBConfig.getDbVersion();
    String dbEngine = DBConfig.getDbEngine();
    if (dbVersion == 1) {
      this.revokingDB = new RevokingDBWithCachingOldValue(dbName);
    } else if (dbVersion == 2) {
      if ("LEVELDB".equals(dbEngine.toUpperCase())) {
        this.revokingDB = new Chainbase(new SnapshotRoot(
            new LevelDB(
                new LevelDbDataSourceImpl(DBConfig.getOutputDirectoryByDbName(dbName),
                    dbName,
                    DBConfig.getOptionsByDbName(dbName),
                    new WriteOptions().sync(DBConfig.isDbSync())))));
      } else if ("ROCKSDB".equals(dbEngine.toUpperCase())) {
        String parentPath = Paths.get(DBConfig.getOutputDirectoryByDbName(dbName), DBConfig.getDbDirectory()).toString();
        this.revokingDB = new Chainbase(new SnapshotRoot(
            new RocksDB(new RocksDbDataSourceImpl(parentPath, dbName, DBConfig.getRocksDbSettings()))));
      }
    } else {
      throw new RuntimeException("db version is error.");
    }
  }

  protected LedgerYiStoreWithRevoking(DB<byte[], byte[]> db) {
    int dbVersion = DBConfig.getDbVersion();
    if (dbVersion == 2) {
      this.revokingDB = new Chainbase(new SnapshotRoot(db));
    } else {
      throw new RuntimeException("db version is only 2.(" + dbVersion + ")");
    }
  }

  // only for test
  protected LedgerYiStoreWithRevoking(String dbName, RevokingDatabase revokingDatabase) {
    this.revokingDB = new RevokingDBWithCachingOldValue(dbName,
        (AbstractRevokingStore) revokingDatabase);
  }

  @Override
  public String getDbName() {
    return null;
  }

  @PostConstruct
  private void init() {
    revokingDatabase.add(revokingDB);
  }

  @Override
  public void put(byte[] key, T item) {
    if (Objects.isNull(key) || Objects.isNull(item)) {
      return;
    }

    revokingDB.put(key, item.getData());
  }

  @Override
  public void delete(byte[] key) {
    revokingDB.delete(key);
  }

  @Override
  public T get(byte[] key) throws ItemNotFoundException, BadItemException {
    return of(revokingDB.get(key));
  }

  @Override
  public T getUnchecked(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);

    try {
      return of(value);
    } catch (BadItemException e) {
      return null;
    }
  }

  public T of(byte[] value) throws BadItemException {
    try {
      Constructor constructor = token.getRawType().getConstructor(byte[].class);
      @SuppressWarnings("unchecked")
      T t = (T) constructor.newInstance((Object) value);
      return t;
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new BadItemException(e.getMessage());
    }
  }

  @Override
  public boolean has(byte[] key) {
    return revokingDB.has(key);
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public void close() {
    revokingDB.close();
  }

  @Override
  public void reset() {
    revokingDB.reset();
  }

  @Override
  public Iterator<Map.Entry<byte[], T>> iterator() {
    return Iterators.transform(revokingDB.iterator(), e -> {
      try {
        return Maps.immutableEntry(e.getKey(), of(e.getValue()));
      } catch (BadItemException e1) {
        throw new RuntimeException(e1);
      }
    });
  }

  public long size() {
    return Streams.stream(revokingDB.iterator()).count();
  }

  public void setMode(boolean mode) {
    revokingDB.setMode(mode);
  }
}
