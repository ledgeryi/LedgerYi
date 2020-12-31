package cn.ledgeryi.chainbase.core.store;

import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.capsule.BytesCapsule;
import cn.ledgeryi.chainbase.core.db.LedgerYiStoreWithRevoking;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.chainbase.core.config.Parameter;
import cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant;

@Slf4j(topic = "DB")
@Component
public class DynamicPropertiesStore extends LedgerYiStoreWithRevoking<BytesCapsule> {

  private static final byte[] LATEST_BLOCK_HEADER_TIMESTAMP = "latest_block_header_timestamp".getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_NUMBER = "latest_block_header_number".getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_HASH = "latest_block_header_hash".getBytes();
  private static final byte[] STATE_FLAG = "state_flag".getBytes(); // 1 : is maintenance, 0 : is not maintenance
  private static final byte[] LATEST_SOLIDIFIED_BLOCK_NUM = "LATEST_SOLIDIFIED_BLOCK_NUM".getBytes();
  private static final byte[] BLOCK_FILLED_SLOTS = "BLOCK_FILLED_SLOTS".getBytes();
  private static final byte[] BLOCK_FILLED_SLOTS_INDEX = "BLOCK_FILLED_SLOTS_INDEX".getBytes();
  private static final String FORK_PREFIX = "FORK_VERSION_";
  //Used only for protobuf data filter , once，value is 0,1
  private static final byte[] ALLOW_PROTO_FILTER_NUM = "ALLOW_PROTO_FILTER_NUM".getBytes();
  //Used only for account state root, once，value is {0,1} allow is 1
  private static final byte[] ALLOW_ACCOUNT_STATE_ROOT = "ALLOW_ACCOUNT_STATE_ROOT".getBytes();
  private static final byte[] ACTIVE_DEFAULT_OPERATIONS = "ACTIVE_DEFAULT_OPERATIONS".getBytes();
  private static final byte[] MAX_CPU_TIME_OF_ONE_TX = "MAX_CPU_TIME_OF_ONE_TX".getBytes();
  //If the parameter is larger than 0, the contract is allowed to be created.
  private static final byte[] ALLOW_CREATION_OF_CONTRACTS = "ALLOW_CREATION_OF_CONTRACTS".getBytes();
  private static final byte[] ENERGY_FEE = "ENERGY_FEE".getBytes();
  //This value is only allowed to be 0, 1, -1
  private static final byte[] ALLOW_ADAPTIVE_ENERGY = "ALLOW_ADAPTIVE_ENERGY".getBytes();

  @Autowired
  private DynamicPropertiesStore(@Value("properties") String dbName) {
    super(dbName);

    try {
      this.getLatestBlockHeaderTimestamp();
    } catch (IllegalArgumentException e) {
      this.saveLatestBlockHeaderTimestamp(0);
    }

    try {
      this.getLatestBlockHeaderNumber();
    } catch (IllegalArgumentException e) {
      this.saveLatestBlockHeaderNumber(1);
    }

    try {
      this.getLatestBlockHeaderHash();
    } catch (IllegalArgumentException e) {
      this.saveLatestBlockHeaderHash(ByteString.copyFrom(ByteArray.fromHexString("00")));
    }

    try {
      this.getStateFlag();
    } catch (IllegalArgumentException e) {
      this.saveStateFlag(0);
    }

    try {
      this.getLatestSolidifiedBlockNum();
    } catch (IllegalArgumentException e) {
      this.saveLatestSolidifiedBlockNum(0);
    }

    try {
      this.getBlockFilledSlotsIndex();
    } catch (IllegalArgumentException e) {
      this.saveBlockFilledSlotsIndex(0);
    }

    try {
      this.getBlockFilledSlots();
    } catch (IllegalArgumentException e) {
      int[] blockFilledSlots = new int[getBlockFilledSlotsNumber()];
      Arrays.fill(blockFilledSlots, 1);
      this.saveBlockFilledSlots(blockFilledSlots);
    }

    try {
      this.getAllowProtoFilterNum();
    } catch (IllegalArgumentException e) {
      this.saveAllowProtoFilterNum(0);
    }

    try {
      this.getActiveDefaultOperations();
    } catch (IllegalArgumentException e) {
      String contractType = "7fff1fc0033e0000000000000000000000000000000000000000000000000000";
      byte[] bytes = ByteArray.fromHexString(contractType);
      this.saveActiveDefaultOperations(bytes);
    }

    try {
      this.getMaxCpuTimeOfOneTx();
    } catch (IllegalArgumentException e) {
      this.saveMaxCpuTimeOfOneTx(50L);
    }

    try {
      this.getEnergyFee();
    } catch (IllegalArgumentException e) {
      this.saveEnergyFee(100L);// 100 sun per energy
    }

    try {
      this.getAdaptiveResourceLimitTargetRatio();
    } catch (IllegalArgumentException e) {
      this.saveAdaptiveResourceLimitTargetRatio(14400);// 24 * 60 * 10,one minute 1/10 total limit
    }

    try {
      this.getTotalEnergyLimit();
    } catch (IllegalArgumentException e) {
      this.saveTotalEnergyLimit(50_000_000_000L);
    }

    try {
      this.getTotalEnergyCurrentLimit();
    } catch (IllegalArgumentException e) {
      this.saveTotalEnergyCurrentLimit(getTotalEnergyLimit());
    }


    try {
      this.getAllowAdaptiveEnergy();
    } catch (IllegalArgumentException e) {
      this.saveAllowAdaptiveEnergy(0);
    }

    try {
      this.getTotalEnergyWeight();
    } catch (IllegalArgumentException e) {
      this.saveTotalEnergyWeight(0L);
    }

    try {
      this.getAllowCreationOfContracts();
    } catch (IllegalArgumentException e) {
      this.saveAllowCreationOfContracts(1);
    }

  }

  public void saveAllowAdaptiveEnergy(long value) {
    this.put(ALLOW_ADAPTIVE_ENERGY, new BytesCapsule(ByteArray.fromLong(value)));
  }


  public void saveTotalEnergyLimit(long totalEnergyLimit) {
    this.put(DynamicResourceProperties.TOTAL_ENERGY_LIMIT, new BytesCapsule(ByteArray.fromLong(totalEnergyLimit)));
    long ratio = getAdaptiveResourceLimitTargetRatio();
    saveTotalEnergyTargetLimit(totalEnergyLimit / ratio);
  }

  public String intArrayToString(int[] a) {
    StringBuilder sb = new StringBuilder();
    for (int i : a) {
      sb.append(i);
    }
    return sb.toString();
  }

  public int[] stringToIntArray(String s) {
    int length = s.length();
    int[] result = new int[length];
    for (int i = 0; i < length; ++i) {
      result[i] = Integer.parseInt(s.substring(i, i + 1));
    }
    return result;
  }

  public void saveBlockFilledSlotsIndex(int blockFilledSlotsIndex) {
    log.debug("blockFilledSlotsIndex:" + blockFilledSlotsIndex);
    this.put(BLOCK_FILLED_SLOTS_INDEX, new BytesCapsule(ByteArray.fromInt(blockFilledSlotsIndex)));
  }

  public int getBlockFilledSlotsIndex() {
    return Optional.ofNullable(getUnchecked(BLOCK_FILLED_SLOTS_INDEX))
        .map(BytesCapsule::getData)
        .map(ByteArray::toInt)
        .orElseThrow(() -> new IllegalArgumentException("not found BLOCK_FILLED_SLOTS_INDEX"));
  }

  public void saveBlockFilledSlots(int[] blockFilledSlots) {
    log.debug("blockFilledSlots:" + intArrayToString(blockFilledSlots));
    this.put(BLOCK_FILLED_SLOTS, new BytesCapsule(ByteArray.fromString(intArrayToString(blockFilledSlots))));
  }

  public int[] getBlockFilledSlots() {
    return Optional.ofNullable(getUnchecked(BLOCK_FILLED_SLOTS))
        .map(BytesCapsule::getData)
        .map(ByteArray::toStr)
        .map(this::stringToIntArray)
        .orElseThrow(() -> new IllegalArgumentException("not found latest SOLIDIFIED_BLOCK_NUM timestamp"));
  }

  public void saveTotalEnergyWeight(long totalEnergyWeight) {
    this.put(DynamicResourceProperties.TOTAL_ENERGY_WEIGHT,
            new BytesCapsule(ByteArray.fromLong(totalEnergyWeight)));
  }

  public long getTotalEnergyWeight() {
    return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_ENERGY_WEIGHT))
            .map(BytesCapsule::getData)
            .map(ByteArray::toLong)
            .orElseThrow(() -> new IllegalArgumentException("not found TOTAL_ENERGY_WEIGHT"));
  }

  public int getBlockFilledSlotsNumber() {
    return ChainConstant.BLOCK_FILLED_SLOTS_NUMBER;
  }

  public void applyBlock(boolean fillBlock) {
    int[] blockFilledSlots = getBlockFilledSlots();
    int blockFilledSlotsIndex = getBlockFilledSlotsIndex();
    blockFilledSlots[blockFilledSlotsIndex] = fillBlock ? 1 : 0;
    saveBlockFilledSlotsIndex((blockFilledSlotsIndex + 1) % getBlockFilledSlotsNumber());
    saveBlockFilledSlots(blockFilledSlots);
  }

  public int calculateFilledSlotsCount() {
    int[] blockFilledSlots = getBlockFilledSlots();
    return 100 * IntStream.of(blockFilledSlots).sum() / getBlockFilledSlotsNumber();
  }

  public void saveLatestSolidifiedBlockNum(long number) {
    this.put(LATEST_SOLIDIFIED_BLOCK_NUM, new BytesCapsule(ByteArray.fromLong(number)));
  }

  public long getLatestSolidifiedBlockNum() {
    return Optional.ofNullable(getUnchecked(LATEST_SOLIDIFIED_BLOCK_NUM))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found latest SOLIDIFIED_BLOCK_NUM"));
  }

  /**
   * get timestamp of creating global latest block.
   */
  public long getLatestBlockHeaderTimestamp() {
    return Optional.ofNullable(getUnchecked(LATEST_BLOCK_HEADER_TIMESTAMP))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found latest block header timestamp"));
  }

  /**
   * get number of global latest block.
   */
  public long getLatestBlockHeaderNumber() {
    return Optional.ofNullable(getUnchecked(LATEST_BLOCK_HEADER_NUMBER))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found latest block header number"));
  }

  public int getStateFlag() {
    return Optional.ofNullable(getUnchecked(STATE_FLAG))
        .map(BytesCapsule::getData)
        .map(ByteArray::toInt)
        .orElseThrow(() -> new IllegalArgumentException("not found maintenance flag"));
  }

  /**
   * get id of global latest block.
   */
  public Sha256Hash getLatestBlockHeaderHash() {
    byte[] blockHash = Optional.ofNullable(getUnchecked(LATEST_BLOCK_HEADER_HASH))
        .map(BytesCapsule::getData)
        .orElseThrow(() -> new IllegalArgumentException("not found block hash"));
    return Sha256Hash.wrap(blockHash);
  }

  /**
   * save timestamp of creating global latest block.
   */
  public void saveLatestBlockHeaderTimestamp(long t) {
    log.debug("update latest block header timestamp = {}", t);
    this.put(LATEST_BLOCK_HEADER_TIMESTAMP, new BytesCapsule(ByteArray.fromLong(t)));
  }

  /**
   * save number of global latest block.
   */
  public void saveLatestBlockHeaderNumber(long n) {
    log.info("update latest block header number = {}", n);
    this.put(LATEST_BLOCK_HEADER_NUMBER, new BytesCapsule(ByteArray.fromLong(n)));
  }

  /**
   * save id of global latest block.
   */
  public void saveLatestBlockHeaderHash(ByteString h) {
    log.debug("update latest block header id = {}", ByteArray.toHexString(h.toByteArray()));
    this.put(LATEST_BLOCK_HEADER_HASH, new BytesCapsule(h.toByteArray()));
  }

  public void saveStateFlag(int n) {
    log.info("update state flag = {}", n);
    this.put(STATE_FLAG, new BytesCapsule(ByteArray.fromInt(n)));
  }

  public long getMaintenanceSkipSlots() {
    return Parameter.ChainConstant.MAINTENANCE_SKIP_SLOTS;
  }

  public void statsByVersion(int version, byte[] stats) {
    String statsKey = FORK_PREFIX + version;
    put(statsKey.getBytes(), new BytesCapsule(stats));
  }

  public byte[] statsByVersion(int version) {
    String statsKey = FORK_PREFIX + version;
    return revokingDB.getUnchecked(statsKey.getBytes());
  }

  /**
   * get allow protobuf number.
   */
  public long getAllowProtoFilterNum() {
    return Optional.ofNullable(getUnchecked(ALLOW_PROTO_FILTER_NUM))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found allow protobuf number"));
  }

  /**
   * save allow protobuf  number.
   */
  public void saveAllowProtoFilterNum(long num) {
    log.info("update allow protobuf number = {}", num);
    this.put(ALLOW_PROTO_FILTER_NUM, new BytesCapsule(ByteArray.fromLong(num)));
  }

  public void saveActiveDefaultOperations(byte[] value) {
    this.put(ACTIVE_DEFAULT_OPERATIONS,
            new BytesCapsule(value));
  }

  public byte[] getActiveDefaultOperations() {
    return Optional.ofNullable(getUnchecked(ACTIVE_DEFAULT_OPERATIONS))
            .map(BytesCapsule::getData)
            .orElseThrow(
                    () -> new IllegalArgumentException("not found ACTIVE_DEFAULT_OPERATIONS"));
  }

  public void saveMaxCpuTimeOfOneTx(long time) {
    this.put(MAX_CPU_TIME_OF_ONE_TX,
            new BytesCapsule(ByteArray.fromLong(time)));
  }

  public long getMaxCpuTimeOfOneTx() {
    return Optional.ofNullable(getUnchecked(MAX_CPU_TIME_OF_ONE_TX))
            .map(BytesCapsule::getData)
            .map(ByteArray::toLong)
            .orElseThrow(
                    () -> new IllegalArgumentException("not found MAX_CPU_TIME_OF_ONE_TX"));
  }

  public void saveEnergyFee(long totalEnergyFee) {
    this.put(ENERGY_FEE, new BytesCapsule(ByteArray.fromLong(totalEnergyFee)));
  }

  public long getEnergyFee() {
    return Optional.ofNullable(getUnchecked(ENERGY_FEE))
            .map(BytesCapsule::getData)
            .map(ByteArray::toLong)
            .orElseThrow(
                    () -> new IllegalArgumentException("not found ENERGY_FEE"));
  }

  public boolean supportVM() {
    return getAllowCreationOfContracts() == 1L;
  }

  public void saveAllowCreationOfContracts(long allowCreationOfContracts) {
    this.put(ALLOW_CREATION_OF_CONTRACTS, new BytesCapsule(ByteArray.fromLong(allowCreationOfContracts)));
  }

  public long getAllowCreationOfContracts() {
    return Optional.ofNullable(getUnchecked(ALLOW_CREATION_OF_CONTRACTS))
            .map(BytesCapsule::getData)
            .map(ByteArray::toLong)
            .orElseThrow(() -> new IllegalArgumentException("not found ALLOW_CREATION_OF_CONTRACTS"));
  }

  public void saveTotalEnergyCurrentLimit(long totalEnergyCurrentLimit) {
    this.put(DynamicResourceProperties.TOTAL_ENERGY_CURRENT_LIMIT,
            new BytesCapsule(ByteArray.fromLong(totalEnergyCurrentLimit)));
  }

  public long getTotalEnergyCurrentLimit() {
    return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_ENERGY_CURRENT_LIMIT))
            .map(BytesCapsule::getData)
            .map(ByteArray::toLong)
            .orElseThrow(
                    () -> new IllegalArgumentException("not found TOTAL_ENERGY_CURRENT_LIMIT"));
  }

  public void saveTotalEnergyTargetLimit(long targetTotalEnergyLimit) {
    this.put(DynamicResourceProperties.TOTAL_ENERGY_TARGET_LIMIT,
            new BytesCapsule(ByteArray.fromLong(targetTotalEnergyLimit)));
  }

  public long getAllowAdaptiveEnergy() {
    return Optional.ofNullable(getUnchecked(ALLOW_ADAPTIVE_ENERGY))
            .map(BytesCapsule::getData)
            .map(ByteArray::toLong)
            .orElseThrow(
                    () -> new IllegalArgumentException("not found ALLOW_ADAPTIVE_ENERGY"));
  }

  public void saveTotalEnergyLimit2(long totalEnergyLimit) {
    this.put(DynamicResourceProperties.TOTAL_ENERGY_LIMIT,
            new BytesCapsule(ByteArray.fromLong(totalEnergyLimit)));

    long ratio = getAdaptiveResourceLimitTargetRatio();
    saveTotalEnergyTargetLimit(totalEnergyLimit / ratio);
    if (getAllowAdaptiveEnergy() == 0) {
      saveTotalEnergyCurrentLimit(totalEnergyLimit);
    }
  }

  public long getTotalEnergyLimit() {
    return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_ENERGY_LIMIT))
            .map(BytesCapsule::getData)
            .map(ByteArray::toLong)
            .orElseThrow(() -> new IllegalArgumentException("not found TOTAL_ENERGY_LIMIT"));
  }

  public void saveAdaptiveResourceLimitTargetRatio(long adaptiveResourceLimitTargetRatio) {
    this.put(DynamicResourceProperties.ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO,
            new BytesCapsule(ByteArray.fromLong(adaptiveResourceLimitTargetRatio)));
  }

  public long getAdaptiveResourceLimitTargetRatio() {
    return Optional
            .ofNullable(getUnchecked(DynamicResourceProperties.ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO))
            .map(BytesCapsule::getData)
            .map(ByteArray::toLong)
            .orElseThrow(() -> new IllegalArgumentException("not found ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO"));
  }

  private static class DynamicResourceProperties {
    private static final byte[] ONE_DAY_NET_LIMIT = "ONE_DAY_NET_LIMIT".getBytes();
    //public free bandwidth
    private static final byte[] PUBLIC_NET_USAGE = "PUBLIC_NET_USAGE".getBytes();
    //fixed
    private static final byte[] PUBLIC_NET_LIMIT = "PUBLIC_NET_LIMIT".getBytes();
    private static final byte[] PUBLIC_NET_TIME = "PUBLIC_NET_TIME".getBytes();
    private static final byte[] FREE_NET_LIMIT = "FREE_NET_LIMIT".getBytes();
    private static final byte[] TOTAL_NET_WEIGHT = "TOTAL_NET_WEIGHT".getBytes();
    //ONE_DAY_NET_LIMIT - PUBLIC_NET_LIMIT，current TOTAL_NET_LIMIT
    private static final byte[] TOTAL_NET_LIMIT = "TOTAL_NET_LIMIT".getBytes();
    private static final byte[] TOTAL_ENERGY_TARGET_LIMIT = "TOTAL_ENERGY_TARGET_LIMIT".getBytes();
    private static final byte[] TOTAL_ENERGY_CURRENT_LIMIT = "TOTAL_ENERGY_CURRENT_LIMIT".getBytes();
    private static final byte[] TOTAL_ENERGY_AVERAGE_USAGE = "TOTAL_ENERGY_AVERAGE_USAGE".getBytes();
    private static final byte[] TOTAL_ENERGY_AVERAGE_TIME = "TOTAL_ENERGY_AVERAGE_TIME".getBytes();
    private static final byte[] TOTAL_ENERGY_WEIGHT = "TOTAL_ENERGY_WEIGHT".getBytes();
    private static final byte[] TOTAL_ENERGY_LIMIT = "TOTAL_ENERGY_LIMIT".getBytes();
    private static final byte[] BLOCK_ENERGY_USAGE = "BLOCK_ENERGY_USAGE".getBytes();
    private static final byte[] ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER = "ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER".getBytes();
    private static final byte[] ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO = "ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO".getBytes();

  }


}
