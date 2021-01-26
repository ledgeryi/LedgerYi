package cn.ledgeryi.framework.core.db;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "DB")
public class PendingManager implements AutoCloseable {

  @Getter
  private List<TransactionCapsule> tmpTransactions = new ArrayList<>();
  private Manager dbManager;

  public PendingManager(Manager db, BlockCapsule block) {
    this.dbManager = db;
    tmpTransactions.addAll(block.getTransactions());
    db.getSession().reset();
  }

  @Override
  public void close() {

    for (TransactionCapsule tx : tmpTransactions) {
      try {
        if (tx.getTxTrace() != null) {
          dbManager.getRepushTransactions().put(tx);
        }
      } catch (InterruptedException e) {
        log.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    tmpTransactions.clear();

    /*for (TransactionCapsule tx : dbManager.getPoppedTransactions()) {
      try {
        if (tx.getTxTrace() != null) {
          dbManager.getRepushTransactions().put(tx);
        }
      } catch (InterruptedException e) {
        log.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    dbManager.getPoppedTransactions().clear();*/
  }
}
