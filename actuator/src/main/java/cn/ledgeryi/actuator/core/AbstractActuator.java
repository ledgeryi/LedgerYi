package cn.ledgeryi.actuator.core;

import cn.ledgeryi.chainbase.actuator.Actuator;
import cn.ledgeryi.chainbase.actuator.TransactionFactory;
import cn.ledgeryi.chainbase.common.utils.ForkUtils;
import cn.ledgeryi.chainbase.core.ChainBaseManager;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import cn.ledgeryi.protos.Protocol.Transaction.Contract;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;


public abstract class AbstractActuator implements Actuator {

  protected Any any;
  protected ChainBaseManager chainBaseManager;
  protected Contract contract;
  protected TransactionCapsule tx;
  protected ForkUtils forkUtils;

  public AbstractActuator(ContractType type, Class<? extends GeneratedMessageV3> clazz) {
    TransactionFactory.register(type, getClass(), clazz);
  }

  public Any getAny() {
    return any;
  }

  public AbstractActuator setAny(Any any) {
    this.any = any;
    return this;
  }

  public ChainBaseManager getChainBaseManager() {
    return chainBaseManager;
  }

  public AbstractActuator setChainBaseManager(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
    return this;
  }

  public Contract getContract() {
    return contract;
  }

  public AbstractActuator setContract(Contract contract) {
    this.contract = contract;
    this.any = contract.getParameter();
    return this;
  }

  public TransactionCapsule getTx() {
    return tx;
  }

  public AbstractActuator setTx(TransactionCapsule tx) {
    this.tx = tx;
    return this;
  }

  public ForkUtils getForkUtils() {
    return forkUtils;
  }

  public AbstractActuator setForkUtils(ForkUtils forkUtils) {
    this.forkUtils = forkUtils;
    return this;
  }
}
