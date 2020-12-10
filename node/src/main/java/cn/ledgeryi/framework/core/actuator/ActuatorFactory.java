package cn.ledgeryi.framework.core.actuator;

import cn.ledgeryi.actuator.core.AbstractActuator;
import cn.ledgeryi.chainbase.actuator.Actuator;
import cn.ledgeryi.chainbase.actuator.TransactionFactory;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.protos.Protocol;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import cn.ledgeryi.framework.core.db.Manager;

@Slf4j(topic = "actuator")
public class ActuatorFactory {

  public static final ActuatorFactory INSTANCE = new ActuatorFactory();

  private ActuatorFactory() {
  }

  public static ActuatorFactory getInstance() {
    return INSTANCE;
  }

  /**
   * create actuator.
   */
  public static List<Actuator> createActuator(TransactionCapsule transactionCapsule,
                                              Manager manager) {
    List<Actuator> actuatorList = Lists.newArrayList();
    if (null == transactionCapsule || null == transactionCapsule.getInstance()) {
      log.info("transactionCapsule or Transaction is null");
      return actuatorList;
    }

    Preconditions.checkNotNull(manager, "manager is null");
    Protocol.Transaction.raw rawData = transactionCapsule.getInstance().getRawData();
    Protocol.Transaction.Contract contract = rawData.getContract();
    try {
      actuatorList.add(getActuatorByContract(contract, manager, transactionCapsule));
    } catch (IllegalAccessException | InstantiationException e) {
      e.printStackTrace();
    }
    return actuatorList;
  }

  private static Actuator getActuatorByContract(Protocol.Transaction.Contract contract, Manager manager,
                                                TransactionCapsule tx) throws IllegalAccessException, InstantiationException {
    Class<? extends Actuator> clazz = TransactionFactory.getActuator(contract.getType());
    AbstractActuator abstractActuator = (AbstractActuator) clazz.newInstance();
    abstractActuator.setChainBaseManager(manager.getChainBaseManager()).setContract(contract)
        .setForkUtils(manager.getForkController()).setTx(tx);
    return abstractActuator;
  }

}
