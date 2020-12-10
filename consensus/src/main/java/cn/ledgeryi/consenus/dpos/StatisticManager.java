package cn.ledgeryi.consenus.dpos;

import cn.ledgeryi.consenus.ConsensusDelegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.MasterCapsule;

@Slf4j(topic = "consensus")
@Component
public class StatisticManager {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Autowired
  private DposSlot dposSlot;

  public void applyBlock(BlockCapsule blockCapsule) {
    MasterCapsule wc;
    long blockNum = blockCapsule.getNum();
    long blockTime = blockCapsule.getTimeStamp();
    byte[] blockMaster = blockCapsule.getMasterAddress().toByteArray();
    wc = consensusDelegate.getMaster(blockMaster);
    wc.setTotalProduced(wc.getTotalProduced() + 1);
    wc.setLatestBlockNum(blockNum);
    wc.setLatestSlotNum(dposSlot.getAbSlot(blockTime));
    consensusDelegate.saveMaster(wc);

    long slot = 1;
    if (blockNum != 1) {
      slot = dposSlot.getSlot(blockTime);
    }
    for (int i = 1; i < slot; ++i) {
      byte[] master = dposSlot.getScheduledMaster(i).toByteArray();
      wc = consensusDelegate.getMaster(master);
      wc.setTotalMissed(wc.getTotalMissed() + 1);
      consensusDelegate.saveMaster(wc);
      log.info("Current block: {}, master: {} totalMissed: {}", blockNum, wc.createReadableString(), wc.getTotalMissed());
      consensusDelegate.applyBlock(false);
    }
    consensusDelegate.applyBlock(true);
  }
}