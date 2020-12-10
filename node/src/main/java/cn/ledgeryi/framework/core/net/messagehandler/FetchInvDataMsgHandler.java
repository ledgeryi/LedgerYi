package cn.ledgeryi.framework.core.net.messagehandler;


import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.config.Parameter;
import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.common.core.exception.P2pException;
import cn.ledgeryi.common.utils.Sha256Hash;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.overlay.discover.node.statistics.MessageCount;
import cn.ledgeryi.framework.core.net.LedgerYiNetDelegate;
import cn.ledgeryi.framework.core.net.message.BlockMessage;
import cn.ledgeryi.framework.core.net.message.FetchInvDataMessage;
import cn.ledgeryi.framework.core.net.message.TransactionMessage;
import cn.ledgeryi.framework.core.net.message.TransactionsMessage;
import cn.ledgeryi.framework.core.net.message.LedgerYiMessage;
import cn.ledgeryi.framework.core.net.peer.Item;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;
import cn.ledgeryi.framework.core.net.service.AdvService;
import cn.ledgeryi.framework.core.net.service.SyncService;
import cn.ledgeryi.protos.Protocol.Inventory.InventoryType;
import cn.ledgeryi.protos.Protocol.ReasonCode;
import cn.ledgeryi.protos.Protocol.Transaction;

import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

@Slf4j(topic = "net")
@Component
public class FetchInvDataMsgHandler implements LedgerYiMsgHandler {

  private static final int MAX_SIZE = 1_000_000;
  @Autowired
  private LedgerYiNetDelegate ledgerYiNetDelegate;
  @Autowired
  private SyncService syncService;
  @Autowired
  private AdvService advService;

  /**
   * 处理收到同步区块或交易的请求：发生同步请求中包含的交易或区块
   */
  @Override
  public void processMessage(PeerConnection peer, LedgerYiMessage msg) throws P2pException {

    FetchInvDataMessage fetchInvDataMsg = (FetchInvDataMessage) msg;

    check(peer, fetchInvDataMsg);

    InventoryType type = fetchInvDataMsg.getInventoryType();
    List<Transaction> transactions = Lists.newArrayList();

    int size = 0;

    for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
      Item item = new Item(hash, type);
      Message message = advService.getMessage(item);
      if (message == null) {
        try {
          message = ledgerYiNetDelegate.getData(hash, type);
        } catch (Exception e) {
          log.error("Fetch item {} failed. reason: {}", item, e.getMessage());
          peer.disconnect(ReasonCode.FETCH_FAIL);
          return;
        }
      }

      if (type == InventoryType.BLOCK) {
        BlockCapsule.BlockId blockId = ((BlockMessage) message).getBlockCapsule().getBlockId();
        if (peer.getBlockBothHave().getNum() < blockId.getNum()) {
          peer.setBlockBothHave(blockId);
        }
        peer.sendMessage(message);
      } else {
        transactions.add(((TransactionMessage) message).getTransactionCapsule().getInstance());
        size += ((TransactionMessage) message).getTransactionCapsule().getInstance().getSerializedSize();
        if (size > MAX_SIZE) {
          peer.sendMessage(new TransactionsMessage(transactions));
          transactions = Lists.newArrayList();
          size = 0;
        }
      }
    }
    if (!transactions.isEmpty()) {
      peer.sendMessage(new TransactionsMessage(transactions));
    }
  }

  private void check(PeerConnection peer, FetchInvDataMessage fetchInvDataMsg) throws P2pException {
    MessageTypes type = fetchInvDataMsg.getInvMessageType();

    if (type == MessageTypes.TX) {
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.TX)) == null) {
          throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "not spread inv: {}" + hash);
        }
      }
      int fetchCount = peer.getNodeStatistics().messageStatistics.ledgerYiInTxFetchInvDataElement.getCount(10);
      int maxCount = advService.getTxCount().getCount(60);
      if (fetchCount > maxCount) {
        log.error("maxCount: " + maxCount + ", fetchCount: " + fetchCount);
        throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "maxCount: " + maxCount + ", fetchCount: " + fetchCount);
      }
    } else {
      boolean isAdv = true;
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.BLOCK)) == null) {
          isAdv = false;
          break;
        }
      }
      if (isAdv) {
        MessageCount ledgerYiOutAdvBlock = peer.getNodeStatistics().messageStatistics.ledgerYiOutAdvBlock;
        ledgerYiOutAdvBlock.add(fetchInvDataMsg.getHashList().size());
        int outBlockCountIn1min = ledgerYiOutAdvBlock.getCount(60);
        int producedBlockIn2min = 120_000 / BLOCK_PRODUCED_INTERVAL;
        if (outBlockCountIn1min > producedBlockIn2min) {
          log.error("producedBlockIn2min: " + producedBlockIn2min + ", outBlockCountIn1min: "
              + outBlockCountIn1min);
          throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "producedBlockIn2min: " + producedBlockIn2min
              + ", outBlockCountIn1min: " + outBlockCountIn1min);
        }
      } else {
        if (!peer.isNeedSyncFromUs()) {
          throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "no need sync");
        }
        for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
          long blockNum = new BlockCapsule.BlockId(hash).getNum();
          long minBlockNum =
              peer.getLastSyncBlockId().getNum() - 2 * Parameter.NodeConstant.SYNC_FETCH_BATCH_NUM;
          if (blockNum < minBlockNum) {
            throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE,
                "minBlockNum: " + minBlockNum + ", blockNum: " + blockNum);
          }
          if (peer.getSyncBlockIdCache().getIfPresent(hash) != null) {
            throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE,
                new BlockCapsule.BlockId(hash).getString() + " is exist");
          }
          peer.getSyncBlockIdCache().put(hash, System.currentTimeMillis());
        }
      }
    }
  }

}
