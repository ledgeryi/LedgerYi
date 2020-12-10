package cn.ledgeryi.chainbase.core.capsule;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.chainbase.core.store.DynamicPropertiesStore;
import cn.ledgeryi.protos.Protocol.Account;
import cn.ledgeryi.protos.Protocol.Account.Builder;
import cn.ledgeryi.protos.Protocol.AccountType;
import cn.ledgeryi.protos.Protocol.Key;
import cn.ledgeryi.protos.Protocol.Permission;
import cn.ledgeryi.protos.Protocol.Permission.PermissionType;
import cn.ledgeryi.protos.Protocol.Vote;
import cn.ledgeryi.protos.contract.AccountContract.AccountCreateContract;
import cn.ledgeryi.protos.contract.AccountContract.AccountUpdateContract;

@Slf4j(topic = "capsule")
public class AccountCapsule implements ProtoCapsule<Account>, Comparable<AccountCapsule> {

  private Account account;


  /**
   * get account from bytes data.
   */
  public AccountCapsule(byte[] data) {
    try {
      this.account = Account.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      log.debug(e.getMessage());
    }
  }

  /**
   * initial account capsule.
   */
  public AccountCapsule(ByteString accountName, ByteString address, AccountType accountType, long balance) {
    this.account = Account.newBuilder()
            .setAccountName(accountName)
            .setType(accountType)
            .setAddress(address)
            .setBalance(balance)
            .build();
  }

  /**
   * construct account from AccountCreateContract.
   */
  public AccountCapsule(final AccountCreateContract contract) {
    this.account = Account.newBuilder()
        .setType(contract.getType())
        .setAddress(contract.getAccountAddress())
        .setTypeValue(contract.getTypeValue())
        .build();
  }

  /**
   * construct account from AccountCreateContract and createTime.
   */
  public AccountCapsule(final AccountCreateContract contract, long createTime,DynamicPropertiesStore dynamicPropertiesStore) {
      this.account = Account.newBuilder()
          .setType(contract.getType())
          .setAddress(contract.getAccountAddress())
          .setTypeValue(contract.getTypeValue())
          .setCreateTime(createTime)
          .build();
    }

  /**
   * get account from address and account name.
   */
  public AccountCapsule(ByteString address, ByteString accountName, AccountType accountType) {
    this.account = Account.newBuilder()
        .setType(accountType)
        .setAccountName(accountName)
        .setAddress(address)
        .build();
  }

  /**
   * get account from address.
   */
  public AccountCapsule(ByteString address, AccountType accountType) {
    this.account = Account.newBuilder()
        .setType(accountType)
        .setAddress(address)
        .build();
  }

  /**
   * get account from address.
   */
  public AccountCapsule(ByteString address, AccountType accountType, long createTime) {
    this.account = Account.newBuilder()
            .setType(accountType)
            .setAddress(address)
            .setCreateTime(createTime)
            .build();
  }

  /**
   * get account from address.
   */
  public AccountCapsule(Account account) {
    this.account = account;
  }

  public byte[] getData() {
    return this.account.toByteArray();
  }

  @Override
  public Account getInstance() {
    return this.account;
  }

  public void setInstance(Account account) {
    this.account = account;
  }

  public ByteString getAddress() {
    return this.account.getAddress();
  }

  public byte[] createDbKey() {
    return getAddress().toByteArray();
  }

  public String createReadableString() {
    return ByteArray.toHexString(getAddress().toByteArray());
  }

  public AccountType getType() {
    return this.account.getType();
  }

  public ByteString getAccountName() {
    return this.account.getAccountName();
  }

  /**
   * set account name
   */
  public void setAccountName(byte[] name) {
    this.account = this.account.toBuilder().setAccountName(ByteString.copyFrom(name)).build();
  }

  public ByteString getAccountId() {
    return this.account.getAccountId();
  }

  /**
   * set account id
   */
  public void setAccountId(byte[] id) {
    this.account = this.account.toBuilder().setAccountId(ByteString.copyFrom(id)).build();
  }

  public void setBalance(long balance) {
    this.account = this.account.toBuilder().setBalance(balance).build();
  }
  public long getBalance() {
    return this.account.getBalance();
  }

  public byte[] getMasterPermissionAddress() {
    if (this.account.getMasterPermission().getKeysCount() == 0) {
      return getAddress().toByteArray();
    } else {
      return this.account.getMasterPermission().getKeys(0).getAddress().toByteArray();
    }
  }

  public long getLatestOperationTime() {
    return this.account.getLatestOprationTime();
  }

  public void setLatestOperationTime(long latest_time) {
    this.account = this.account.toBuilder().setLatestOprationTime(latest_time).build();
  }

  public long getLatestConsumeTime() {
    return this.account.getLatestConsumeTime();
  }

  public void setLatestConsumeTime(long latest_time) {
    this.account = this.account.toBuilder().setLatestConsumeTime(latest_time).build();
  }

  @Override
  public String toString() {
    return this.account.toString();
  }

  public void clearVotes() {
    this.account = this.account.toBuilder()
        .clearVotes()
        .build();
  }

  /**
   * get votes.
   */
  public List<Vote> getVotesList() {
    if (this.account.getVotesList() != null) {
      return this.account.getVotesList();
    } else {
      return Lists.newArrayList();
    }
  }

  public long getLatestAssetOperationTimeV2(String assetName) {
    return this.account.getLatestAssetOperationTimeV2OrDefault(assetName, 0);
  }

  public ByteString getAssetIssuedName() {
    return getInstance().getAssetIssuedName();
  }

  public void setAssetIssuedID(byte[] id) {
    ByteString assetIssuedID = ByteString.copyFrom(id);
    this.account = this.account.toBuilder().setAssetIssuedID(assetIssuedID).build();
  }

  public long getAllowance() {
    return getInstance().getAllowance();
  }

  public void setAllowance(long allowance) {
    this.account = this.account.toBuilder().setAllowance(allowance).build();
  }

  public long getLatestWithdrawTime() {
    return getInstance().getLatestWithdrawTime();
  }

  public boolean getIsMaster() {
    return getInstance().getIsMaster();
  }

  public void setIsMaster(boolean isMaster) {
    this.account = this.account.toBuilder().setIsMaster(isMaster).build();
  }

  public boolean getIsCommittee() {
    return getInstance().getIsCommittee();
  }

  public void setIsCommittee(boolean isCommittee) {
    this.account = this.account.toBuilder().setIsCommittee(isCommittee).build();
  }

  public void updateAccountType(AccountType accountType) {
    this.account = this.account.toBuilder().setType(accountType).build();
  }

  @Override
  public int compareTo(AccountCapsule otherObject) {
    return 0;
  }
}