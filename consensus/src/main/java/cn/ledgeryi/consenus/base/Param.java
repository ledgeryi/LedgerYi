package cn.ledgeryi.consenus.base;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import cn.ledgeryi.chainbase.core.config.args.GenesisBlock;

import java.util.List;

public class Param {

  @Getter
  @Setter
  private boolean enable;
  @Getter
  @Setter
  private boolean needSyncCheck;
  @Getter
  @Setter
  private int minParticipationRate;
  @Getter
  @Setter
  private int blockProduceTimeoutPercent;
  @Getter
  @Setter
  private GenesisBlock genesisBlock;
  @Getter
  @Setter
  private List<Miner> miners;
  @Getter
  @Setter
  private BlockHandle blockHandle;

  public class Miner {

    @Getter
    @Setter
    private byte[] privateKey;

    @Getter
    @Setter
    private ByteString privateKeyAddress;

    @Getter
    @Setter
    private ByteString masterAddress;

    public Miner(byte[] privateKey, ByteString privateKeyAddress, ByteString masterAddress) {
      this.privateKey = privateKey;
      this.privateKeyAddress = privateKeyAddress;
      this.masterAddress = masterAddress;
    }
  }
}