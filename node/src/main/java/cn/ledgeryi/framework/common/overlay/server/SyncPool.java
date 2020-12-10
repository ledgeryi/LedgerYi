package cn.ledgeryi.framework.common.overlay.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.overlay.client.PeerClient;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeHandler;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;

@Slf4j(topic = "net")
@Component
public class SyncPool {

  private final double factor = Args.getInstance().getConnectFactor();
  private final AtomicInteger passivePeersCount = new AtomicInteger(0);
  private final AtomicInteger activePeersCount = new AtomicInteger(0);
  private final List<PeerConnection> activePeers = Collections.synchronizedList(new ArrayList<PeerConnection>());
  private final double activeFactor = Args.getInstance().getActiveConnectFactor();
  private final Cache<NodeHandler, Long> nodeHandlerCache =
          CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(180, TimeUnit.SECONDS).recordStats().build();

  @Autowired
  private NodeManager nodeManager;
  @Autowired
  private ApplicationContext ctx;

  private PeerClient peerClient;
  private ChannelManager channelManager;
  private final Args args = Args.getInstance();
  private final int maxActiveNodes = args.getNodeMaxActiveNodes();
  private final int maxActivePeersWithSameIp = args.getNodeMaxActiveNodesWithSameIp();
  private final ScheduledExecutorService poolLoopExecutor = Executors.newSingleThreadScheduledExecutor();
  private final ScheduledExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();

  public void init() {
    channelManager = ctx.getBean(ChannelManager.class);
    peerClient = ctx.getBean(PeerClient.class);

    poolLoopExecutor.scheduleWithFixedDelay(() -> {
      try {
        fillUp();
      } catch (Throwable t) {
        log.error("Exception in sync worker", t);
      }
    }, 30000, 3600, TimeUnit.MILLISECONDS);

    logExecutor.scheduleWithFixedDelay(() -> {
      try {
        logActivePeers();
      } catch (Throwable t) {
      }
    }, 30, 10, TimeUnit.SECONDS);
  }

  private void fillUp() {
    List<NodeHandler> connectNodes = new ArrayList<>();
    Set<InetAddress> addressInUse = new HashSet<>();
    Set<String> nodesInUse = new HashSet<>();
    channelManager.getActivePeers().forEach(channel -> {
      nodesInUse.add(channel.getPeerId());
      addressInUse.add(channel.getInetAddress());
    });

    channelManager.getActiveNodes().forEach((address, node) -> {
      nodesInUse.add(node.getHexId());
      if (!addressInUse.contains(address)) {
        connectNodes.add(nodeManager.getNodeHandler(node));
      }
    });

    /*Map<InetAddress, Node> activeNodes = channelManager.getActiveNodes();
    Set<InetAddress> inetAddresses = activeNodes.keySet();*/

    int size = Math.max((int) (maxActiveNodes * factor) - activePeers.size(), (int) (maxActiveNodes * activeFactor - activePeersCount.get()));
    int lackSize = size - connectNodes.size();
    if (lackSize > 0) {
      nodesInUse.add(nodeManager.getPublicHomeNode().getHexId());
      List<NodeHandler> newNodes = nodeManager.getNodes(new NodeSelector(nodesInUse), lackSize);
      connectNodes.addAll(newNodes);
    }

    connectNodes.forEach(n -> {
      peerClient.connectAsync(n, false);
      nodeHandlerCache.put(n, System.currentTimeMillis());
    });
  }

  synchronized void logActivePeers() {
    String str = String.format("\n\nPeer stats: all %d, active %d, passive %d\n\n",
        channelManager.getActivePeers().size(), activePeersCount.get(), passivePeersCount.get());
    StringBuilder sb = new StringBuilder(str);
    for (PeerConnection peer : new ArrayList<>(activePeers)) {
      sb.append(peer.log()).append('\n');
    }
    log.info(sb.toString());
  }

  public List<PeerConnection> getActivePeers() {
    List<PeerConnection> peers = Lists.newArrayList();
    activePeers.forEach(peer -> {
      if (!peer.isDisconnect()) {
        peers.add(peer);
      }
    });
    return peers;
  }

  public synchronized void onConnect(Channel peer) {
    PeerConnection peerConnection = (PeerConnection) peer;
    if (!activePeers.contains(peerConnection)) {
      if (!peerConnection.isActive()) {
        passivePeersCount.incrementAndGet();
      } else {
        activePeersCount.incrementAndGet();
      }
      activePeers.add(peerConnection);
      activePeers.sort(Comparator.comparingDouble(c -> c.getNodeStatistics().pingMessageLatency.getAvrg()));
      peerConnection.onConnect();
    }
  }

  public synchronized void onDisconnect(Channel peer) {
    PeerConnection peerConnection = (PeerConnection) peer;
    if (activePeers.contains(peerConnection)) {
      if (!peerConnection.isActive()) {
        passivePeersCount.decrementAndGet();
      } else {
        activePeersCount.decrementAndGet();
      }
      activePeers.remove(peerConnection);
      peerConnection.onDisconnect();
    }
  }

  public boolean isCanConnect() {
    return passivePeersCount.get() < maxActiveNodes * (1 - activeFactor);
  }

  public void close() {
    try {
      poolLoopExecutor.shutdownNow();
      logExecutor.shutdownNow();
    } catch (Exception e) {
      log.warn("Problems shutting down executor", e);
    }
  }

  public AtomicInteger getPassivePeersCount() {
    return passivePeersCount;
  }

  public AtomicInteger getActivePeersCount() {
    return activePeersCount;
  }

  class NodeSelector implements Predicate<NodeHandler> {

    private Set<String> nodesInUse;

    public NodeSelector(Set<String> nodesInUse) {
      this.nodesInUse = nodesInUse;
    }

    @Override
    public boolean test(NodeHandler handler) {
      InetAddress inetAddress = handler.getInetSocketAddress().getAddress();
      return !((handler.getNode().getHost().equals(nodeManager.getPublicHomeNode().getHost()) &&
          handler.getNode().getPort() == nodeManager.getPublicHomeNode().getPort())
          || (channelManager.getRecentlyDisconnected().getIfPresent(inetAddress) != null)
          || (channelManager.getBadPeers().getIfPresent(inetAddress) != null)
          || (channelManager.getConnectionNum(inetAddress) >= maxActivePeersWithSameIp)
          || (nodesInUse.contains(handler.getNode().getHexId()))
          || (nodeHandlerCache.getIfPresent(handler) != null));
    }
  }

}
