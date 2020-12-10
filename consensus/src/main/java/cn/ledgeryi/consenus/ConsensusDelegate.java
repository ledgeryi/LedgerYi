package cn.ledgeryi.consenus;

import cn.ledgeryi.chainbase.core.capsule.MasterCapsule;
import cn.ledgeryi.chainbase.core.store.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j(topic = "consensus")
@Component
public class ConsensusDelegate {

  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;

  @Autowired
  private MasterStore masterStore;

  @Autowired
  private MasterScheduleStore masterScheduleStore;


  public int calculateFilledSlotsCount() {
    return dynamicPropertiesStore.calculateFilledSlotsCount();
  }

  public long getLatestBlockHeaderTimestamp() {
    return dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
  }

  public long getLatestBlockHeaderNumber() {
    return dynamicPropertiesStore.getLatestBlockHeaderNumber();
  }

  public boolean lastHeadBlockIsMaintenance() {
    return dynamicPropertiesStore.getStateFlag() == 1;
  }

  public long getMaintenanceSkipSlots() {
    return dynamicPropertiesStore.getMaintenanceSkipSlots();
  }

  public void saveActiveMasters(List<ByteString> addresses) {
    masterScheduleStore.saveActiveMasters(addresses);
  }

  public List<ByteString> getActiveMasters() {
    return masterScheduleStore.getActiveMasters();
  }

  public MasterCapsule getMaster(byte[] address) {
    return masterStore.get(address);
  }

  public void saveMaster(MasterCapsule masterCapsule) {
    masterStore.put(masterCapsule.createDbKey(), masterCapsule);
  }

  public List<MasterCapsule> getAllMasters() {
    return masterStore.getAllMasteres();
  }

  public long getLatestSolidifiedBlockNum() {
    return dynamicPropertiesStore.getLatestSolidifiedBlockNum();
  }

  public void saveLatestSolidifiedBlockNum(long num) {
    dynamicPropertiesStore.saveLatestSolidifiedBlockNum(num);
  }

  public void applyBlock(boolean flag) {
    dynamicPropertiesStore.applyBlock(flag);
  }
}