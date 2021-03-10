package cn.ledgeryi.framework.common.overlay.discover.node;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.net.udp.handler.EventHandler;
import cn.ledgeryi.framework.common.net.udp.handler.UdpEvent;
import cn.ledgeryi.framework.common.net.udp.message.Message;
import cn.ledgeryi.framework.common.net.udp.message.discover.FindNodeMessage;
import cn.ledgeryi.framework.common.net.udp.message.discover.NeighborsMessage;
import cn.ledgeryi.framework.common.net.udp.message.discover.PingMessage;
import cn.ledgeryi.framework.common.net.udp.message.discover.PongMessage;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeHandler.State;
import cn.ledgeryi.framework.common.overlay.discover.node.statistics.NodeStatistics;
import cn.ledgeryi.framework.common.overlay.discover.table.NodeTable;
import cn.ledgeryi.framework.common.utils.CollectionUtils;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.db.Manager;

@Slf4j(topic = "discover")
@Component
public class NodeManager implements EventHandler {

  private static final int MAX_NODES = 2000;
  private static final int MAX_NODES_WRITE_TO_DB = 30;
  private static final int NODES_TRIM_THRESHOLD = 3000;
  private static final long DB_COMMIT_RATE = 1 * 60 * 1000L;

  private Node homeNode;
  private NodeTable table;
  private Manager dbManager;
  private Consumer<UdpEvent> messageSender;
  private ScheduledExecutorService pongTimer;

  private Args args = Args.getInstance();
  private final List<Node> bootNodes = new ArrayList<>();
  private final Timer nodeManagerTasksTimer = new Timer("NodeManagerTasks");
  private final Map<String, NodeHandler> nodeHandlerMap = new ConcurrentHashMap<>();

  private volatile boolean inited = false;
  private volatile boolean discoveryEnabled;

  @Autowired
  public NodeManager(Manager dBManager) {
    this.dbManager = dBManager;
    discoveryEnabled = args.isNodeDiscoveryEnable();
    for (String boot : args.getSeedNode().getIpList()) {
      bootNodes.add(Node.instanceOf(boot));
    }
    homeNode = new Node(Node.getNodeId(), args.getNodeExternalIp(), args.getNodeListenPort());
    log.info("homeNode : {}", homeNode);
    // create a routing table
    table = new NodeTable(homeNode);
    this.pongTimer = Executors.newSingleThreadScheduledExecutor();
  }

  public ScheduledExecutorService getPongTimer() {
    return pongTimer;
  }

  @Override
  public void channelActivated() {
    if (!inited) {
      inited = true;

      if (args.isNodeDiscoveryPersist()) {
        dbRead();
        nodeManagerTasksTimer.scheduleAtFixedRate(new TimerTask() {
          @Override
          public void run() {
            dbWrite();
          }
        }, DB_COMMIT_RATE, DB_COMMIT_RATE);
      }

      for (Node node : bootNodes) {
        getNodeHandler(node);
      }
    }
  }

  private boolean isNodeAlive(NodeHandler nodeHandler) {
    return nodeHandler.getState().equals(State.ALIVE)
        || nodeHandler.getState().equals(State.ACTIVE)
        || nodeHandler.getState().equals(State.EVICTCANDIDATE);
  }

  private void dbRead() {
    Set<Node> nodes = this.dbManager.readNeighbours();
    log.info("Reading Node statistics from PeersStore: " + nodes.size() + " nodes.");
    nodes.forEach(node -> getNodeHandler(node).getNodeStatistics()
        .setPersistedReputation(node.getReputation()));
  }

  private void dbWrite() {
    List<Node> batch = new ArrayList<>();
    for (NodeHandler nodeHandler : nodeHandlerMap.values()) {
      if (nodeHandler.getNode().isConnectible()) {
        nodeHandler.getNode().setReputation(nodeHandler.getNodeStatistics().getReputation());
        batch.add(nodeHandler.getNode());
      }
    }
    int size = batch.size();
    batch.sort(Comparator.comparingInt(value -> -value.getReputation()));
    if (batch.size() > MAX_NODES_WRITE_TO_DB) {
      batch = batch.subList(0, MAX_NODES_WRITE_TO_DB);
    }
    Set<Node> nodes = new HashSet<>();
    nodes.addAll(batch);
    /*log.info("Write Node statistics to PeersStore after: m:{}/t:{}/{}/{} nodes.",
        nodeHandlerMap.size(), getTable().getAllNodes().size(), size, nodes.size());*/
    dbManager.clearAndWriteNeighbours(nodes);
  }

  public void setMessageSender(Consumer<UdpEvent> messageSender) {
    this.messageSender = messageSender;
  }

  private String getKey(Node n) {
    return getKey(new InetSocketAddress(n.getHost(), n.getPort()));
  }

  private String getKey(InetSocketAddress address) {
    InetAddress addr = address.getAddress();
    return (addr == null ? address.getHostString() : addr.getHostAddress()) + ":" + address
        .getPort();
  }

  public NodeHandler getNodeHandler(Node n) {
    String key = getKey(n);
    NodeHandler ret = nodeHandlerMap.get(key);
    if (ret == null) {
      trimTable();
      ret = new NodeHandler(n, this);
      nodeHandlerMap.put(key, ret);
    } else if (ret.getNode().isDiscoveryNode() && !n.isDiscoveryNode()) {
      ret.setNode(n);
    }
    return ret;
  }

  private void trimTable() {
    if (nodeHandlerMap.size() > NODES_TRIM_THRESHOLD) {
      nodeHandlerMap.values().forEach(handler -> {
        if (!handler.getNode().isConnectible()) {
          nodeHandlerMap.remove(handler);
        }
      });
    }
    if (nodeHandlerMap.size() > NODES_TRIM_THRESHOLD) {
      List<NodeHandler> sorted = new ArrayList<>(nodeHandlerMap.values());
      sorted.sort(Comparator.comparingInt(o -> o.getNodeStatistics().getReputation()));
      for (NodeHandler handler : sorted) {
        nodeHandlerMap.values().remove(handler);
        if (nodeHandlerMap.size() <= MAX_NODES) {
          break;
        }
      }
    }
  }

  public boolean hasNodeHandler(Node n) {
    return nodeHandlerMap.containsKey(getKey(n));
  }

  public NodeTable getTable() {
    return table;
  }

  public NodeStatistics getNodeStatistics(Node n) {
    return getNodeHandler(n).getNodeStatistics();
  }

  @Override
  public void handleEvent(UdpEvent udpEvent) {
    Message m = udpEvent.getMessage();

    InetSocketAddress sender = udpEvent.getAddress();
    Node n = new Node(m.getFrom().getId(), sender.getHostString(), sender.getPort(), m.getFrom().getPort());
    NodeHandler nodeHandler = getNodeHandler(n);

    nodeHandler.getNodeStatistics().messageStatistics.addUdpInMessage(m.getType());
    switch (m.getType()) {
      case DISCOVER_PING:
        nodeHandler.handlePing((PingMessage) m);
        break;
      case DISCOVER_PONG:
        nodeHandler.handlePong((PongMessage) m);
        break;
      case DISCOVER_FIND_NODE:
        nodeHandler.handleFindNode((FindNodeMessage) m);
        break;
      case DISCOVER_NEIGHBORS:
        nodeHandler.handleNeighbours((NeighborsMessage) m);
        break;
      default:
        break;
    }
  }

  public void sendOutbound(UdpEvent udpEvent) {
    if (discoveryEnabled && messageSender != null) {
      messageSender.accept(udpEvent);
    }
  }

  public List<NodeHandler> getNodes(Predicate<NodeHandler> predicate, int limit) {
    List<NodeHandler> filtered = new ArrayList<>();
    for (NodeHandler handler : nodeHandlerMap.values()) {
      if (handler.getNode().isConnectible() && predicate.test(handler)) {
        filtered.add(handler);
      }
    }
    filtered.sort(Comparator.comparingInt(handler -> -handler.getNodeStatistics().getReputation()));
    return CollectionUtils.truncate(filtered, limit);
  }

  public List<NodeHandler> dumpActiveNodes() {
    List<NodeHandler> handlers = new ArrayList<>();
    for (NodeHandler handler : this.nodeHandlerMap.values()) {
      if (isNodeAlive(handler)) {
        handlers.add(handler);
      }
    }
    return handlers;
  }

  public Node getPublicHomeNode() {
    return homeNode;
  }

  public void close() {
    try {
      nodeManagerTasksTimer.cancel();
      pongTimer.shutdownNow();
    } catch (Exception e) {
      log.warn("close failed.", e);
    }
  }

}
