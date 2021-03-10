package cn.ledgeryi.framework.common.application;

import cn.ledgeryi.chainbase.core.db.BlockStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.consensus.ConsensusService;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.framework.core.net.LedgerYiNetService;

@Slf4j(topic = "app")
@Component
public class ApplicationImpl implements Application {

  //private BlockStore blockStoreDb;
  private ServiceContainer services;

  @Autowired
  private LedgerYiNetService ledgerYiNetService;

  @Autowired
  private Manager dbManager;

  @Autowired
  private ConsensusService consensusService;

  /*@Override
  public void setOptions(Args args) {
    // not used
  }*/

  @Override
  @Autowired
  public void init(Args args) {
    //blockStoreDb = dbManager.getBlockStore();
    services = new ServiceContainer();
  }

  @Override
  public void addService(Service service) {
    services.add(service);
  }

  @Override
  public void initServices(Args args) {
    //services.init(args);
  }

  /**
   * start up the app：p2p and consensus
   */
  public void startup() {
    ledgerYiNetService.start();
    consensusService.start();
  }

  @Override
  public void shutdown() {
    log.info("******** begin to shutdown ********");
    ledgerYiNetService.stop();
    consensusService.stop();
    synchronized (dbManager.getRevokingStore()) {
      closeRevokingStore();
      closeAllStore();
    }
    dbManager.stopRepushThread();
    log.info("******** end to shutdown ********");
  }

  @Override
  public void startServices() {
    services.start();
  }

  @Override
  public void shutdownServices() {
    services.stop();
  }

  /*@Override
  public BlockStore getBlockStoreS() {
    return blockStoreDb;
  }*/

  /*@Override
  public Manager getDbManager() {
    return dbManager;
  }*/

  private void closeRevokingStore() {
    log.info("******** begin to closeRevokingStore ********");
    dbManager.getRevokingStore().shutdown();
  }

  private void closeAllStore() {
    dbManager.closeAllStore();
  }

}
