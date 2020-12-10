package cn.ledgeryi.consenus.base;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;

public interface BlockHandle {

  State getState();

  Object getLock();

  BlockCapsule produce(Param.Miner miner, long blockTime, long timeout);

}