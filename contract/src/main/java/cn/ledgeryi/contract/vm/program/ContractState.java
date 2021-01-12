/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package cn.ledgeryi.contract.vm.program;

import cn.ledgeryi.chainbase.core.capsule.AccountCapsule;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.BytesCapsule;
import cn.ledgeryi.chainbase.core.capsule.ContractCapsule;
import cn.ledgeryi.chainbase.core.store.DynamicPropertiesStore;
import cn.ledgeryi.common.runtime.vm.DataWord;
import cn.ledgeryi.contract.vm.program.invoke.ProgramInvoke;
import cn.ledgeryi.contract.vm.program.listener.ProgramListener;
import cn.ledgeryi.contract.vm.program.listener.ProgramListenerAware;
import cn.ledgeryi.contract.vm.repository.Key;
import cn.ledgeryi.contract.vm.repository.Repository;
import cn.ledgeryi.contract.vm.repository.Value;
import cn.ledgeryi.protos.Protocol;

public class ContractState implements Repository, ProgramListenerAware {

  // contract address
  private final DataWord address;
  private Repository repository;
  private ProgramListener programListener;

  ContractState(ProgramInvoke programInvoke) {
    this.address = programInvoke.getContractAddress();
    this.repository = programInvoke.getDeposit();
  }

  @Override
  public void setProgramListener(ProgramListener listener) {
    this.programListener = listener;
  }

  @Override
  public DynamicPropertiesStore getDynamicPropertiesStore() {
    return repository.getDynamicPropertiesStore();
  }

  @Override
  public AccountCapsule createAccount(byte[] addr, Protocol.AccountType type) {
    return repository.createAccount(addr, type);
  }

  @Override
  public AccountCapsule createAccount(byte[] address, String accountName, Protocol.AccountType type) {
    return repository.createAccount(address, accountName, type);
  }

  @Override
  public AccountCapsule getAccount(byte[] addr) {
    return repository.getAccount(addr);
  }

  public BytesCapsule getDynamic(byte[] bytesKey) {
    return repository.getDynamic(bytesKey);
  }

  @Override
  public void deleteContract(byte[] address) {
    repository.deleteContract(address);
  }

  @Override
  public void createContract(byte[] codeHash, ContractCapsule contractCapsule) {
    repository.createContract(codeHash, contractCapsule);
  }

  @Override
  public ContractCapsule getContract(byte[] codeHash) {
    return repository.getContract(codeHash);
  }

  @Override
  public void updateContract(byte[] address, ContractCapsule contractCapsule) {
    repository.updateContract(address, contractCapsule);
  }

  @Override
  public void updateAccount(byte[] address, AccountCapsule accountCapsule) {
    repository.updateAccount(address, accountCapsule);
  }

  @Override
  public void saveCode(byte[] address, byte[] code) {
    repository.saveCode(address, code);
  }

  @Override
  public byte[] getCode(byte[] address) {
    return repository.getCode(address);
  }

  @Override
  public void putStorageValue(byte[] addr, DataWord key, DataWord value) {
    if (canListenTrace(addr)) {
      programListener.onStoragePut(key, value);
    }
    repository.putStorageValue(addr, key, value);
  }

  private boolean canListenTrace(byte[] address) {
    return (programListener != null) && this.address.equals(new DataWord(address));
  }

  @Override
  public DataWord getStorageValue(byte[] addr, DataWord key) {
    return repository.getStorageValue(addr, key);
  }


  @Override
  public Repository newRepositoryChild() {
    return repository.newRepositoryChild();
  }

  @Override
  public void setParent(Repository repository) {
    this.repository.setParent(repository);
  }

  @Override
  public void commit() {
    repository.commit();
  }

  @Override
  public void putAccount(Key key, Value value) {
    repository.putAccount(key, value);
  }

  @Override
  public void putCode(Key key, Value value) {
    repository.putCode(key, value);
  }


  @Override
  public void putContract(Key key, Value value) {
    repository.putContract(key, value);
  }


  public void putStorage(Key key, Storage cache) {
    repository.putStorage(key, cache);
  }

  @Override
  public Storage getStorage(byte[] address) {
    return repository.getStorage(address);
  }

  @Override
  public void putAccountValue(byte[] address, AccountCapsule accountCapsule) {
    this.repository.putAccountValue(address, accountCapsule);
  }

  @Override
  public byte[] getBlackHoleAddress() {
    return repository.getBlackHoleAddress();
  }

  @Override
  public BlockCapsule getBlockByNum(long num) {
    return repository.getBlockByNum(num);
  }

  @Override
  public AccountCapsule createNormalAccount(byte[] address) {
    return repository.createNormalAccount(address);
  }

  @Override
  public void putStorageUsedValue(byte[] address, long value) {

  }

  @Override
  public void putCpuTimeUsedValue(byte[] address, long value) {

  }
}
