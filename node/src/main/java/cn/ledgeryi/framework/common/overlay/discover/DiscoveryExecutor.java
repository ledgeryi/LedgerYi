package cn.ledgeryi.framework.common.overlay.discover;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;
import cn.ledgeryi.framework.common.overlay.discover.table.KademliaOptions;

public class DiscoveryExecutor {

  private ScheduledExecutorService discoverer = Executors.newSingleThreadScheduledExecutor();
  private ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor();

  private NodeManager nodeManager;

  public DiscoveryExecutor(NodeManager nodeManager) {
    this.nodeManager = nodeManager;
  }

  public void start() {
    discoverer.scheduleWithFixedDelay(
        new DiscoverTask(nodeManager), 1, KademliaOptions.DISCOVER_CYCLE, TimeUnit.SECONDS);

    refresher.scheduleWithFixedDelay(
        new RefreshTask(nodeManager), 1, KademliaOptions.BUCKET_REFRESH, TimeUnit.MILLISECONDS);
  }

  public void close() {
    discoverer.shutdownNow();
    refresher.shutdownNow();
  }
}
