package cn.ledgeryi.consenus.dpos;

import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.common.utils.Time;
import cn.ledgeryi.consenus.base.Param;
import cn.ledgeryi.consenus.base.State;
import com.google.protobuf.ByteString;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.protos.Protocol.BlockHeader;

import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

@Slf4j(topic = "consensus")
@Component
public class DposTask {

  @Autowired
  private DposSlot dposSlot;

  @Autowired
  private StateManager stateManager;

  @Setter
  private DposService dposService;

  private Thread produceThread;

  private volatile boolean isRunning = true;

  public void init() {

    if (!dposService.isEnable() || StringUtils.isEmpty(dposService.getMiners())) {
      return;
    }

    Runnable runnable = () -> {
      while (isRunning) {
        try {
          if (dposService.isNeedSyncCheck()) {
            Thread.sleep(1000);
            dposService.setNeedSyncCheck(dposSlot.getTime(1) > System.currentTimeMillis());
          } else {
            long time = BLOCK_PRODUCED_INTERVAL - System.currentTimeMillis() % BLOCK_PRODUCED_INTERVAL;
            Thread.sleep(time);
            State state = produceBlock();
            if (!State.OK.equals(state)) {
              log.debug("Produce block failed: {}", state);
            }
          }
        } catch (Throwable throwable) {
          log.error("Produce block error.", throwable);
        }
      }
    };
    produceThread = new Thread(runnable, "DPosMiner");
    produceThread.start();
    log.info("====================DPoS task started====================");
  }

  public void stop() {
    isRunning = false;
    if (produceThread != null) {
      produceThread.interrupt();
    }
    log.info("====================DPoS task stopped====================");
  }

  private State produceBlock() {
    State state = stateManager.getState();
    if (!State.OK.equals(state)) {
      return state;
    }
    synchronized (dposService.getBlockHandle().getLock()) {
      long slot = dposSlot.getSlot(System.currentTimeMillis() + 50);
      if (slot == 0) {
        return State.NOT_TIME_YET;
      }

      ByteString masterAddress = dposSlot.getScheduledMaster(slot);
      Param.Miner miner = dposService.getMiners().get(masterAddress);
      if (miner == null) {
        return State.NOT_MY_TURN;
      }

      long pTime = dposSlot.getTime(slot);
      long timeout = pTime + BLOCK_PRODUCED_INTERVAL / 2 * dposService.getBlockProduceTimeoutPercent() / 100;
      //produce block
      BlockCapsule blockCapsule = dposService.getBlockHandle().produce(miner, pTime, timeout);
      if (blockCapsule == null) {
        return State.PRODUCE_BLOCK_FAILED;
      }

      BlockHeader.raw raw = blockCapsule.getInstance().getBlockHeader().getRawData();
      log.info("Produce block successfully, num: {}, time: {}, master: {}, ID:{}, parentID:{}",
          raw.getNumber(),
          Time.getTimeString(raw.getTimestamp()),
          ByteArray.toHexString(raw.getMasterAddress().toByteArray()),
          new Sha256Hash(raw.getNumber(), Sha256Hash.of(DBConfig.isEccCryptoEngine(), raw.toByteArray())),
          ByteArray.toHexString(raw.getParentHash().toByteArray()));
    }
    return State.OK;
  }

}
