package cn.ledgeryi.framework.core.services;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.common.utils.ByteArray;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j(topic = "Master")
@Service
public class MasterProductBlockService {

  private Cache<Long, BlockCapsule> historyBlockCapsuleCache = CacheBuilder.newBuilder()
      .initialCapacity(200).maximumSize(200).build();

  private Map<String, CheatMasterInfo> cheatMasterInfoMap = new HashMap<>();

  public void validMasterProductTwoBlock(BlockCapsule block) {
    try {
      BlockCapsule blockCapsule = historyBlockCapsuleCache.getIfPresent(block.getNum());
      if (blockCapsule != null && Arrays.equals(blockCapsule.getMasterAddress().toByteArray(),
          block.getMasterAddress().toByteArray()) && !Arrays.equals(block.getBlockId().getBytes(),
          blockCapsule.getBlockId().getBytes())) {
        String key = ByteArray.toHexString(block.getMasterAddress().toByteArray());
        if (!cheatMasterInfoMap.containsKey(key)) {
          CheatMasterInfo cheatMasterInfo = new CheatMasterInfo();
          cheatMasterInfoMap.put(key, cheatMasterInfo);
        }
        cheatMasterInfoMap.get(key).clear().setTime(System.currentTimeMillis())
            .setLatestBlockNum(block.getNum()).add(block).add(blockCapsule).increment();
      } else {
        historyBlockCapsuleCache.put(block.getNum(), block);
      }
    } catch (Exception e) {
      log.error("valid Master same time product two block fail! blockNum: {}, blockHash: {}",
          block.getNum(), block.getBlockId().toString(), e);
    }
  }

  public Map<String, CheatMasterInfo> queryCheatMasterInfo() {
    return cheatMasterInfoMap;
  }

  public static class CheatMasterInfo {

    private AtomicInteger times = new AtomicInteger(0);
    private long latestBlockNum;
    private Set<BlockCapsule> blockCapsuleSet = new HashSet<>();
    private long time;

    public CheatMasterInfo increment() {
      times.incrementAndGet();
      return this;
    }

    public AtomicInteger getTimes() {
      return times;
    }

    public CheatMasterInfo setTimes(AtomicInteger times) {
      this.times = times;
      return this;
    }

    public long getLatestBlockNum() {
      return latestBlockNum;
    }

    public CheatMasterInfo setLatestBlockNum(long latestBlockNum) {
      this.latestBlockNum = latestBlockNum;
      return this;
    }

    public Set<BlockCapsule> getBlockCapsuleSet() {
      return new HashSet<>(blockCapsuleSet);
    }

    public CheatMasterInfo setBlockCapsuleSet(Set<BlockCapsule> blockCapsuleSet) {
      this.blockCapsuleSet = new HashSet<>(blockCapsuleSet);
      return this;
    }

    public CheatMasterInfo clear() {
      blockCapsuleSet.clear();
      return this;
    }

    public CheatMasterInfo add(BlockCapsule blockCapsule) {
      blockCapsuleSet.add(blockCapsule);
      return this;
    }

    public long getTime() {
      return time;
    }

    public CheatMasterInfo setTime(long time) {
      this.time = time;
      return this;
    }

    @Override
    public String toString() {
      return "{" +
          "times=" + times.get() +
          ", time=" + time +
          ", latestBlockNum=" + latestBlockNum +
          ", blockCapsuleSet=" + blockCapsuleSet +
          '}';
    }
  }
}
