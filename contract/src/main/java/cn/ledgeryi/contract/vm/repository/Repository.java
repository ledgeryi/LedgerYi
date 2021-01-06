package cn.ledgeryi.contract.vm.repository;

import cn.ledgeryi.chainbase.core.capsule.*;
import cn.ledgeryi.chainbase.core.store.DynamicPropertiesStore;
import cn.ledgeryi.common.runtime.vm.DataWord;
import cn.ledgeryi.contract.vm.program.Storage;
import cn.ledgeryi.protos.Protocol;

public interface Repository {

  DynamicPropertiesStore getDynamicPropertiesStore();

  AccountCapsule createAccount(byte[] address, Protocol.AccountType type);

  AccountCapsule createAccount(byte[] address, String accountName, Protocol.AccountType type);

  AccountCapsule getAccount(byte[] address);

  BytesCapsule getDynamic(byte[] bytesKey);

  void deleteContract(byte[] address);

  void createContract(byte[] address, ContractCapsule contractCapsule);

  ContractCapsule getContract(byte[] address);

  void updateContract(byte[] address, ContractCapsule contractCapsule);

  void updateAccount(byte[] address, AccountCapsule accountCapsule);

  void saveCode(byte[] address, byte[] code);

  byte[] getCode(byte[] address);

  void putStorageValue(byte[] address, DataWord key, DataWord value);

  void putStorageConsumeValue(byte[] address, long value);

  void putCpuTimeConsumeValue(byte[] address, long value);

  DataWord getStorageValue(byte[] address, DataWord key);

  Storage getStorage(byte[] address);

  long getBalance(byte[] address);

  long addBalance(byte[] address, long value);

  Repository newRepositoryChild();

  void setParent(Repository deposit);

  void commit();

  void putAccount(Key key, Value value);

  void putCode(Key key, Value value);

  void putContract(Key key, Value value);

  void putStorage(Key key, Storage cache);

  void putAccountValue(byte[] address, AccountCapsule accountCapsule);

  byte[] getBlackHoleAddress();

  public BlockCapsule getBlockByNum(final long num);

  public AccountCapsule createNormalAccount(byte[] address);
}
