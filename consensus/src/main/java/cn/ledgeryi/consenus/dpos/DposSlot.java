package cn.ledgeryi.consenus.dpos;


import cn.ledgeryi.consenus.ConsensusDelegate;
import com.google.protobuf.ByteString;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static cn.ledgeryi.consenus.base.Constant.SINGLE_REPEAT;

@Slf4j(topic = "consensus")
@Component
public class DposSlot {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Setter
  private DposService dposService;

  public long getAbSlot(long time) {
    return (time - dposService.getGenesisBlockTime()) / BLOCK_PRODUCED_INTERVAL;
  }

  public long getSlot(long time) {
    long firstSlotTime = getTime(1);
    if (time < firstSlotTime) {
      return 0;
    }
    return (time - firstSlotTime) / BLOCK_PRODUCED_INTERVAL + 1;
  }

  public long getTime(long slot) {
    if (slot == 0) {
      return System.currentTimeMillis();
    }
    long interval = BLOCK_PRODUCED_INTERVAL;
    if (consensusDelegate.getLatestBlockHeaderNumber() == 0) {
      return dposService.getGenesisBlockTime() + slot * interval;
    }
    long time = consensusDelegate.getLatestBlockHeaderTimestamp();
    time = time - ((time - dposService.getGenesisBlockTime()) % interval);
    return time + interval * slot;
  }

  public ByteString getScheduledMaster(long slot) {
    final long currentSlot = getAbSlot(consensusDelegate.getLatestBlockHeaderTimestamp()) + slot;
    if (currentSlot < 0) {
      throw new RuntimeException("current slot should be positive.");
    }
    int size = consensusDelegate.getAllMasters().size();
    if (size <= 0) {
      throw new RuntimeException("active masters is null.");
    }
    int masterIndex = (int) currentSlot % (size * SINGLE_REPEAT);
    masterIndex /= SINGLE_REPEAT;
    return consensusDelegate.getAllMasters().get(masterIndex).getAddress();
  }

}
