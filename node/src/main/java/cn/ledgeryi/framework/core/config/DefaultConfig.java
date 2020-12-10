package cn.ledgeryi.framework.core.config;

import cn.ledgeryi.chainbase.core.db.RevokingDatabase;
import cn.ledgeryi.chainbase.core.db.RevokingStore;
import cn.ledgeryi.chainbase.core.db2.core.SnapshotManager;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.db.TransactionCache;
import cn.ledgeryi.framework.core.db.backup.BackupRocksDBAspect;
import cn.ledgeryi.framework.core.db.backup.NeedBeanCondition;

@Slf4j(topic = "app")
@Configuration
@Import(CommonConfig.class)
public class DefaultConfig {

  static {
    RocksDB.loadLibrary();
  }

  @Autowired
  public ApplicationContext appCtx;

  @Autowired
  public CommonConfig commonConfig;

  public DefaultConfig() {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> log.error("Uncaught exception", e));
  }

  @Bean
  public RevokingDatabase revokingDatabase() {
    int dbVersion = Args.getInstance().getStorage().getDbVersion();
    RevokingDatabase revokingDatabase;
    try {
      if (dbVersion == 1) {
        revokingDatabase = RevokingStore.getInstance();
      } else if (dbVersion == 2) {
        revokingDatabase = new SnapshotManager(
            Args.getInstance().getOutputDirectoryByDbName("block"));
      } else {
        throw new RuntimeException("db version is error.");
      }
      return revokingDatabase;
    } finally {
      log.info("key-value data source created.");
    }
  }

  @Bean
  public TransactionCache transactionCache() {
    int dbVersion = Args.getInstance().getStorage().getDbVersion();
    if (dbVersion == 2) {
      return new TransactionCache("trans-cache");
    }

    return null;
  }

  @Bean
  @Conditional(NeedBeanCondition.class)
  public BackupRocksDBAspect backupRocksDBAspect() {
    return new BackupRocksDBAspect();
  }
}
