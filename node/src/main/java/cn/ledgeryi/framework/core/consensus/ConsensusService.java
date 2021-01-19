package cn.ledgeryi.framework.core.consensus;

import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.capsule.MasterCapsule;
import cn.ledgeryi.chainbase.core.store.MasterStore;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.consenus.Consensus;
import cn.ledgeryi.consenus.base.Param;
import cn.ledgeryi.crypto.SignUtils;
import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.core.config.args.Args;

@Slf4j(topic = "consensus")
@Component
public class ConsensusService {

  @Autowired
  private Consensus consensus;

  @Autowired
  private MasterStore masterStore;

  @Autowired
  private BlockHandleImpl blockHandle;

  private Args args = Args.getInstance();

  public void start() {
    Param param = new Param();
    param.setEnable(args.isMaster());
    param.setGenesisBlock(args.getGenesisBlock());
    param.setMinParticipationRate(args.getMinParticipationRate());
    param.setBlockProduceTimeoutPercent(Args.getInstance().getBlockProducedTimeOut());
    param.setNeedSyncCheck(args.isNeedSyncCheck());
    List<Param.Miner> miners = new ArrayList<>();
    byte[] privateKey = ByteArray.fromHexString(Args.getInstance().getLocalMasters().getPrivateKey());
    byte[] privateKeyAddress = SignUtils.fromPrivate(privateKey, Args.getInstance().isEccCryptoEngine()).getAddress();
    byte[] masterAddress = Args.getInstance().getLocalMasters().getMasterAccountAddress(DBConfig.isEccCryptoEngine());
    MasterCapsule masterCapsule = masterStore.get(masterAddress);
    if (null == masterCapsule) {
      log.warn("Master {} is not in masterStore.", Hex.encodeHexString(masterAddress));
    } else {
      Param.Miner miner = param.new Miner(privateKey, ByteString.copyFrom(privateKeyAddress), ByteString.copyFrom(masterAddress));
      miners.add(miner);
    }
    param.setMiners(miners);
    param.setBlockHandle(blockHandle);
    consensus.start(param);
  }

  public void stop() {
    consensus.stop();
  }

}
