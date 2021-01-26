package cn.ledgeryi.framework.core.net.messagehandler;


import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.common.core.exception.P2pException;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.net.LedgerYiNetDelegate;
import cn.ledgeryi.framework.core.net.message.BlockMessage;
import cn.ledgeryi.framework.core.net.message.LedgerYiMessage;
import cn.ledgeryi.framework.core.net.peer.Item;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;
import cn.ledgeryi.framework.core.net.service.AdvService;
import cn.ledgeryi.framework.core.net.service.SyncService;
import cn.ledgeryi.framework.core.services.MasterProductBlockService;
import cn.ledgeryi.protos.Protocol.Inventory.InventoryType;

import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.BLOCK_SIZE;
import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

@Slf4j(topic = "net")
@Component
public class BlockMsgHandler implements LedgerYiMsgHandler {

  @Autowired
  private LedgerYiNetDelegate ledgerYiNetDelegate;

  @Autowired
  private AdvService advService;

  @Autowired
  private SyncService syncService;

  @Autowired
  private MasterProductBlockService masterProductBlockService;

  private int maxBlockSize = BLOCK_SIZE + 1000;

  private boolean fastForward = Args.getInstance().isFastForward();

  @Override
  public void processMessage(PeerConnection peer, LedgerYiMessage msg) throws P2pException {
    BlockMessage blockMessage = (BlockMessage) msg;
    BlockCapsule.BlockId blockId = blockMessage.getBlockId();
    if (!fastForward && !peer.isFastForwardPeer()) {
      check(peer, blockMessage);
    }
    if (peer.getSyncBlockRequested().containsKey(blockId)) {
      peer.getSyncBlockRequested().remove(blockId);
      syncService.processBlock(peer, blockMessage);
    } else {
      Long time = peer.getAdvInvRequest().remove(new Item(blockId, InventoryType.BLOCK));
      long now = System.currentTimeMillis();
      long interval = blockId.getNum() - ledgerYiNetDelegate.getHeadBlockId().getNum();
      processBlock(peer, blockMessage.getBlockCapsule());
      log.debug(
          "Receive block/interval {}/{} from {} fetch/delay {}/{}ms, txs/process {}/{}ms, master: {}",
          blockId.getNum(),
          interval,
          peer.getInetAddress(),
          time == null ? 0 : now - time,
          now - blockMessage.getBlockCapsule().getTimeStamp(),
          ((BlockMessage) msg).getBlockCapsule().getTransactions().size(),
          System.currentTimeMillis() - now,
          Hex.toHexString(blockMessage.getBlockCapsule().getMasterAddress().toByteArray()));
    }
  }

  private void check(PeerConnection peer, BlockMessage msg) throws P2pException {
    Item item = new Item(msg.getBlockId(), InventoryType.BLOCK);
    if (!peer.getSyncBlockRequested().containsKey(msg.getBlockId()) && !peer.getAdvInvRequest()
        .containsKey(item)) {
      throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "no request");
    }
    BlockCapsule blockCapsule = msg.getBlockCapsule();
    if (blockCapsule.getInstance().getSerializedSize() > maxBlockSize) {
      throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "block size over limit");
    }
    long gap = blockCapsule.getTimeStamp() - System.currentTimeMillis();
    if (gap >= BLOCK_PRODUCED_INTERVAL) {
      throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "block time error");
    }
  }

  private void processBlock(PeerConnection peer, BlockCapsule block) throws P2pException {
    BlockCapsule.BlockId blockId = block.getBlockId();
    if (!ledgerYiNetDelegate.containBlock(block.getParentBlockId())) {
      log.warn("Get unlink block {} from {}, head is {}.", blockId.getString(),
          peer.getInetAddress(), ledgerYiNetDelegate.getHeadBlockId().getString());
      syncService.startSync(peer);
      return;
    }

    Item item = new Item(blockId, InventoryType.BLOCK);
    if (fastForward || peer.isFastForwardPeer()) {
      peer.getAdvInvReceive().put(item, System.currentTimeMillis());
      advService.addInvToCache(item);
    }

    if (fastForward) {
      if (block.getNum() < ledgerYiNetDelegate.getHeadBlockId().getNum()) {
        log.warn("Receive a low block {}, head {}", blockId.getString(), ledgerYiNetDelegate.getHeadBlockId().getString());
        return;
      }
      if (ledgerYiNetDelegate.validBlock(block)) {
        advService.fastForward(new BlockMessage(block));
        ledgerYiNetDelegate.trustNode(peer);
      }
    }

    ledgerYiNetDelegate.processBlock(block);
    masterProductBlockService.validMasterProductTwoBlock(block);
    ledgerYiNetDelegate.getActivePeer().forEach(p -> {
      if (p.getAdvInvReceive().getIfPresent(blockId) != null) {
        p.setBlockBothHave(blockId);
      }
    });

    if (!fastForward) {
      advService.broadcast(new BlockMessage(block));
    }
  }

}
