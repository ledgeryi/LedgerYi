package cn.ledgeryi.framework.core.consensus;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.consenus.Consensus;
import cn.ledgeryi.consenus.base.BlockHandle;
import cn.ledgeryi.consenus.base.Param;
import cn.ledgeryi.consenus.base.State;
import cn.ledgeryi.framework.common.backup.BackupManager;
import cn.ledgeryi.framework.common.backup.BackupManager.BackupStatusEnum;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.framework.core.net.LedgerYiNetService;
import cn.ledgeryi.framework.core.net.message.BlockMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j(topic = "consensus")
@Component
public class BlockHandleImpl implements BlockHandle {

  @Autowired
  private Manager manager;

  @Autowired
  private BackupManager backupManager;

  @Autowired
  private LedgerYiNetService ledgerYiNetService;

  @Autowired
  private Consensus consensus;

  @Override
  public State getState() {
    if (!backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
      return State.BACKUP_IS_NOT_MASTER;
    }
    return State.OK;
  }

  public Object getLock() {
    return manager;
  }

  public BlockCapsule produce(Param.Miner miner, long blockTime, long timeout) {
    BlockCapsule blockCapsule = manager.generateBlock(miner, blockTime, timeout);
    if (blockCapsule == null) {
      return null;
    }
//    if (blockCapsule.getTransactions().size() <= 0){
//      log.info("Produce block failed: " + State.BLOCK_NOT_CONTAIN_TRANSACTIONS);
//     return null;
//    }
    try {
      consensus.receiveBlock(blockCapsule);
      BlockMessage blockMessage = new BlockMessage(blockCapsule);
      manager.pushBlock(blockCapsule);
      ledgerYiNetService.broadcast(blockMessage);
    } catch (Exception e) {
      log.error("Handle block {} failed, reason: {}.", blockCapsule.getBlockId().getString(), e.getMessage());
      return null;
    }
    return blockCapsule;
  }
}
