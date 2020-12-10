package cn.ledgeryi.consenus.dpos;

import cn.ledgeryi.consenus.ConsensusDelegate;
import cn.ledgeryi.consenus.base.State;
import com.google.protobuf.ByteString;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule.BlockId;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.MAX_ACTIVE_WITNESS_NUM;

@Slf4j(topic = "consensus")
@Component
public class StateManager {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Setter
  private DposService dposService;

  @Setter
  private volatile BlockId currentBlockId;

  private final AtomicInteger dupBlockCount = new AtomicInteger(0);
  private final AtomicLong dupBlockTime = new AtomicLong(0);
  private long blockCycle = BLOCK_PRODUCED_INTERVAL * MAX_ACTIVE_WITNESS_NUM;


  public State getState() {
    if (System.currentTimeMillis() < consensusDelegate.getLatestBlockHeaderTimestamp()) {
      return State.CLOCK_ERROR;
    }
    State status = dposService.getBlockHandle().getState();
    if (!State.OK.equals(status)) {
      return status;
    }
    if (isDupMaster()) {
      return State.DUP_WITNESS;
    }
    int participation = consensusDelegate.calculateFilledSlotsCount();
    int minParticipationRate = dposService.getMinParticipationRate();
    if (participation < minParticipationRate) {
      log.warn("Participation:{} <  minParticipationRate:{}", participation, minParticipationRate);
      return State.LOW_PARTICIPATION;
    }
    return State.OK;
  }

  public void receiveBlock(BlockCapsule blockCapsule) {
    if (dposService.isNeedSyncCheck()) {
      return;
    }
    if (blockCapsule.generatedByMyself) {
      currentBlockId = blockCapsule.getBlockId();
      return;
    }
    ByteString master = blockCapsule.getMasterAddress();
    if (!dposService.getMiners().containsKey(master)) {
      return;
    }
    if (blockCapsule.getBlockId().equals(currentBlockId)) {
      return;
    }
    if (System.currentTimeMillis() - blockCapsule.getTimeStamp() > BLOCK_PRODUCED_INTERVAL) {
      return;
    }
    if (dposService.getBlockHandle().getState() != State.OK) {
      dupBlockCount.set(1);
      return;
    }
    if (dupBlockCount.get() == 0) {
      dupBlockCount.set(new Random().nextInt(10));
    } else {
      dupBlockCount.set(10);
    }
    dupBlockTime.set(System.currentTimeMillis());
    log.warn("Dup block produced: {}", blockCapsule);
  }

  private boolean isDupMaster() {
    if (dupBlockCount.get() == 0) {
      return false;
    }
    if (System.currentTimeMillis() - dupBlockTime.get() > dupBlockCount.get() * blockCycle) {
      dupBlockCount.set(0);
      return false;
    }
    return true;
  }
}
