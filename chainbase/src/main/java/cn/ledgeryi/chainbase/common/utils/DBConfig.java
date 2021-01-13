package cn.ledgeryi.chainbase.common.utils;

import cn.ledgeryi.chainbase.common.storage.rocksdb.RocksDbSettings;
import cn.ledgeryi.chainbase.core.config.args.GenesisBlock;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;

import java.io.File;
import java.util.Map;
import java.util.Set;


public class DBConfig {

  private static final CompressionType DEFAULT_COMPRESSION_TYPE = CompressionType.SNAPPY;
  private static final int DEFAULT_BLOCK_SIZE = 4 * 1024;
  private static final int DEFAULT_WRITE_BUFFER_SIZE = 10 * 1024 * 1024;
  private static final long DEFAULT_CACHE_SIZE = 10 * 1024 * 1024L;
  private static final int DEFAULT_MAX_OPEN_FILES = 100;
  @Getter
  @Setter
  private static int dbVersion;
  @Getter
  @Setter
  private static String dbEngine;
  @Getter
  @Setter
  private static String outputDirectoryConfig;
  @Getter
  @Setter
  private static Map<String, Property> propertyMap;
  @Getter
  @Setter
  private static GenesisBlock genesisBlock;
  @Getter
  @Setter
  private static boolean dbSync;
  @Getter
  @Setter
  private static RocksDbSettings rocksDbSettings;
  @Getter
  @Setter
  private static String blockTimestamp;
  @Getter
  @Setter
  private static String dbDirectory;
  @Getter
  @Setter
  private static boolean debug;
  @Getter
  @Setter
  private static double minTimeRatio;
  @Getter
  @Setter
  private static double maxTimeRatio;
  @Getter
  @Setter
  private static int validContractProtoThreadNum;
  @Getter
  @Setter
  private static int longRunningTime;
  @Getter
  @Setter
  private static long changedDelegation;

  @Getter
  @Setter
  private static Set<String> actuatorSet;

  @Getter
  @Setter
  private static boolean isECKeyCryptoEngine = true;

  public static String getOutputDirectoryByDbName(String dbName) {
    String path = getPathByDbName(dbName);
    if (!StringUtils.isBlank(path)) {
      return path;
    }
    return getOutputDirectory();
  }

  public static String getPathByDbName(String dbName) {
    if (hasProperty(dbName)) {
      return getProperty(dbName).getPath();
    }
    return null;
  }

  private static boolean hasProperty(String dbName) {
    if (propertyMap != null) {
      return propertyMap.containsKey(dbName);
    }
    return false;
  }

  private static Property getProperty(String dbName) {
    return propertyMap.get(dbName);
  }

  public static String getOutputDirectory() {
    if (!outputDirectoryConfig.equals("") && !outputDirectoryConfig.endsWith(File.separator)) {
      return outputDirectoryConfig + File.separator;
    }
    return outputDirectoryConfig;
  }

  public static Options getOptionsByDbName(String dbName) {
    if (hasProperty(dbName)) {
      return getProperty(dbName).getDbOptions();
    }
    return createDefaultDbOptions();
  }

  private static Options createDefaultDbOptions() {
    Options dbOptions = new Options();
    dbOptions.createIfMissing(true);
    dbOptions.paranoidChecks(true);
    dbOptions.verifyChecksums(true);
    dbOptions.compressionType(DEFAULT_COMPRESSION_TYPE);
    dbOptions.blockSize(DEFAULT_BLOCK_SIZE);
    dbOptions.writeBufferSize(DEFAULT_WRITE_BUFFER_SIZE);
    dbOptions.cacheSize(DEFAULT_CACHE_SIZE);
    dbOptions.maxOpenFiles(DEFAULT_MAX_OPEN_FILES);
    return dbOptions;
  }
}
