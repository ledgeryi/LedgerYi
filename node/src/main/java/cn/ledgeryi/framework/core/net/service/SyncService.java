package cn.ledgeryi.framework.core.net.service;


import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule.BlockId;
import cn.ledgeryi.chainbase.core.config.Parameter;
import cn.ledgeryi.common.core.exception.P2pException;
import cn.ledgeryi.common.utils.Pair;
import cn.ledgeryi.protos.Protocol;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.overlay.server.Channel.LedgerYiState;
import cn.ledgeryi.framework.core.net.LedgerYiNetDelegate;
import cn.ledgeryi.framework.core.net.message.BlockMessage;
import cn.ledgeryi.framework.core.net.message.FetchInvDataMessage;
import cn.ledgeryi.framework.core.net.message.SyncBlockChainMessage;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;

import static cn.ledgeryi.chainbase.core.config.Parameter.NetConstants.MAX_BLOCK_FETCH_PER_PEER;

@Slf4j(topic = "net")
@Component
public class SyncService {

  @Autowired
  private LedgerYiNetDelegate ledgerYiNetDelegate;

  private Map<BlockMessage, PeerConnection> blockWaitToProcess = new ConcurrentHashMap<>();

  private Map<BlockMessage, PeerConnection> blockJustReceived = new ConcurrentHashMap<>();

  private Cache<BlockId, Long> requestBlockIds = CacheBuilder.newBuilder().maximumSize(10_000)
      .expireAfterWrite(1, TimeUnit.HOURS).initialCapacity(10_000)
      .recordStats().build();

  private ScheduledExecutorService fetchExecutor = Executors.newSingleThreadScheduledExecutor();

  private ScheduledExecutorService blockHandleExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private volatile boolean handleFlag = false;

  @Setter
  private volatile boolean fetchFlag = false;

  public void init() {
    fetchExecutor.scheduleWithFixedDelay(() -> {
      try {
        if (fetchFlag) {
          fetchFlag = false;
          startFetchSyncBlock();
        }
      } catch (Exception e) {
        log.error("Fetch sync block error.", e);
      }
    }, 10, 1, TimeUnit.SECONDS);

    blockHandleExecutor.scheduleWithFixedDelay(() -> {
      try {
        if (handleFlag) {
          handleFlag = false;
          handleSyncBlock();
        }
      } catch (Exception e) {
        log.error("Handle sync block error.", e);
      }
    }, 10, 1, TimeUnit.SECONDS);
  }

  public void close() {
    fetchExecutor.shutdown();
    blockHandleExecutor.shutdown();
  }

  public void startSync(PeerConnection peer) {
    peer.setJingCHainState(LedgerYiState.SYNCING);
    peer.setNeedSyncFromPeer(true);
    peer.getSyncBlockToFetch().clear();
    peer.setRemainNum(0);
    peer.setBlockBothHave(ledgerYiNetDelegate.getGenesisBlockId());
    syncNext(peer);
  }

  public void syncNext(PeerConnection peer) {
    try {
      if (peer.getSyncChainRequested() != null) {
        log.warn("Peer {} is in sync.", peer.getNode().getHost());
        return;
      }
      LinkedList<BlockId> chainSummary = getBlockChainSummary(peer);
      peer.setSyncChainRequested(new Pair<>(chainSummary, System.currentTimeMillis()));
      peer.sendMessage(new SyncBlockChainMessage(chainSummary));
    } catch (Exception e) {
      log.error("Peer {} sync failed, reason: {}", peer.getInetAddress(), e.getMessage());
      peer.disconnect(Protocol.ReasonCode.SYNC_FAIL);
    }
  }

  public void processBlock(PeerConnection peer, BlockMessage blockMessage) {
    synchronized (blockJustReceived) {
      blockJustReceived.put(blockMessage, peer);
    }
    handleFlag = true;
    if (peer.isIdle()) {
      if (peer.getRemainNum() > 0
          && peer.getSyncBlockToFetch().size() <= Parameter.NodeConstant.SYNC_FETCH_BATCH_NUM) {
        syncNext(peer);
      } else {
        fetchFlag = true;
      }
    }
  }

  public void onDisconnect(PeerConnection peer) {
    if (!peer.getSyncBlockRequested().isEmpty()) {
      peer.getSyncBlockRequested().keySet().forEach(blockId -> invalid(blockId));
    }
  }

  private void invalid(BlockId blockId) {
    requestBlockIds.invalidate(blockId);
    fetchFlag = true;
  }

  private LinkedList<BlockId> getBlockChainSummary(PeerConnection peer) throws P2pException {
    BlockId beginBlockId = peer.getBlockBothHave();
    List<BlockId> blockIds = new ArrayList<>(peer.getSyncBlockToFetch());
    List<BlockId> forkList = new LinkedList<>();
    LinkedList<BlockId> summary = new LinkedList<>();
    long syncBeginNumber = ledgerYiNetDelegate.getSyncBeginNumber();
    long low = syncBeginNumber < 0 ? 0 : syncBeginNumber;
    long highNoFork;
    long high;

    if (beginBlockId.getNum() == 0) {
      highNoFork = high = ledgerYiNetDelegate.getHeadBlockId().getNum();
    } else {
      if (ledgerYiNetDelegate.containBlockInMainChain(beginBlockId)) {
        highNoFork = high = beginBlockId.getNum();
      } else {
        forkList = ledgerYiNetDelegate.getBlockChainHashesOnFork(beginBlockId);
        if (forkList.isEmpty()) {
          throw new P2pException(P2pException.TypeEnum.SYNC_FAILED, "can't find blockId: " + beginBlockId.getString());
        }
        highNoFork = ((LinkedList<BlockId>) forkList).peekLast().getNum();
        ((LinkedList) forkList).pollLast();
        Collections.reverse(forkList);
        high = highNoFork + forkList.size();
      }
    }

    if (low > highNoFork) {
      throw new P2pException(P2pException.TypeEnum.SYNC_FAILED, "low: " + low + " gt highNoFork: " + highNoFork);
    }

    long realHigh = high + blockIds.size();
    log.info("Get block chain summary, low: {}, highNoFork: {}, high: {}, realHigh: {}", low, highNoFork, high, realHigh);
    while (low <= realHigh) {
      if (low <= highNoFork) {
        summary.offer(ledgerYiNetDelegate.getBlockIdByNum(low));
      } else if (low <= high) {
        summary.offer(forkList.get((int) (low - highNoFork - 1)));
      } else {
        summary.offer(blockIds.get((int) (low - high - 1)));
      }
      low += (realHigh - low + 2) / 2;
    }

    return summary;
  }

  private void startFetchSyncBlock() {
    HashMap<PeerConnection, List<BlockId>> send = new HashMap<>();

    ledgerYiNetDelegate.getActivePeer().stream()
        .filter(peer -> peer.isNeedSyncFromPeer() && peer.isIdle())
        .forEach(peer -> {
          if (!send.containsKey(peer)) {
            send.put(peer, new LinkedList<>());
          }
          for (BlockId blockId : peer.getSyncBlockToFetch()) {
            if (requestBlockIds.getIfPresent(blockId) == null) {
              requestBlockIds.put(blockId, System.currentTimeMillis());
              peer.getSyncBlockRequested().put(blockId, System.currentTimeMillis());
              send.get(peer).add(blockId);
              if (send.get(peer).size() >= MAX_BLOCK_FETCH_PER_PEER) {
                break;
              }
            }
          }
        });

    send.forEach((peer, blockIds) -> {
      if (!blockIds.isEmpty()) {
        peer.sendMessage(new FetchInvDataMessage(new LinkedList<>(blockIds), Protocol.Inventory.InventoryType.BLOCK));
      }
    });
  }

  private synchronized void handleSyncBlock() {
    synchronized (blockJustReceived) {
      blockWaitToProcess.putAll(blockJustReceived);
      blockJustReceived.clear();
    }
    final boolean[] isProcessed = {true};

    while (isProcessed[0]) {
      isProcessed[0] = false;
      synchronized (ledgerYiNetDelegate.getBlockLock()) {
        blockWaitToProcess.forEach((msg, peerConnection) -> {
          if (peerConnection.isDisconnect()) {
            blockWaitToProcess.remove(msg);
            invalid(msg.getBlockId());
            return;
          }
          final boolean[] isFound = {false};
          ledgerYiNetDelegate.getActivePeer().stream()
              .filter(peer -> msg.getBlockId().equals(peer.getSyncBlockToFetch().peek()))
              .forEach(peer -> {
                peer.getSyncBlockToFetch().pop();
                peer.getSyncBlockInProcess().add(msg.getBlockId());
                isFound[0] = true;
              });
          if (isFound[0]) {
            blockWaitToProcess.remove(msg);
            isProcessed[0] = true;
            processSyncBlock(msg.getBlockCapsule());
          }
        });
      }
    }
  }

  private void processSyncBlock(BlockCapsule block) {
    boolean flag = true;
    BlockId blockId = block.getBlockId();
    try {
      ledgerYiNetDelegate.processBlock(block);
    } catch (Exception e) {
      log.error("Process sync block {} failed.", blockId.getString(), e);
      flag = false;
    }
    for (PeerConnection peer : ledgerYiNetDelegate.getActivePeer()) {
      if (peer.getSyncBlockInProcess().remove(blockId)) {
        if (flag) {
          peer.setBlockBothHave(blockId);
          if (peer.getSyncBlockToFetch().isEmpty()) {
            syncNext(peer);
          }
        } else {
          peer.disconnect(Protocol.ReasonCode.BAD_BLOCK);
        }
      }
    }
  }

}
