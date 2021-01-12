package cn.ledgeryi.consenus.dpos;

import cn.ledgeryi.consenus.ConsensusDelegate;
import cn.ledgeryi.consenus.base.BlockHandle;
import cn.ledgeryi.consenus.base.ConsensusInterface;
import cn.ledgeryi.consenus.base.Param;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.MasterCapsule;
import cn.ledgeryi.chainbase.core.config.args.GenesisBlock;

import java.util.*;
import java.util.stream.Collectors;

import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.MAX_ACTIVE_WITNESS_NUM;
import static cn.ledgeryi.consenus.base.Constant.SOLIDIFIED_THRESHOLD;

@Slf4j(topic = "consensus")
@Component
public class DposService implements ConsensusInterface {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Autowired
  private DposTask dposTask;

  @Autowired
  private DposSlot dposSlot;

  @Autowired
  private StateManager stateManager;

  @Autowired
  private StatisticManager statisticManager;

  @Getter
  @Setter
  private volatile boolean needSyncCheck;
  @Getter
  private volatile boolean enable;
  @Getter
  private int minParticipationRate;
  @Getter
  private int blockProduceTimeoutPercent;
  @Getter
  private long genesisBlockTime;
  @Getter
  private BlockHandle blockHandle;
  @Getter
  private GenesisBlock genesisBlock;
  @Getter
  private Map<ByteString, Param.Miner> miners = new HashMap<>();

  @Override
  public void start(Param param) {
    this.enable = param.isEnable();
    this.needSyncCheck = param.isNeedSyncCheck();
    this.minParticipationRate = param.getMinParticipationRate();
    this.blockProduceTimeoutPercent = param.getBlockProduceTimeoutPercent();
    this.blockHandle = param.getBlockHandle();
    this.genesisBlock = param.getGenesisBlock();
    this.genesisBlockTime = Long.parseLong(param.getGenesisBlock().getTimestamp());
    param.getMiners().forEach(miner -> miners.put(miner.getMasterAddress(), miner));

    dposTask.setDposService(this);
    dposSlot.setDposService(this);
    stateManager.setDposService(this);

    if (consensusDelegate.getLatestBlockHeaderNumber() == 0) {
      List<ByteString> masters = new ArrayList<>();
      consensusDelegate.getAllMasters().forEach(masterCapsule -> masters.add(masterCapsule.getAddress()));
      updateMaster(masters);
      List<ByteString> addresses = consensusDelegate.getActiveMasters();
      addresses.forEach(address -> {
        MasterCapsule masterCapsule = consensusDelegate.getMaster(address.toByteArray());
        masterCapsule.setIsJobs(true);
        consensusDelegate.saveMaster(masterCapsule);
      });
    }

    dposTask.init();
  }

  @Override
  public void stop() {
    dposTask.stop();
  }

  @Override
  public void receiveBlock(BlockCapsule blockCapsule) {
    stateManager.receiveBlock(blockCapsule);
  }

  @Override
  public boolean validBlock(BlockCapsule blockCapsule) {
    if (consensusDelegate.getLatestBlockHeaderNumber() == 0) {
      return true;
    }
    ByteString masterAddress = blockCapsule.getMasterAddress();
    long timeStamp = blockCapsule.getTimeStamp();
    long bSlot = dposSlot.getAbSlot(timeStamp);
    long hSlot = dposSlot.getAbSlot(consensusDelegate.getLatestBlockHeaderTimestamp());
    if (bSlot <= hSlot) {
      log.warn("ValidBlock failed: bSlot: {} <= hSlot: {}", bSlot, hSlot);
      return false;
    }
    long slot = dposSlot.getSlot(timeStamp);
    final ByteString scheduledMaster = dposSlot.getScheduledMaster(slot);
    if (!scheduledMaster.equals(masterAddress)) {
      log.warn("ValidBlock failed: sMaster: {}, bMaster: {}, bTimeStamp: {}, slot: {}",
          ByteArray.toHexString(scheduledMaster.toByteArray()),
          ByteArray.toHexString(masterAddress.toByteArray()), new DateTime(timeStamp), slot);
      return false;
    }
    return true;
  }

  @Override
  public boolean applyBlock(BlockCapsule blockCapsule) {
    statisticManager.applyBlock(blockCapsule);
    updateSolidBlock();
    return true;
  }

  private void updateSolidBlock() {
    List<Long> numbers = consensusDelegate.getActiveMasters().stream()
            .map(address -> consensusDelegate.getMaster(address.toByteArray()).getLatestBlockNum())
            .sorted()
            .collect(Collectors.toList());
    long size = consensusDelegate.getActiveMasters().size();
    int position = (int) (size * (1 - SOLIDIFIED_THRESHOLD * 1.0 / 100));
    long newSolidNum = numbers.get(position);
    long oldSolidNum = consensusDelegate.getLatestSolidifiedBlockNum();
    if (newSolidNum < oldSolidNum) {
      log.warn("Update solid block number failed, new: {} < old: {}", newSolidNum, oldSolidNum);
      return;
    }
    consensusDelegate.saveLatestSolidifiedBlockNum(newSolidNum);
    log.info("Update solid block number to {}", newSolidNum);
  }

  public void updateMaster(List<ByteString> list) {
    list.sort(Comparator.comparingLong((ByteString b) ->
        consensusDelegate.getMaster(b.toByteArray()).getVoteCount())
        .reversed().thenComparing(Comparator.comparingInt(ByteString::hashCode).reversed()));
    if (list.size() > MAX_ACTIVE_WITNESS_NUM) {
      consensusDelegate.saveActiveMasters(list.subList(0, MAX_ACTIVE_WITNESS_NUM));
    } else {
      consensusDelegate.saveActiveMasters(list);
    }
  }

}
