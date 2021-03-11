package cn.ledgeryi.framework.common.overlay.server;

import cn.ledgeryi.common.core.db.ByteArrayWrapper;
import cn.ledgeryi.protos.Protocol;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.overlay.client.PeerClient;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.core.config.args.Args;

import static cn.ledgeryi.protos.Protocol.ReasonCode.*;

@Slf4j(topic = "net")
@Component
public class ChannelManager {

  @Autowired
  private SyncPool syncPool;
  @Autowired
  private PeerServer peerServer;
  @Autowired
  private PeerClient peerClient;
  @Autowired
  private FastForward fastForward;

  private final Args args = Args.getInstance();
  private final int maxActivePeers = args.getNodeMaxActiveNodes();
  private final Map<ByteArrayWrapper, Channel> activePeers = new ConcurrentHashMap<>();
  private final int getMaxActivePeersWithSameIp = args.getNodeMaxActiveNodesWithSameIp();

  private final Cache<InetAddress, Protocol.ReasonCode> badPeers = CacheBuilder.newBuilder().maximumSize(10000)
      .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

  private final Cache<InetAddress, Protocol.ReasonCode> recentlyDisconnected = CacheBuilder.newBuilder()
      .maximumSize(1000).expireAfterWrite(30, TimeUnit.SECONDS).recordStats().build();

  @Getter
  private final Cache<InetAddress, Node> trustNodes = CacheBuilder.newBuilder().maximumSize(100).build();
  @Getter
  private final Map<InetSocketAddress, Node> activeNodes = new ConcurrentHashMap<>();
  @Getter
  private final Map<InetAddress, Node> fastForwardNodes = new ConcurrentHashMap<>();

  public void init() {
    if (this.args.getNodeListenPort() > 0) {
      //Open port(node.listen.port) to listen for connection request
      new Thread(() -> peerServer.start(Args.getInstance().getNodeListenPort()), "PeerServerThread").start();
    }

    InetAddress address;
    for (Node node : args.getPassiveNodes()) {
      address = new InetSocketAddress(node.getHost(), node.getPort()).getAddress();
      trustNodes.put(address, node);
    }

    for (Node node : args.getActiveNodes()) {
      InetSocketAddress socketAddress = new InetSocketAddress(node.getHost(), node.getPort());
      address = socketAddress.getAddress();
      trustNodes.put(address, node);
      activeNodes.put(socketAddress, node);
    }

    for (Node node : args.getFastForwardNodes()) {
      address = new InetSocketAddress(node.getHost(), node.getPort()).getAddress();
      trustNodes.put(address, node);
      fastForwardNodes.put(address, node);
    }

    log.info("Node config, trust {}, active {}, forward {}.", trustNodes.size(), activeNodes.size(), fastForwardNodes.size());

    syncPool.init();
    //fastForward.init();
  }

  public void putNewNode(Node newNode){
    //InetAddress address = new InetSocketAddress(newNode.getHost(), newNode.getPort()).getAddress();
    InetSocketAddress socketAddress = new InetSocketAddress(newNode.getHost(), newNode.getPort());
    activeNodes.put(socketAddress, newNode);
  }

  public void processDisconnect(Channel channel, Protocol.ReasonCode reason) {
    InetAddress inetAddress = channel.getInetAddress();
    if (inetAddress == null) {
      return;
    }
    switch (reason) {
      case BAD_PROTOCOL:
      case BAD_BLOCK:
      case BAD_TX:
        badPeers.put(channel.getInetAddress(), reason);
        break;
      default:
        recentlyDisconnected.put(channel.getInetAddress(), reason);
        break;
    }
  }

  public void notifyDisconnect(Channel channel) {
    syncPool.onDisconnect(channel);
    activePeers.values().remove(channel);
    if (channel != null) {
      if (channel.getNodeStatistics() != null) {
        channel.getNodeStatistics().notifyDisconnect();
      }
      InetAddress inetAddress = channel.getInetAddress();
      if (inetAddress != null && recentlyDisconnected.getIfPresent(inetAddress) == null) {
        recentlyDisconnected.put(channel.getInetAddress(), UNKNOWN);
      }
    }
  }

  public synchronized boolean processPeer(Channel peer) {

    if (trustNodes.getIfPresent(peer.getInetAddress()) == null) {
      if (recentlyDisconnected.getIfPresent(peer) != null) {
        log.info("Peer {} recently disconnected.", peer.getInetAddress());
        return false;
      }

      if (badPeers.getIfPresent(peer) != null) {
        peer.disconnect(peer.getNodeStatistics().getDisconnectReason());
        return false;
      }

      if (!peer.isActive() && activePeers.size() >= maxActivePeers) {
        peer.disconnect(TOO_MANY_PEERS);
        return false;
      }

      if (getConnectionNum(peer.getInetAddress()) >= getMaxActivePeersWithSameIp) {
        peer.disconnect(TOO_MANY_PEERS_WITH_SAME_IP);
        return false;
      }
    }

    Channel channel = activePeers.get(peer.getNodeIdWrapper());
    if (channel != null) {
      if (channel.getStartTime() > peer.getStartTime()) {
        log.info("Disconnect connection established later, {}", channel.getNode());
        channel.disconnect(DUPLICATE_PEER);
      } else {
        peer.disconnect(DUPLICATE_PEER);
        return false;
      }
    }
    activePeers.put(peer.getNodeIdWrapper(), peer);
    log.info("Add active peer {}, total active peers: {}", peer, activePeers.size());
    return true;
  }

  public int getConnectionNum(InetAddress inetAddress) {
    int cnt = 0;
    for (Channel channel : activePeers.values()) {
      if (channel.getInetAddress().equals(inetAddress)) {
        cnt++;
      }
    }
    return cnt;
  }

  public Collection<Channel> getActivePeers() {
    return activePeers.values();
  }

  public Cache<InetAddress, Protocol.ReasonCode> getRecentlyDisconnected() {
    return this.recentlyDisconnected;
  }

  public Cache<InetAddress, Protocol.ReasonCode> getBadPeers() {
    return this.badPeers;
  }

  public void close() {
    peerServer.close();
    peerClient.close();
    syncPool.close();
  }
}
