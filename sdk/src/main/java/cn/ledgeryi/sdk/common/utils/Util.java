package cn.ledgeryi.sdk.common.utils;

import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.ClearABIContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.CreateSmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import cn.ledgeryi.sdk.common.TransactionFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.util.List;

@Slf4j(topic = "API")
public class Util {

  private static final String PERMISSION_ID = "Permission_id";
  private static final String VISIBLE = "visible";
  private static final String VALUE = "value";
  private static final String CONTRACT_TYPE = "contractType";
  private static final String EXTRA_DATA = "extra_data";

  public static String printErrorMsg(Exception e) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("Error", e.getClass() + " : " + e.getMessage());
    return jsonObject.toJSONString();
  }

  private static JSONArray printTransactionListToJSON(List<Transaction> list, boolean selfType) {
    JSONArray transactions = new JSONArray();
    list.stream().forEach(transaction ->
      transactions.add(printTransactionToJSON(transaction, selfType))
    );
    return transactions;
  }

  public static String printTransaction(Transaction transaction, boolean selfType) {
    return printTransactionToJSON(transaction, selfType).toJSONString();
  }

  public static String printCreateTransaction(Transaction transaction, boolean selfType) {
    JSONObject jsonObject = printTransactionToJSON(transaction, selfType);
    jsonObject.put(VISIBLE, selfType);
    return jsonObject.toJSONString();
  }

  private static JSONObject printTransactionToJSON(Transaction transaction, boolean selfType) {
    JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction, selfType));
    JSONArray contracts = new JSONArray();
    Transaction.Contract contract = transaction.getRawData().getContract();
    try {
      JSONObject contractJson = null;
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case CreateSmartContract:
          CreateSmartContract createSmartContract = contractParameter.unpack(CreateSmartContract.class);
          contractJson = JSONObject.parseObject(JsonFormat.printToString(createSmartContract, selfType));
          break;
        case TriggerSmartContract:
          TriggerSmartContract triggerSmartContract = contractParameter.unpack(TriggerSmartContract.class);
          contractJson = JSONObject.parseObject(JsonFormat.printToString(triggerSmartContract, selfType));
          break;
        case ClearABIContract:
          ClearABIContract clearABIContract = contractParameter.unpack(ClearABIContract.class);
          contractJson = JSONObject.parseObject(JsonFormat.printToString(clearABIContract, selfType));
          break;
        // todo add other contract
        default:
          break;
      }
      JSONObject parameter = new JSONObject();
      parameter.put(VALUE, contractJson);
      parameter.put("type_url", contract.getParameterOrBuilder().getTypeUrl());
      JSONObject jsonContract = new JSONObject();
      jsonContract.put("parameter", parameter);
      jsonContract.put("type", contract.getType());
      if (contract.getPermissionId() > 0) {
        jsonContract.put(PERMISSION_ID, contract.getPermissionId());
      }
      contracts.add(jsonContract);
    } catch (InvalidProtocolBufferException e) {
      log.debug("InvalidProtocolBufferException: {}", e.getMessage());
    }

    JSONObject rawData = JSONObject.parseObject(jsonTransaction.get("raw_data").toString());
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    String rawDataHex = ByteArray.toHexString(transaction.getRawData().toByteArray());
    jsonTransaction.put("raw_data_hex", rawDataHex);
    String txID = ByteArray.toHexString(Sha256Hash.hash(true, transaction.getRawData().toByteArray()));
    jsonTransaction.put("txID", txID);
    return jsonTransaction;
  }

  public static Transaction packTransaction(String strTransaction, boolean selfType) {
    JSONObject jsonTransaction = JSONObject.parseObject(strTransaction);
    JSONObject rawData = jsonTransaction.getJSONObject("raw_data");
    JSONArray contracts = new JSONArray();
    JSONArray rawContractArray = rawData.getJSONArray("contract");

    for (int i = 0; i < rawContractArray.size(); i++) {
      try {
        JSONObject contract = rawContractArray.getJSONObject(i);
        JSONObject parameter = contract.getJSONObject("parameter");
        String contractType = contract.getString("type");
        Any any = null;
        Class clazz = TransactionFactory.getContract(ContractType.valueOf(contractType));
        if (clazz != null) {
          Constructor<GeneratedMessageV3> constructor = clazz.getDeclaredConstructor();
          constructor.setAccessible(true);
          GeneratedMessageV3 generatedMessageV3 = constructor.newInstance();
          Message.Builder builder = generatedMessageV3.toBuilder();
          JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(), builder, selfType);
          any = Any.pack(builder.build());
        }
        if (any != null) {
          String value = ByteArray.toHexString(any.getValue().toByteArray());
          parameter.put(VALUE, value);
          contract.put("parameter", parameter);
          contracts.add(contract);
        }
      } catch (JsonFormat.ParseException e) {
        log.debug("ParseException: {}", e.getMessage());
      } catch (Exception e) {
        log.error("", e);
      }
    }
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    Transaction.Builder transactionBuilder = Transaction.newBuilder();
    try {
      JsonFormat.merge(jsonTransaction.toJSONString(), transactionBuilder, selfType);
      return transactionBuilder.build();
    } catch (JsonFormat.ParseException e) {
      log.debug("ParseException: {}", e.getMessage());
      return null;
    }
  }


}
