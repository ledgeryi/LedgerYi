package cn.ledgeryi.framework.common.storage;

import cn.ledgeryi.chainbase.core.capsule.*;
import cn.ledgeryi.chainbase.core.db.BlockStore;
import cn.ledgeryi.chainbase.core.store.*;
import cn.ledgeryi.common.core.exception.BadItemException;
import cn.ledgeryi.common.core.exception.ItemNotFoundException;
import cn.ledgeryi.common.runtime.vm.DataWord;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.contract.vm.config.VmConfig;
import cn.ledgeryi.contract.vm.program.Storage;
import cn.ledgeryi.contract.vm.repository.Key;
import cn.ledgeryi.contract.vm.repository.Type;
import cn.ledgeryi.contract.vm.repository.Value;
import cn.ledgeryi.crypto.utils.Hash;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.framework.core.db.TransactionStore;
import cn.ledgeryi.protos.Protocol;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.Strings;

import java.util.HashMap;

@Slf4j(topic = "deposit")
public class DepositImpl implements Deposit {
  private Manager dbManager;
  private Deposit parent = null;

  private HashMap<Key, Value> accountCache = new HashMap<>();
  private HashMap<Key, Value> transactionCache = new HashMap<>();
  private HashMap<Key, Value> blockCache = new HashMap<>();
  private HashMap<Key, Value> witnessCache = new HashMap<>();
  private HashMap<Key, Value> codeCache = new HashMap<>();
  private HashMap<Key, Value> contractCache = new HashMap<>();
  private HashMap<Key, Storage> storageCache = new HashMap<>();

  private DepositImpl(Manager dbManager, DepositImpl parent) {
    init(dbManager, parent);
  }

  public static DepositImpl createRoot(Manager dbManager) {
    return new DepositImpl(dbManager, null);
  }

  protected void init(Manager dbManager, DepositImpl parent) {
    this.dbManager = dbManager;
    this.parent = parent;
  }

  @Override
  public Manager getDbManager() {
    return dbManager;
  }

  private BlockStore getBlockStore() {
    return dbManager.getBlockStore();
  }

  private TransactionStore getTransactionStore() {
    return dbManager.getTransactionStore();
  }

  private ContractStore getContractStore() {
    return dbManager.getContractStore();
  }

  private MasterStore getWitnessStore() {
    return dbManager.getMasterStore();
  }

  private VotesStore getVotesStore() {
    return dbManager.getVotesStore();
  }

  private DynamicPropertiesStore getDynamicPropertiesStore() {
    return dbManager.getDynamicPropertiesStore();
  }

  private AccountStore getAccountStore() {
    return dbManager.getAccountStore();
  }

  private CodeStore getCodeStore() {
    return dbManager.getCodeStore();
  }

  @Override
  public Deposit newDepositChild() {
    return new DepositImpl(dbManager, this);
  }

  @Override
  public synchronized AccountCapsule createAccount(byte[] address, Protocol.AccountType type) {
    Key key = new Key(address);
    AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address), type);
    accountCache.put(key, new Value(account.getData(), Type.VALUE_TYPE_CREATE));
    return account;
  }

  @Override
  public AccountCapsule createAccount(byte[] address, String accountName, Protocol.AccountType type) {
    Key key = new Key(address);
    AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address),
        ByteString.copyFromUtf8(accountName), type);
    accountCache.put(key, new Value(account.getData(), Type.VALUE_TYPE_CREATE));
    return account;
  }

  @Override
  public synchronized AccountCapsule getAccount(byte[] address) {
    Key key = new Key(address);
    if (accountCache.containsKey(key)) {
      return accountCache.get(key).getAccount();
    }
    AccountCapsule accountCapsule;
    if (parent != null) {
      accountCapsule = parent.getAccount(address);
    } else {
      accountCapsule = getAccountStore().get(address);
    }
    if (accountCapsule != null) {
      accountCache.put(key, Value.create(accountCapsule.getData()));
    }
    return accountCapsule;
  }

  @Override
  public MasterCapsule getWitness(byte[] address) {
    Key key = new Key(address);
    if (witnessCache.containsKey(key)) {
      return witnessCache.get(key).getMaster();
    }
    MasterCapsule witnessCapsule;
    if (parent != null) {
      witnessCapsule = parent.getWitness(address);
    } else {
      witnessCapsule = getWitnessStore().get(address);
    }

    if (witnessCapsule != null) {
      witnessCache.put(key, Value.create(witnessCapsule.getData()));
    }
    return witnessCapsule;
  }

  // just for depositRoot
  @Override
  public void deleteContract(byte[] address) {
    getCodeStore().delete(address);
    getAccountStore().delete(address);
    getContractStore().delete(address);
  }

  @Override
  public synchronized void createContract(byte[] address, ContractCapsule contractCapsule) {
    Key key = Key.create(address);
    Value value = Value.create(contractCapsule.getData(), Type.VALUE_TYPE_CREATE);
    contractCache.put(key, value);
  }

  @Override
  public void updateContract(byte[] address, ContractCapsule contractCapsule) {
    Key key = Key.create(address);
    Value value = Value.create(contractCapsule.getData(), Type.VALUE_TYPE_DIRTY);
    contractCache.put(key, value);
  }

  @Override
  public void updateAccount(byte[] address, AccountCapsule accountCapsule) {
    Key key = Key.create(address);
    Value value = Value.create(accountCapsule.getData(), Type.VALUE_TYPE_DIRTY);
    accountCache.put(key, value);
  }

  @Override
  public synchronized ContractCapsule getContract(byte[] address) {
    Key key = Key.create(address);
    if (contractCache.containsKey(key)) {
      return contractCache.get(key).getContract();
    }
    ContractCapsule contractCapsule;
    if (parent != null) {
      contractCapsule = parent.getContract(address);
    } else {
      contractCapsule = getContractStore().get(address);
    }
    if (contractCapsule != null) {
      contractCache.put(key, Value.create(contractCapsule.getData()));
    }
    return contractCapsule;
  }

  @Override
  public synchronized void saveCode(byte[] address, byte[] code) {
    Key key = Key.create(address);
    Value value = Value.create(code, Type.VALUE_TYPE_CREATE);
    codeCache.put(key, value);
    if (VmConfig.allowTvmConstantinople()) {
      ContractCapsule contract = getContract(address);
      byte[] codeHash = Hash.sha3(code);
      contract.setCodeHash(codeHash);
      updateContract(address, contract);
    }
  }

  @Override
  public synchronized byte[] getCode(byte[] address) {
    Key key = Key.create(address);
    if (codeCache.containsKey(key)) {
      return codeCache.get(key).getCode().getData();
    }
    byte[] code;
    if (parent != null) {
      code = parent.getCode(address);
    } else {
      if (null == getCodeStore().get(address)) {
        code = null;
      } else {
        code = getCodeStore().get(address).getData();
      }
    }
    if (code != null) {
      codeCache.put(key, Value.create(code));
    }
    return code;
  }

  @Override
  public synchronized void putStorageValue(byte[] address, DataWord key, DataWord value) {
    if (getAccount(address) == null) {
      return;
    }
    Key addressKey = Key.create(address);
    Storage storage;
    if (storageCache.containsKey(addressKey)) {
      storage = storageCache.get(addressKey);
    } else {
      storage = getStorage(address);
      storageCache.put(addressKey, storage);
    }
    storage.put(key, value);
  }

  @Override
  public synchronized Storage getStorage(byte[] address) {
    Key key = Key.create(address);
    if (storageCache.containsKey(key)) {
      return storageCache.get(key);
    }
    Storage storage;
    if (this.parent != null) {
      storage = parent.getStorage(address);
    } else {
      storage = new Storage(address, dbManager.getStorageRowStore());
    }
    ContractCapsule contract = getContract(address);
    if (contract != null && !ByteUtil.isNullOrZeroArray(contract.getTrxHash())) {
      storage.generateAddrHash(contract.getTrxHash());
    }
    return storage;
  }

  @Override
  public synchronized DataWord getStorageValue(byte[] address, DataWord key) {
    if (getAccount(address) == null) {
      return null;
    }
    Key addressKey = Key.create(address);
    Storage storage;
    if (storageCache.containsKey(addressKey)) {
      storage = storageCache.get(addressKey);
    } else {
      storage = getStorage(address);
      storageCache.put(addressKey, storage);
    }
    return storage.getValue(key);
  }

  @Override
  public synchronized long getBalance(byte[] address) {
    AccountCapsule accountCapsule = getAccount(address);
    return accountCapsule == null ? 0L : accountCapsule.getBalance();
  }

  @Override
  public synchronized long addBalance(byte[] address, long value) {
    AccountCapsule accountCapsule = getAccount(address);
    if (accountCapsule == null) {
      accountCapsule = createAccount(address, Protocol.AccountType.Normal);
    }

    long balance = accountCapsule.getBalance();
    if (value == 0) {
      return balance;
    }

    if (value < 0 && balance < -value) {
      throw new RuntimeException(DecodeUtil.createReadableString(accountCapsule.createDbKey()) + " insufficient balance");
    }
    accountCapsule.setBalance(Math.addExact(balance, value));
    Key key = Key.create(address);
    Value val = Value.create(accountCapsule.getData(),
        Type.VALUE_TYPE_DIRTY | accountCache.get(key).getType().getType());
    accountCache.put(key, val);
    return accountCapsule.getBalance();
  }

  @Override
  public TransactionCapsule getTransaction(byte[] trxHash) {
    Key key = Key.create(trxHash);
    if (transactionCache.containsKey(key)) {
      return transactionCache.get(key).getTransaction();
    }
    TransactionCapsule transactionCapsule;
    if (parent != null) {
      transactionCapsule = parent.getTransaction(trxHash);
    } else {
      try {
        transactionCapsule = getTransactionStore().get(trxHash);
      } catch (BadItemException e) {
        transactionCapsule = null;
      }
    }
    if (transactionCapsule != null) {
      transactionCache.put(key, Value.create(transactionCapsule.getData()));
    }
    return transactionCapsule;
  }

  @Override
  public BlockCapsule getBlock(byte[] blockHash) {
    Key key = Key.create(blockHash);
    if (blockCache.containsKey(key)) {
      return blockCache.get(key).getBlock();
    }
    BlockCapsule ret;
    try {
      if (parent != null) {
        ret = parent.getBlock(blockHash);
      } else {
        ret = getBlockStore().get(blockHash);
      }
    } catch (Exception e) {
      ret = null;
    }

    if (ret != null) {
      blockCache.put(key, Value.create(ret.getData()));
    }
    return ret;
  }

  @Override
  public void putAccount(Key key, Value value) {
    accountCache.put(key, value);
  }

  @Override
  public void putTransaction(Key key, Value value) {
    transactionCache.put(key, value);
  }

  @Override
  public void putBlock(Key key, Value value) {
    blockCache.put(key, value);
  }

  @Override
  public void putWitness(Key key, Value value) {
    witnessCache.put(key, value);
  }

  @Override
  public void putCode(Key key, Value value) {
    codeCache.put(key, value);
  }

  @Override
  public void putContract(Key key, Value value) {
    contractCache.put(key, value);
  }

  @Override
  public void putStorage(Key key, Storage cache) {
    storageCache.put(key, cache);
  }

  private void commitAccountCache(Deposit deposit) {
    accountCache.forEach((key, value) -> {
      if (value.getType().isCreate() || value.getType().isDirty()) {
        if (deposit != null) {
          deposit.putAccount(key, value);
        } else {
          getAccountStore().put(key.getData(), value.getAccount());
        }
      }
    });
  }

  private void commitTransactionCache(Deposit deposit) {
    transactionCache.forEach((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putTransaction(key, value);
        } else {
          getTransactionStore().put(key.getData(), value.getTransaction());
        }
      }
    });
  }

  private void commitBlockCache(Deposit deposit) {
    blockCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putBlock(key, value);
        } else {
          getBlockStore().put(key.getData(), value.getBlock());
        }
      }
    }));
  }

  private void commitWitnessCache(Deposit deposit) {
    witnessCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putWitness(key, value);
        } else {
          getWitnessStore().put(key.getData(), value.getMaster());
        }
      }
    }));
  }

  private void commitCodeCache(Deposit deposit) {
    codeCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putCode(key, value);
        } else {
          getCodeStore().put(key.getData(), value.getCode());
        }
      }
    }));
  }

  private void commitContractCache(Deposit deposit) {
    contractCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putContract(key, value);
        } else {
          getContractStore().put(key.getData(), value.getContract());
        }
      }
    }));
  }

  private void commitStorageCache(Deposit deposit) {
    storageCache.forEach((Key address, Storage storage) -> {
      if (deposit != null) {
        // write to parent cache
        deposit.putStorage(address, storage);
      } else {
        // persistence
        storage.commit();
      }
    });
  }

  @Override
  public synchronized void commit() {
    Deposit deposit = null;
    if (parent != null) {
      deposit = parent;
    }
    commitAccountCache(deposit);
    commitTransactionCache(deposit);
    commitBlockCache(deposit);
    commitWitnessCache(deposit);
    commitCodeCache(deposit);
    commitContractCache(deposit);
    commitStorageCache(deposit);
  }

  @Override
  public void setParent(Deposit deposit) {
    parent = deposit;
  }
}

