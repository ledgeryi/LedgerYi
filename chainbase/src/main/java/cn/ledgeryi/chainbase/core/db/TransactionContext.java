package cn.ledgeryi.chainbase.core.db;

import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import lombok.Data;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.chainbase.core.store.StoreFactory;

@Data
public class TransactionContext {

  private boolean isStatic;
  private boolean eventPluginLoaded;
  private BlockCapsule blockCap;
  private StoreFactory storeFactory;
  private TransactionCapsule txCap;
  private ProgramResult programResult = new ProgramResult();

  public TransactionContext(BlockCapsule blockCap, TransactionCapsule txCap,
      StoreFactory storeFactory, boolean isStatic, boolean eventPluginLoaded) {

    this.txCap = txCap;
    this.isStatic = isStatic;
    this.blockCap = blockCap;
    this.storeFactory = storeFactory;
    this.eventPluginLoaded = eventPluginLoaded;
  }
}
