package cn.ledgeryi.framework.common.utils;

import cn.ledgeryi.chainbase.common.utils.ForkUtils;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.common.utils.DecodeUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import cn.ledgeryi.framework.core.db.Manager;

@Slf4j(topic = "utils")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ForkController extends ForkUtils {

  @Getter
  private Manager manager;

  public static ForkController instance() {
    return ForkControllerEnum.INSTANCE.getInstance();
  }

  public void init(Manager manager) {
    this.manager = manager;
    super.init(manager.getDynamicPropertiesStore());
  }

  public synchronized void update(BlockCapsule blockCapsule) {
    List<ByteString> masters = manager.getMasterScheduleStore().getActiveMasters();
    ByteString master = blockCapsule.getMasterAddress();
    int slot = masters.indexOf(master);
    if (slot < 0) {
      return;
    }
    int version = blockCapsule.getInstance().getBlockHeader().getRawData().getVersion();
    downgrade(version, slot);
    byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
    if (check(stats)) {
      upgrade(version, stats.length);
      return;
    }
    if (Objects.isNull(stats) || stats.length != masters.size()) {
      stats = new byte[masters.size()];
    }

    stats[slot] = VERSION_UPGRADE;
    manager.getDynamicPropertiesStore().statsByVersion(version, stats);
    log.info(
        "*******update hard fork:{}, master size:{}, solt:{}, master:{}, version:{}",
        Streams.zip(masters.stream(), Stream.of(ArrayUtils.toObject(stats)), Maps::immutableEntry)
            .map(e -> Maps
                .immutableEntry(DecodeUtil.createReadableString(e.getKey().toByteArray()), e.getValue()))
            .map(e -> Maps
                .immutableEntry(StringUtils.substring(e.getKey(), e.getKey().length() - 4),
                    e.getValue()))
            .collect(Collectors.toList()),
        masters.size(),
        slot,
        DecodeUtil.createReadableString(master.toByteArray()),
        version);
  }

  private enum ForkControllerEnum {
    INSTANCE;

    private ForkController instance;

    ForkControllerEnum() {
      instance = new ForkController();
    }

    private ForkController getInstance() {
      return instance;
    }
  }
}
