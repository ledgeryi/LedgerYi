package cn.ledgeryi.sdk.common;

import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import com.google.protobuf.GeneratedMessageV3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionFactory {

  private static Map<ContractType, Class<? extends GeneratedMessageV3>> contractMap = new ConcurrentHashMap<>();

  static {
    register(ContractType.CreateSmartContract,SmartContractOuterClass.CreateSmartContract.class);
    register(ContractType.TriggerSmartContract,SmartContractOuterClass.TriggerSmartContract.class);
  }

  public static void register(ContractType type, Class<? extends GeneratedMessageV3> clazz) {
    if (type != null && clazz != null) {
      contractMap.put(type, clazz);
    }
  }

  public static Class<? extends GeneratedMessageV3> getContract(ContractType type) {
    return contractMap.get(type);
  }
}
