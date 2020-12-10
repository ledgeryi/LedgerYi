package cn.ledgeryi.framework.core.net.messagehandler;


import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule.BlockId;
import cn.ledgeryi.chainbase.core.config.Parameter;
import cn.ledgeryi.common.core.exception.P2pException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.core.net.LedgerYiNetDelegate;
import cn.ledgeryi.framework.core.net.message.ChainInventoryMessage;
import cn.ledgeryi.framework.core.net.message.LedgerYiMessage;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;
import cn.ledgeryi.framework.core.net.service.SyncService;

import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

@Slf4j(topic = "net")
@Component
public class ChainInventoryMsgHandler implements LedgerYiMsgHandler {

  @Autowired
  private LedgerYiNetDelegate ledgerYiNetDelegate;

  @Autowired
  private SyncService syncService;

  @Override
  public void processMessage(PeerConnection peer, LedgerYiMessage msg) throws P2pException {
    ChainInventoryMessage chainInventoryMessage = (ChainInventoryMessage) msg;
    check(peer, chainInventoryMessage);
    peer.setNeedSyncFromPeer(true);
    peer.setSyncChainRequested(null);
    //
    Deque<BlockId> blockIdWeGet = new LinkedList<>(chainInventoryMessage.getBlockIds());
    if (blockIdWeGet.size() == 1 && ledgerYiNetDelegate.containBlock(blockIdWeGet.peek())) {
      peer.setNeedSyncFromPeer(false);
      return;
    }
    while (!peer.getSyncBlockToFetch().isEmpty()) {
      if (peer.getSyncBlockToFetch().peekLast().equals(blockIdWeGet.peekFirst())) {
        break;
      }
      peer.getSyncBlockToFetch().pollLast();
    }
    blockIdWeGet.poll();
    peer.setRemainNum(chainInventoryMessage.getRemainNum());
    peer.getSyncBlockToFetch().addAll(blockIdWeGet);
    synchronized (ledgerYiNetDelegate.getBlockLock()) {
      while (!peer.getSyncBlockToFetch().isEmpty() && ledgerYiNetDelegate.containBlock(peer.getSyncBlockToFetch().peek())) {
        BlockId blockId = peer.getSyncBlockToFetch().pop();
        peer.setBlockBothHave(blockId);
        log.info("Block {} from {} is processed", blockId.getString(), peer.getNode().getHost());
      }
    }
    if ((chainInventoryMessage.getRemainNum() == 0 && !peer.getSyncBlockToFetch().isEmpty()) ||
        (chainInventoryMessage.getRemainNum() != 0 && peer.getSyncBlockToFetch().size() > Parameter.NodeConstant.SYNC_FETCH_BATCH_NUM)) {
      syncService.setFetchFlag(true);
    } else {
      syncService.syncNext(peer);
    }
  }

  private void check(PeerConnection peer, ChainInventoryMessage msg) throws P2pException {
    if (peer.getSyncChainRequested() == null) {
      throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "not send syncBlockChainMsg");
    }

    List<BlockId> blockIds = msg.getBlockIds();
    if (CollectionUtils.isEmpty(blockIds)) {
      throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "blockIds is empty");
    }

    if (blockIds.size() > Parameter.NodeConstant.SYNC_FETCH_BATCH_NUM + 1) {
      throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "big blockIds size: " + blockIds.size());
    }

    if (msg.getRemainNum() != 0 && blockIds.size() < Parameter.NodeConstant.SYNC_FETCH_BATCH_NUM) {
      throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE,
          "remain: " + msg.getRemainNum() + ", blockIds size: " + blockIds.size());
    }

    long num = blockIds.get(0).getNum();
    for (BlockId id : msg.getBlockIds()) {
      if (id.getNum() != num++) {
        throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "not continuous block");
      }
    }

    if (!peer.getSyncChainRequested().getKey().contains(blockIds.get(0))) {
      throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "unlinked block, my head: "
          + peer.getSyncChainRequested().getKey().getLast().getString()
          + ", peer: " + blockIds.get(0).getString());
    }

    if (ledgerYiNetDelegate.getHeadBlockId().getNum() > 0) {
      long maxRemainTime =
          Parameter.ChainConstant.CLOCK_MAX_DELAY + System.currentTimeMillis() - ledgerYiNetDelegate
              .getBlockTime(ledgerYiNetDelegate.getSolidBlockId());
      long maxFutureNum =
          maxRemainTime / BLOCK_PRODUCED_INTERVAL + ledgerYiNetDelegate.getSolidBlockId().getNum();
      long lastNum = blockIds.get(blockIds.size() - 1).getNum();
      if (lastNum + msg.getRemainNum() > maxFutureNum) {
        throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "lastNum: " + lastNum + " + remainNum: "
            + msg.getRemainNum() + " > futureMaxNum: " + maxFutureNum);
      }
    }
  }

}
