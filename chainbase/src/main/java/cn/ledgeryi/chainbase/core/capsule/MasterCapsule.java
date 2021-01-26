package cn.ledgeryi.chainbase.core.capsule;

import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.crypto.utils.Hash;
import cn.ledgeryi.protos.Protocol.Master;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "capsule")
public class MasterCapsule implements ProtoCapsule<Master>, Comparable<MasterCapsule> {

  private Master master;


  /**
   * MasterCapsule constructor with pubKey and url.
   */
  public MasterCapsule(final ByteString pubKey, final String url) {
    final Master.Builder MasterBuilder = Master.newBuilder();
    this.master = MasterBuilder
        .setPubKey(pubKey)
        .setAddress(ByteString.copyFrom(Hash.computeAddress(pubKey.toByteArray()))).build();
  }

  public MasterCapsule(final Master master) {
    this.master = master;
  }

  /**
   * MasterCapsule constructor with address.
   */
  public MasterCapsule(final ByteString address) {
    this.master = cn.ledgeryi.protos.Protocol.Master.newBuilder().setAddress(address).build();
  }

  public MasterCapsule(final byte[] data) {
    try {
      this.master = cn.ledgeryi.protos.Protocol.Master.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      log.debug(e.getMessage(), e);
    }
  }

  public ByteString getAddress() {
    return this.master.getAddress();
  }

  public byte[] createDbKey() {
    return getAddress().toByteArray();
  }

  public String createReadableString() {
    return ByteArray.toHexString(getAddress().toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.master.toByteArray();
  }

  @Override
  public Master getInstance() {
    return this.master;
  }

  public void setPubKey(final ByteString pubKey) {
    this.master = this.master.toBuilder().setPubKey(pubKey).build();
  }

  public long getTotalProduced() {
    return this.master.getTotalProduced();
  }

  public void setTotalProduced(final long totalProduced) {
    this.master = this.master.toBuilder().setTotalProduced(totalProduced).build();
  }

  public long getTotalMissed() {
    return this.master.getTotalMissed();
  }

  public void setTotalMissed(final long totalMissed) {
    this.master = this.master.toBuilder().setTotalMissed(totalMissed).build();
  }

  public long getLatestBlockNum() {
    return this.master.getLatestBlockNum();
  }

  public void setLatestBlockNum(final long latestBlockNum) {
    this.master = this.master.toBuilder().setLatestBlockNum(latestBlockNum).build();
  }

  public long getLatestSlotNum() {
    return this.master.getLatestSlotNum();
  }

  public void setLatestSlotNum(final long latestSlotNum) {
    this.master = this.master.toBuilder().setLatestSlotNum(latestSlotNum).build();
  }

  public boolean getIsJobs() {
    return this.master.getIsJobs();
  }

  public void setIsJobs(final boolean isJobs) {
    this.master = this.master.toBuilder().setIsJobs(isJobs).build();
  }

  @Override
  public int compareTo(MasterCapsule otherMaster) {
    String thisMasterAddress = DecodeUtil.createReadableString(this.getAddress());
    return thisMasterAddress.compareTo(DecodeUtil.createReadableString(otherMaster.getAddress()));
  }
}
