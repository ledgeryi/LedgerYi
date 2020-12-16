package cn.ledgeryi.actuator.core;

import cn.ledgeryi.chainbase.common.utils.AdjustBalanceUtil;
import cn.ledgeryi.chainbase.core.capsule.AccountCapsule;
import cn.ledgeryi.chainbase.core.capsule.TransactionResultCapsule;
import cn.ledgeryi.chainbase.core.store.AccountStore;
import cn.ledgeryi.chainbase.core.store.DynamicPropertiesStore;
import cn.ledgeryi.common.core.exception.BalanceInsufficientException;
import cn.ledgeryi.common.core.exception.ContractExeException;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.BalanceContract;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;

@Slf4j(topic = "actuator")
public class TransferActuator extends AbstractActuator {

  public TransferActuator() {
    super(Protocol.Transaction.Contract.ContractType.TransferContract, BalanceContract.TransferContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException("tx result null");
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    try {
      BalanceContract.TransferContract transferContract = any.unpack(BalanceContract.TransferContract.class);
      long amount = transferContract.getAmount();
      byte[] toAddress = transferContract.getToAddress().toByteArray();
      byte[] ownerAddress = transferContract.getOwnerAddress().toByteArray();
      // if account with to_address does not exist, create it first.
      AccountCapsule toAccount = accountStore.get(toAddress);
      if (toAccount == null) {
        toAccount = new AccountCapsule(ByteString.copyFrom(toAddress), Protocol.AccountType.Normal, dynamicStore.getLatestBlockHeaderTimestamp());
        accountStore.put(toAddress, toAccount);
      }
      ret.setStatus(Protocol.Transaction.Result.code.SUCESS);
      AdjustBalanceUtil.adjustBalance(accountStore, ownerAddress, -amount);
      AdjustBalanceUtil.adjustBalance(accountStore, toAddress, amount);
    } catch (BalanceInsufficientException | ArithmeticException | InvalidProtocolBufferException e) {
      log.debug(e.getMessage(), e);
      ret.setStatus(Protocol.Transaction.Result.code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException("contract not exist");
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException("store not exist");
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    if (!this.any.is(BalanceContract.TransferContract.class)) {
      throw new ContractValidateException("contract type error, expected type [TransferContract], real type [" + this.any.getClass() + "]");
    }
    final BalanceContract.TransferContract transferContract;
    try {
      transferContract = any.unpack(BalanceContract.TransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      log.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] toAddress = transferContract.getToAddress().toByteArray();
    byte[] ownerAddress = transferContract.getOwnerAddress().toByteArray();
    long amount = transferContract.getAmount();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress!");
    }
    if (!DecodeUtil.addressValid(toAddress)) {
      throw new ContractValidateException("Invalid toAddress!");
    }
    if (Arrays.equals(toAddress, ownerAddress)) {
      throw new ContractValidateException("Cannot transfer TX to yourself.");
    }
    AccountCapsule ownerAccount = accountStore.get(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("Validate TransferContract error, no OwnerAccount.");
    }
    long balance = ownerAccount.getBalance();
    if (amount <= 0) {
      throw new ContractValidateException("Amount must be greater than 0.");
    }
    try {
      AccountCapsule toAccount = accountStore.get(toAddress);
      if (balance < Math.addExact(amount, 0)) {
        throw new ContractValidateException("Validate TransferContract error, balance is not sufficient.");
      }
      if (toAccount != null) {
        Math.addExact(toAccount.getBalance(), amount);
      }
    } catch (ArithmeticException e) {
      log.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(BalanceContract.TransferContract.class).getOwnerAddress();
  }

}