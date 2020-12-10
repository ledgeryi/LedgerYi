package cn.ledgeryi.consenus;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.consenus.base.ConsensusInterface;
import cn.ledgeryi.consenus.base.Param;
import cn.ledgeryi.consenus.dpos.DposService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j(topic = "consensus")
@Component
public class Consensus {

  @Autowired
  private DposService dposService;

  private ConsensusInterface consensusInterface;

  public void start(Param param) {
    consensusInterface = dposService;
    consensusInterface.start(param);
  }

  public void stop() {
    consensusInterface = dposService;
    consensusInterface.stop();
  }

  public void receiveBlock(BlockCapsule blockCapsule) {
    consensusInterface.receiveBlock(blockCapsule);
  }

  public boolean validBlock(BlockCapsule blockCapsule) {
    return consensusInterface.validBlock(blockCapsule);
  }

  public boolean applyBlock(BlockCapsule blockCapsule) {
    return consensusInterface.applyBlock(blockCapsule);
  }

}