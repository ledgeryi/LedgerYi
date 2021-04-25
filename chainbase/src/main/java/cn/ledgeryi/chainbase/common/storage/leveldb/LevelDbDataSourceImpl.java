package cn.ledgeryi.chainbase.common.storage.leveldb;

import cn.ledgeryi.chainbase.common.storage.WriteOptionsWrapper;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.db.common.DbSourceInter;
import cn.ledgeryi.chainbase.core.db.common.iterator.StoreIterator;
import cn.ledgeryi.chainbase.core.db2.common.Instance;
import cn.ledgeryi.common.utils.FileUtil;
import com.google.common.collect.Sets;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

@Slf4j(topic = "DB")
@NoArgsConstructor
public class LevelDbDataSourceImpl implements DbSourceInter<byte[]>,
    Iterable<Entry<byte[], byte[]>>, Instance<LevelDbDataSourceImpl> {

  private String dataBaseName;
  private DB database;
  private boolean alive;
  private String parentPath;
  private Options options;
  private WriteOptions writeOptions;
  private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();

  /**
   * constructor.
   */
  public LevelDbDataSourceImpl(String parentPath, String dataBaseName, Options options,
      WriteOptions writeOptions) {
    this.parentPath = Paths.get(
        parentPath,
        DBConfig.getDbDirectory()
    ).toString();
    this.dataBaseName = dataBaseName;
    this.options = options;
    this.writeOptions = writeOptions;
    initDB();
  }

  public LevelDbDataSourceImpl(String parentPath, String dataBaseName) {
    this.parentPath = Paths.get(
        parentPath,
        DBConfig.getDbDirectory()
    ).toString();

    this.dataBaseName = dataBaseName;
    options = new Options();
    writeOptions = new WriteOptions();
  }

  @Override
  public void initDB() {
    resetDbLock.writeLock().lock();
    try {
      log.debug("~> LevelDbDataSourceImpl.initDB(): " + dataBaseName);

      if (isAlive()) {
        return;
      }

      if (dataBaseName == null) {
        throw new NullPointerException("no name set to the dbStore");
      }

      try {
        openDatabase(options);
        alive = true;
      } catch (IOException ioe) {
        throw new RuntimeException("Can't initialize database", ioe);
      }
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  private void openDatabase(Options dbOptions) throws IOException {
    final Path dbPath = getDbPath();
    if (dbPath == null || dbPath.getParent() == null) {
      return;
    }
    if (!Files.isSymbolicLink(dbPath.getParent())) {
      Files.createDirectories(dbPath.getParent());
    }
    try {
      database = factory.open(dbPath.toFile(), dbOptions);
    } catch (IOException e) {
      if (e.getMessage().contains("Corruption:")) {
        factory.repair(dbPath.toFile(), dbOptions);
        database = factory.open(dbPath.toFile(), dbOptions);
      } else {
        throw e;
      }
    }
  }

  @Deprecated
  private Options createDbOptions() {
    Options dbOptions = new Options();
    dbOptions.createIfMissing(true);
    dbOptions.compressionType(CompressionType.NONE);
    dbOptions.blockSize(10 * 1024 * 1024);
    dbOptions.writeBufferSize(10 * 1024 * 1024);
    dbOptions.cacheSize(0);
    dbOptions.paranoidChecks(true);
    dbOptions.verifyChecksums(true);
    dbOptions.maxOpenFiles(32);
    return dbOptions;
  }

  public Path getDbPath() {
    return Paths.get(parentPath, dataBaseName);
  }

  /**
   * reset database.
   */
  public void resetDb() {
    closeDB();
    FileUtil.recursiveDelete(getDbPath().toString());
    initDB();
  }

  @Override
  public boolean isAlive() {
    return alive;
  }

  /**
   * destroy database.
   */
  public void destroyDb(File fileLocation) {
    resetDbLock.writeLock().lock();
    try {
      log.debug("Destroying existing database: " + fileLocation);
      Options options = new Options();
      try {
        factory.destroy(fileLocation, options);
      } catch (IOException e) {
        log.error(e.getMessage(), e);
      }
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  @Override
  public String getDBName() {
    return dataBaseName;
  }

  @Override
  public void setDBName(String name) {
    this.dataBaseName = name;
  }

  @Override
  public byte[] getData(byte[] key) {
    resetDbLock.readLock().lock();
    try {
      return database.get(key);
    } catch (DBException e) {
      log.error(e.getMessage(), e);
    } finally {
      resetDbLock.readLock().unlock();
    }
    return null;
  }

  @Override
  public void putData(byte[] key, byte[] value) {
    resetDbLock.readLock().lock();
    try {
      database.put(key, value, writeOptions);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void deleteData(byte[] key) {
    resetDbLock.readLock().lock();
    try {
      database.delete(key, writeOptions);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Deprecated
  @Override
  public Set<byte[]> allKeys() {
    resetDbLock.readLock().lock();
    try (DBIterator iterator = database.iterator()) {
      Set<byte[]> result = Sets.newHashSet();
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        result.add(iterator.peekNext().getKey());
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Deprecated
  @Override
  public Set<byte[]> allValues() {
    resetDbLock.readLock().lock();
    try (DBIterator iterator = database.iterator()) {
      Set<byte[]> result = Sets.newHashSet();
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        result.add(iterator.peekNext().getValue());
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Set<byte[]> getlatestValues(long limit) {
    if (limit <= 0) {
      return Sets.newHashSet();
    }
    resetDbLock.readLock().lock();
    try (DBIterator iterator = database.iterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      iterator.seekToLast();
      if (iterator.hasNext()) {
        result.add(iterator.peekNext().getValue());
        i++;
      }
      for (; iterator.hasPrev() && i++ < limit; iterator.prev()) {
        result.add(iterator.peekPrev().getValue());
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Set<byte[]> getValuesNext(byte[] key, long limit) {
    if (limit <= 0) {
      return Sets.newHashSet();
    }
    resetDbLock.readLock().lock();
    try (DBIterator iterator = database.iterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      for (iterator.seek(key); iterator.hasNext() && i++ < limit; iterator.next()) {
        result.add(iterator.peekNext().getValue());
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Map<byte[], byte[]> getNext(byte[] key, long limit) {
    if (limit <= 0) {
      return Collections.emptyMap();
    }
    resetDbLock.readLock().lock();
    try (DBIterator iterator = database.iterator()) {
      Map<byte[], byte[]> result = new HashMap<>();
      long i = 0;
      for (iterator.seek(key); iterator.hasNext() && i++ < limit; iterator.next()) {
        Entry<byte[], byte[]> entry = iterator.peekNext();
        result.put(entry.getKey(), entry.getValue());
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Set<byte[]> getValuesPrev(byte[] key, long limit) {
    if (limit <= 0) {
      return Sets.newHashSet();
    }
    resetDbLock.readLock().lock();
    try (DBIterator iterator = database.iterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      byte[] data = getData(key);
      if (Objects.nonNull(data)) {
        result.add(data);
        i++;
      }
      for (iterator.seek(key); iterator.hasPrev() && i++ < limit; iterator.prev()) {
        result.add(iterator.peekPrev().getValue());
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public long getTotal() throws RuntimeException {
    resetDbLock.readLock().lock();
    try (DBIterator iterator = database.iterator()) {
      long total = 0;
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        total++;
      }
      return total;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  private void updateByBatchInner(Map<byte[], byte[]> rows) throws Exception {
    try (WriteBatch batch = database.createWriteBatch()) {
      rows.forEach((key, value) -> {
        if (value == null) {
          batch.delete(key);
        } else {
          batch.put(key, value);
        }
      });
      database.write(batch, writeOptions);
    }
  }

  private void updateByBatchInner(Map<byte[], byte[]> rows, WriteOptions options) throws Exception {
    try (WriteBatch batch = database.createWriteBatch()) {
      rows.forEach((key, value) -> {
        if (value == null) {
          batch.delete(key);
        } else {
          batch.put(key, value);
        }
      });
      database.write(batch, options);
    }
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows, WriteOptionsWrapper options) {
    resetDbLock.readLock().lock();
    try {
      updateByBatchInner(rows, options.level);
    } catch (Exception e) {
      try {
        updateByBatchInner(rows, options.level);
      } catch (Exception e1) {
        throw new RuntimeException(e);
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows) {
    resetDbLock.readLock().lock();
    try {
      updateByBatchInner(rows);
    } catch (Exception e) {
      try {
        updateByBatchInner(rows);
      } catch (Exception e1) {
        throw new RuntimeException(e);
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public boolean flush() {
    return false;
  }

  @Override
  public void closeDB() {
    resetDbLock.writeLock().lock();
    try {
      if (!isAlive()) {
        return;
      }
      database.close();
      alive = false;
    } catch (IOException e) {
      log.error("Failed to find the dbStore file on the closeDB: {} ", dataBaseName);
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  @Override
  public cn.ledgeryi.chainbase.core.db.common.iterator.DBIterator iterator() {
    return new StoreIterator(database.iterator());
  }

  public Stream<Entry<byte[], byte[]>> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  public Stream<Entry<byte[], byte[]>> parallelStream() {
    return StreamSupport.stream(spliterator(), true);
  }

  @Override
  public LevelDbDataSourceImpl newInstance() {
    return new LevelDbDataSourceImpl(DBConfig.getOutputDirectoryByDbName(dataBaseName), dataBaseName, options, writeOptions);
  }
}
