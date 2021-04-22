package cn.ledgeryi.contract.core;

import cn.ledgeryi.chainbase.core.capsule.AccountCapsule;
import cn.ledgeryi.chainbase.core.capsule.ContractCapsule;
import cn.ledgeryi.chainbase.core.capsule.TransactionResultCapsule;
import cn.ledgeryi.chainbase.core.store.AccountStore;
import cn.ledgeryi.chainbase.core.store.ContractStore;
import cn.ledgeryi.common.core.exception.ContractExeException;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.Transaction.Result.code;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.ClearABIContract;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;

@Slf4j(topic = "actuator")
public class ClearABIContractActuator extends AbstractActuator {

  public ClearABIContractActuator() {
    super(Protocol.Transaction.Contract.ContractType.ClearABIContract, ClearABIContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule)result;
    if (Objects.isNull(ret)){
      throw new RuntimeException("TransactionResultCapsule is null");
    }
    ContractStore contractStore = chainBaseManager.getContractStore();
    try {
      ClearABIContract usContract = any.unpack(ClearABIContract.class);
      byte[] contractAddress = usContract.getContractAddress().toByteArray();
      ContractCapsule deployedContract = contractStore.get(contractAddress);
      deployedContract.clearABI();
      contractStore.put(contractAddress, deployedContract);
      ret.setStatus(code.SUCESS);
    } catch (InvalidProtocolBufferException e) {
      log.error(e.getMessage(), e);
      ret.setStatus(code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException("No contract!");
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException("No account store or contract store!");
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    ContractStore contractStore = chainBaseManager.getContractStore();
    if (!this.any.is(ClearABIContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ClearABIContract],real type[" + any.getClass() + "]");
    }
    final ClearABIContract contract;
    try {
      contract = this.any.unpack(ClearABIContract.class);
    } catch (InvalidProtocolBufferException e) {
      log.error(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    if (!DecodeUtil.addressValid(contract.getOwnerAddress().toByteArray())) {
      throw new ContractValidateException("Invalid address");
    }
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = DecodeUtil.createReadableString(ownerAddress);
    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      throw new ContractValidateException("Account[" + readableOwnerAddress + "] not exists");
    }
    byte[] contractAddress = contract.getContractAddress().toByteArray();
    ContractCapsule deployedContract = contractStore.get(contractAddress);
    if (deployedContract == null) {
      throw new ContractValidateException("Contract not exists");
    }
    byte[] deployedContractOwnerAddress = deployedContract.getInstance().getOwnerAddress().toByteArray();
    if (!Arrays.equals(ownerAddress, deployedContractOwnerAddress)) {
      throw new ContractValidateException("Account[" + readableOwnerAddress + "] is not the owner of the contract");
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(ClearABIContract.class).getOwnerAddress();
  }
}
