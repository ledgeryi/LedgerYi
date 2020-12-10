package cn.ledgeryi.framework.core.db;

import java.util.ArrayList;
import java.util.List;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.chainbase.core.db.TransactionTrace;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "DB")
public class PendingManager implements AutoCloseable {

  @Getter
  private List<TransactionCapsule> tmpTransactions = new ArrayList<>();
  private Manager dbManager;
  private long timeout = 60_000;

  public PendingManager(Manager db, BlockCapsule block) {
    this.dbManager = db;
    /*db.getPendingTransactions().forEach(transactionCapsule -> {
      if (System.currentTimeMillis() - transactionCapsule.getTime() < timeout) {
        tmpTransactions.add(transactionCapsule);
      }
    });
    db.getPendingTransactions().clear();*/
    tmpTransactions.addAll(block.getTransactions());
    db.getSession().reset();
  }

  @Override
  public void close() {

    for (TransactionCapsule tx : tmpTransactions) {
      try {
        if (tx.getTxTrace() != null &&
            tx.getTxTrace().getTimeResultType().equals(TransactionTrace.TimeResultType.NORMAL)) {
          dbManager.getRepushTransactions().put(tx);
        }
      } catch (InterruptedException e) {
        log.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    tmpTransactions.clear();

    for (TransactionCapsule tx : dbManager.getPoppedTransactions()) {
      try {
        if (tx.getTxTrace() != null &&
            tx.getTxTrace().getTimeResultType().equals(TransactionTrace.TimeResultType.NORMAL)) {
          dbManager.getRepushTransactions().put(tx);
        }
      } catch (InterruptedException e) {
        log.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    dbManager.getPoppedTransactions().clear();
  }
}
