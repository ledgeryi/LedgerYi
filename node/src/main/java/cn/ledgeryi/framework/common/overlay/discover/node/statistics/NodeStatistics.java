package cn.ledgeryi.framework.common.overlay.discover.node.statistics;

import java.util.concurrent.atomic.AtomicLong;

import cn.ledgeryi.protos.Protocol.ReasonCode;
import lombok.Getter;
import cn.ledgeryi.framework.core.config.args.Args;

public class NodeStatistics {

  public static final int REPUTATION_PREDEFINED = 100000;
  public static final long TOO_MANY_PEERS_PENALIZE_TIMEOUT = 60 * 1000L;
  private static final long CLEAR_CYCLE_TIME = 60 * 60 * 1000L;
  public final MessageStatistics messageStatistics = new MessageStatistics();
  public final MessageCount p2pHandShake = new MessageCount();
  public final MessageCount tcpFlow = new MessageCount();
  public final SimpleStatter discoverMessageLatency;
  public final SimpleStatter pingMessageLatency;
  public final AtomicLong lastPongReplyTime = new AtomicLong(0L); // in milliseconds
  private final long MIN_DATA_LENGTH = Args.getInstance().getReceiveTcpMinDataLength();
  private boolean isPredefined = false;
  private int persistedReputation = 0;
  @Getter
  private int disconnectTimes = 0;
  @Getter
  private ReasonCode ledgerYiLastRemoteDisconnectReason = null;
  @Getter
  private ReasonCode ledgerYiLastLocalDisconnectReason = null;
  private long lastDisconnectedTime = 0;
  private long firstDisconnectedTime = 0;
  private Reputation reputation;

  public NodeStatistics() {
    discoverMessageLatency = new SimpleStatter();
    pingMessageLatency = new SimpleStatter();
    reputation = new Reputation(this);
  }

  public int getReputation() {
    int score = 0;
    if (!isReputationPenalized()) {
      score += persistedReputation / 5 + reputation.calculate();
    }
    if (isPredefined) {
      score += REPUTATION_PREDEFINED;
    }
    return score;
  }

  public ReasonCode getDisconnectReason() {
    if (ledgerYiLastLocalDisconnectReason != null) {
      return ledgerYiLastLocalDisconnectReason;
    }
    if (ledgerYiLastRemoteDisconnectReason != null) {
      return ledgerYiLastRemoteDisconnectReason;
    }
    return ReasonCode.UNKNOWN;
  }

  public boolean isReputationPenalized() {

    if (wasDisconnected() && ledgerYiLastRemoteDisconnectReason == ReasonCode.TOO_MANY_PEERS
        && System.currentTimeMillis() - lastDisconnectedTime < TOO_MANY_PEERS_PENALIZE_TIMEOUT) {
      return true;
    }

    if (wasDisconnected() && ledgerYiLastRemoteDisconnectReason == ReasonCode.DUPLICATE_PEER
        && System.currentTimeMillis() - lastDisconnectedTime < TOO_MANY_PEERS_PENALIZE_TIMEOUT) {
      return true;
    }

    if (firstDisconnectedTime > 0
        && (System.currentTimeMillis() - firstDisconnectedTime) > CLEAR_CYCLE_TIME) {
      ledgerYiLastLocalDisconnectReason = null;
      ledgerYiLastRemoteDisconnectReason = null;
      disconnectTimes = 0;
      persistedReputation = 0;
      firstDisconnectedTime = 0;
    }

    if (ledgerYiLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_PROTOCOL
        || ledgerYiLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_PROTOCOL
        || ledgerYiLastLocalDisconnectReason == ReasonCode.BAD_PROTOCOL
        || ledgerYiLastRemoteDisconnectReason == ReasonCode.BAD_PROTOCOL
        || ledgerYiLastLocalDisconnectReason == ReasonCode.BAD_BLOCK
        || ledgerYiLastRemoteDisconnectReason == ReasonCode.BAD_BLOCK
        || ledgerYiLastLocalDisconnectReason == ReasonCode.BAD_TX
        || ledgerYiLastRemoteDisconnectReason == ReasonCode.BAD_TX
        || ledgerYiLastLocalDisconnectReason == ReasonCode.FORKED
        || ledgerYiLastRemoteDisconnectReason == ReasonCode.FORKED
        || ledgerYiLastLocalDisconnectReason == ReasonCode.UNLINKABLE
        || ledgerYiLastRemoteDisconnectReason == ReasonCode.UNLINKABLE
        || ledgerYiLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN
        || ledgerYiLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN
        || ledgerYiLastRemoteDisconnectReason == ReasonCode.SYNC_FAIL
        || ledgerYiLastLocalDisconnectReason == ReasonCode.SYNC_FAIL
        || ledgerYiLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION
        || ledgerYiLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION) {
      persistedReputation = 0;
      return true;
    }
    return false;
  }

  public void nodeDisconnectedRemote(ReasonCode reason) {
    lastDisconnectedTime = System.currentTimeMillis();
    ledgerYiLastRemoteDisconnectReason = reason;
  }

  public void nodeDisconnectedLocal(ReasonCode reason) {
    lastDisconnectedTime = System.currentTimeMillis();
    ledgerYiLastLocalDisconnectReason = reason;
  }

  public void notifyDisconnect() {
    lastDisconnectedTime = System.currentTimeMillis();
    if (firstDisconnectedTime <= 0) {
      firstDisconnectedTime = lastDisconnectedTime;
    }
    if (ledgerYiLastLocalDisconnectReason == ReasonCode.RESET) {
      return;
    }
    disconnectTimes++;
    persistedReputation = persistedReputation / 2;
  }

  public boolean wasDisconnected() {
    return lastDisconnectedTime > 0;
  }

  public boolean isPredefined() {
    return isPredefined;
  }

  public void setPredefined(boolean isPredefined) {
    this.isPredefined = isPredefined;
  }

  public void setPersistedReputation(int persistedReputation) {
    this.persistedReputation = persistedReputation;
  }

  @Override
  public String toString() {
    return "NodeStat[reput: " + getReputation() + "(" + persistedReputation + "), discover: "
        + messageStatistics.discoverInPong + "/" + messageStatistics.discoverOutPing + " "
        + messageStatistics.discoverOutPong + "/" + messageStatistics.discoverInPing + " "
        + messageStatistics.discoverInNeighbours + "/" + messageStatistics.discoverOutFindNode
        + " "
        + messageStatistics.discoverOutNeighbours + "/" + messageStatistics.discoverInFindNode
        + " "
        + ((int) discoverMessageLatency.getAvrg()) + "ms"
        + ", p2p: " + p2pHandShake + "/" + messageStatistics.p2pInHello + "/"
        + messageStatistics.p2pOutHello + " "
        + ", ledgerYi: " + messageStatistics.ledgerYiInMessage + "/" + messageStatistics.ledgerYiMessage
        + " "
        + (wasDisconnected() ? "X " + disconnectTimes : "")
        + (ledgerYiLastLocalDisconnectReason != null ? ("<=" + ledgerYiLastLocalDisconnectReason) : " ")
        + (ledgerYiLastRemoteDisconnectReason != null ? ("=>" + ledgerYiLastRemoteDisconnectReason) : " ")
        + ", tcp flow: " + tcpFlow.getTotalCount();
  }

  public boolean nodeIsHaveDataTransfer() {
    return tcpFlow.getTotalCount() > MIN_DATA_LENGTH;
  }

  public void resetTcpFlow() {
    tcpFlow.reset();
  }

  public class SimpleStatter {

    private long sum;
    @Getter
    private long count;
    @Getter
    private long last;
    @Getter
    private long min;
    @Getter
    private long max;

    public void add(long value) {
      last = value;
      sum += value;
      min = min == 0 ? value : Math.min(min, value);
      max = Math.max(max, value);
      count++;
    }

    public long getAvrg() {
      return count == 0 ? 0 : sum / count;
    }

  }

}
