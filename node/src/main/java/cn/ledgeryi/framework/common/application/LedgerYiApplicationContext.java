package cn.ledgeryi.framework.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import cn.ledgeryi.framework.common.overlay.discover.DiscoverServer;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;
import cn.ledgeryi.framework.common.overlay.server.ChannelManager;
import cn.ledgeryi.framework.core.db.Manager;

public class LedgerYiApplicationContext extends AnnotationConfigApplicationContext {

  public LedgerYiApplicationContext() {
  }

  public LedgerYiApplicationContext(DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
  }

  public LedgerYiApplicationContext(Class<?>... annotatedClasses) {
    super(annotatedClasses);
  }

  public LedgerYiApplicationContext(String... basePackages) {
    super(basePackages);
  }

  @Override
  public void destroy() {

    Application appT = ApplicationFactory.create(this);
    appT.shutdownServices();
    appT.shutdown();

    DiscoverServer discoverServer = getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = getBean(NodeManager.class);
    nodeManager.close();

    Manager dbManager = getBean(Manager.class);
    dbManager.stopRepushThread();
    super.destroy();
  }
}
