package cn.ledgeryi.chainbase.common.utils;

import cn.ledgeryi.chainbase.core.config.Parameter;
import cn.ledgeryi.chainbase.core.store.DynamicPropertiesStore;
import cn.ledgeryi.chainbase.core.store.MasterStore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;

@Slf4j(topic = "utils")
public class ForkUtils {

  protected static final byte VERSION_DOWNGRADE = (byte) 0;
  protected static final byte VERSION_UPGRADE = (byte) 1;
  protected static final byte[] check;

  static {
    check = new byte[1024];
    Arrays.fill(check, VERSION_UPGRADE);
  }

  @Setter
  @Getter
  protected DynamicPropertiesStore dynamicPropertiesStore;

  protected MasterStore masterStore;

  public void init(DynamicPropertiesStore dynamicPropertiesStore) {
    this.dynamicPropertiesStore = dynamicPropertiesStore;
  }

  public boolean pass(Parameter.ForkBlockVersionEnum forkBlockVersionEnum) {
    return pass(forkBlockVersionEnum.getValue());
  }

  public synchronized boolean pass(int version) {
    byte[] stats = dynamicPropertiesStore.statsByVersion(version);
    return check(stats);
  }

  protected boolean check(byte[] stats) {
    if (stats == null || stats.length == 0) {
      return false;
    }

    for (int i = 0; i < stats.length; i++) {
      if (check[i] != stats[i]) {
        return false;
      }
    }

    return true;
  }

  protected void downgrade(int version, int slot) {
    for (Parameter.ForkBlockVersionEnum versionEnum : Parameter.ForkBlockVersionEnum.values()) {
      int versionValue = versionEnum.getValue();
      if (versionValue > version) {
        byte[] stats = dynamicPropertiesStore.statsByVersion(versionValue);
        if (!check(stats) && Objects.nonNull(stats)) {
          stats[slot] = VERSION_DOWNGRADE;
          dynamicPropertiesStore.statsByVersion(versionValue, stats);
        }
      }
    }
  }

  protected void upgrade(int version, int slotSize) {
    for (Parameter.ForkBlockVersionEnum versionEnum : Parameter.ForkBlockVersionEnum.values()) {
      int versionValue = versionEnum.getValue();
      if (versionValue < version) {
        byte[] stats = dynamicPropertiesStore.statsByVersion(versionValue);
        if (!check(stats)) {
          if (stats == null || stats.length == 0) {
            stats = new byte[slotSize];
          }
          Arrays.fill(stats, VERSION_UPGRADE);
          dynamicPropertiesStore.statsByVersion(versionValue, stats);
        }
      }
    }
  }


  public synchronized void reset() {
    for (Parameter.ForkBlockVersionEnum versionEnum : Parameter.ForkBlockVersionEnum.values()) {
      int versionValue = versionEnum.getValue();
      byte[] stats = dynamicPropertiesStore.statsByVersion(versionValue);
      if (!check(stats) && Objects.nonNull(stats)) {
        Arrays.fill(stats, VERSION_DOWNGRADE);
        dynamicPropertiesStore.statsByVersion(versionValue, stats);
      }
    }
  }
}
