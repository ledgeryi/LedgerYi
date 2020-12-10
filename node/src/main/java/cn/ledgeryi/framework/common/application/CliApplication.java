package cn.ledgeryi.framework.common.application;

import cn.ledgeryi.chainbase.core.db.BlockStore;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.db.Manager;

public class CliApplication implements Application {

  @Override
  public void setOptions(Args args) {

  }

  @Override
  public void init(Args args) {

  }

  @Override
  public void initServices(Args args) {

  }

  @Override
  public void startup() {

  }

  @Override
  public void shutdown() {

  }

  @Override
  public void startServices() {

  }

  @Override
  public void shutdownServices() {

  }

  @Override
  public BlockStore getBlockStoreS() {
    return null;
  }

  @Override
  public void addService(Service service) {

  }

  @Override
  public Manager getDbManager() {
    return null;
  }

}
