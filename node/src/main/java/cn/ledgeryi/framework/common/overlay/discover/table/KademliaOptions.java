package cn.ledgeryi.framework.common.overlay.discover.table;

public class KademliaOptions {

  //每个列表中能够存储的最大条目（桶的容量）
  public static final int BUCKET_SIZE = 16;
  //并行数
  public static final int ALPHA = 3;
  //每个节点都各自维护一个固定大小的路由表，路由表由n个列表（桶）组成。n==BINS
  public static final int BINS = 256;
  //寻找轮数
  public static final int MAX_STEPS = 8;

  public static final long REQ_TIMEOUT = 300;
  //刷新等待时间
  public static final long BUCKET_REFRESH = 7200;     //bucket refreshing interval in millis
  public static final long DISCOVER_CYCLE = 30;       //discovery cycle interval in seconds
}
