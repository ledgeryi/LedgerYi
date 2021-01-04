package cn.ledgeryi.framework.core.actuator;

import cn.ledgeryi.chainbase.actuator.Actuator;
import cn.ledgeryi.contract.core.AbstractActuator;
import cn.ledgeryi.chainbase.actuator.TransactionFactory;
import cn.ledgeryi.chainbase.common.utils.ForkUtils;
import cn.ledgeryi.chainbase.core.ChainBaseManager;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.chainbase.core.store.DynamicPropertiesStore;
import cn.ledgeryi.chainbase.core.store.StoreFactory;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.protos.Protocol.Transaction.Contract;
import cn.ledgeryi.protos.Protocol.Transaction;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "contract")
public class ActuatorCreator {

  private ForkUtils forkUtils = new ForkUtils();

  private DynamicPropertiesStore dynamicPropertiesStore;

  private ChainBaseManager chainBaseManager;

  private ActuatorCreator(StoreFactory storeFactory) {
    chainBaseManager = storeFactory.getChainBaseManager();
    dynamicPropertiesStore = storeFactory.getChainBaseManager().getDynamicPropertiesStore();
    forkUtils.setDynamicPropertiesStore(dynamicPropertiesStore);
  }

  public static ActuatorCreator getINSTANCE() {
    if (ActuatorCreatorInner.INSTANCE == null) {
      ActuatorCreatorInner.INSTANCE = new ActuatorCreator(StoreFactory.getInstance());
    }
    return ActuatorCreatorInner.INSTANCE;
  }

  public static void init() {
    ActuatorCreatorInner.INSTANCE = new ActuatorCreator(StoreFactory.getInstance());
  }

  /**
   * create contract.
   */
  public List<Actuator> createActuator(TransactionCapsule transactionCapsule) throws ContractValidateException {
    List<Actuator> actuatorList = Lists.newArrayList();
    if (null == transactionCapsule || null == transactionCapsule.getInstance()) {
      log.info("transactionCapsule or Transaction is null");
      return actuatorList;
    }
    Transaction.raw rawData = transactionCapsule.getInstance().getRawData();
    try {
      actuatorList.add(getActuatorByContract(rawData.getContract(), transactionCapsule));
    } catch (Exception e) {
      log.error("", e);
      throw new ContractValidateException(e.getMessage());
    }
    return actuatorList;
  }

  private Actuator getActuatorByContract(Contract contract, TransactionCapsule tx)
      throws IllegalAccessException, InstantiationException, ContractValidateException {
    Class<? extends Actuator> clazz = TransactionFactory.getActuator(contract.getType());
    if (clazz == null) {
      throw new ContractValidateException("not exist contract " + contract);
    }
    AbstractActuator abstractActuator = (AbstractActuator) clazz.newInstance();
    abstractActuator.setChainBaseManager(chainBaseManager).setContract(contract).setForkUtils(forkUtils).setTx(tx);
    return abstractActuator;
  }

  private static class ActuatorCreatorInner {
    private static ActuatorCreator INSTANCE;
  }
}
