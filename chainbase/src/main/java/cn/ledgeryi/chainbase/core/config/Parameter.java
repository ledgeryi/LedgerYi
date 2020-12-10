package cn.ledgeryi.chainbase.core.config;

import lombok.Getter;

public class Parameter {

  public class ChainConstant {
    public static final int PRIVATE_KEY_LENGTH = 64;
    public static final int BLOCK_SIZE = 2_000_000;
    public static final long CLOCK_MAX_DELAY = 3600000; // 3600 * 1000 ms
    public static final long MAINTENANCE_SKIP_SLOTS = 2;
    public static final int BLOCK_FILLED_SLOTS_NUMBER = 128;
    public static final int BLOCK_VERSION = 15;
    public static final int BLOCK_PRODUCED_INTERVAL = 3000; //ms,produce block period, must be divisible by 60. millisecond
    public static final int MAX_ACTIVE_WITNESS_NUM = 27;
  }

  public class NodeConstant {
    public static final long SYNC_FETCH_BATCH_NUM = 2000;
    public static final int MAX_TRANSACTION_PENDING = 5000;
    public static final int MAX_HTTP_CONNECT_NUMBER = 50;
  }

  public class NetConstants {
    public static final long ADV_TIME_OUT = 20000L;
    public static final long SYNC_TIME_OUT = 5000L;
    public static final long NET_MAX_TX_PER_SECOND = 700L;
    public static final int MSG_CACHE_DURATION_IN_BLOCKS = 5;
    public static final int MAX_BLOCK_FETCH_PER_PEER = 100;
    public static final int MAX_TX_FETCH_PER_PEER = 1000;
  }

  public enum ForkBlockVersionEnum {
    VERSION_3_2_2(6),
    VERSION_3_5(7),
    VERSION_3_6(8),
    VERSION_3_6_5(9),
    VERSION_3_6_6(10),
    VERSION_4_0(15);

    @Getter
    private int value;

    ForkBlockVersionEnum(int value) {
      this.value = value;
    }
  }

}
