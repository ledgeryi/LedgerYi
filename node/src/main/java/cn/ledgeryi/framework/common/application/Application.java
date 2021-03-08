package cn.ledgeryi.framework.common.application;

import cn.ledgeryi.chainbase.core.db.BlockStore;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.db.Manager;

public interface Application {

  //void setOptions(Args args);

  void init(Args args);

  void initServices(Args args);

  void startup();

  void shutdown();

  void startServices();

  void shutdownServices();

  //BlockStore getBlockStoreS();

  void addService(Service service);

  //Manager getDbManager();

}
