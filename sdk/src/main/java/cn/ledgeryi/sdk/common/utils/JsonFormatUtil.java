package cn.ledgeryi.sdk.common.utils;

import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.crypto.Sha256Sm3Hash;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

public class JsonFormatUtil {

  public static final String PERMISSION_ID = "Permission_id";
  public static final String VALUE = "value";

  public static String printSmartContract(SmartContractOuterClass.SmartContract smartContract){
    String smartStr = JsonFormat.printToString(smartContract, true);
    JSONObject smartJsonObject = JSONObject.parseObject(smartStr);
    JsonFormatUtil.formatJson(smartJsonObject.toJSONString());
    return JsonFormatUtil.formatJson(smartJsonObject.toJSONString());
  }

  public static String printTransactionExceptId(Protocol.Transaction transaction) {
    JSONObject jsonObject = printTransactionToJSON(transaction, true);
    jsonObject.remove("txID");
    return JsonFormatUtil.formatJson(jsonObject.toJSONString());
  }

  public static JSONObject printTransactionToJSON(Protocol.Transaction transaction, boolean selfType) {
    JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction, selfType));
    JSONArray contracts = new JSONArray();
    JSONObject contractJson = null;
    Protocol.Transaction.Contract contract = transaction.getRawData().getContract();
    Any contractParameter = contract.getParameter();
    try {
      switch (contract.getType()) {
        case CreateSmartContract:
          SmartContractOuterClass.CreateSmartContract deployContract =
                  contractParameter.unpack(SmartContractOuterClass.CreateSmartContract.class);
          contractJson = JSONObject.parseObject(JsonFormat.printToString(deployContract, selfType));
          byte[] ownerAddress = deployContract.getOwnerAddress().toByteArray();
          byte[] contractAddress = TransactionUtils.generateContractAddress(transaction, ownerAddress);
          jsonTransaction.put("contract_address", ByteArray.toHexString(contractAddress));
          break;
        case TriggerSmartContract:
          SmartContractOuterClass.TriggerSmartContract triggerSmartContract =
                  contractParameter.unpack(SmartContractOuterClass.TriggerSmartContract.class);
          contractJson = JSONObject.parseObject(JsonFormat.printToString(triggerSmartContract, selfType));
          break;

        case ClearABIContract:
          SmartContractOuterClass.ClearABIContract clearABIContract =
                  contractParameter.unpack(SmartContractOuterClass.ClearABIContract.class);
          contractJson = JSONObject.parseObject(JsonFormat.printToString(clearABIContract, selfType));
          break;
        // todo add other contract
        default:
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
      e.printStackTrace();
    }

    JSONObject rawData = JSONObject.parseObject(jsonTransaction.get("raw_data").toString());
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    String rawDataHex = ByteArray.toHexString(transaction.getRawData().toByteArray());
    jsonTransaction.put("raw_data_hex", rawDataHex);
    String txID = ByteArray.toHexString(Sha256Sm3Hash.hash(transaction.getRawData().toByteArray()));
    jsonTransaction.put("txID", txID);
    return jsonTransaction;
  }
  /**
   * format json string to show type
   */
  public static String formatJson(String jsonStr) {
    if (null == jsonStr || "".equals(jsonStr)) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    char last = '\0';
    char current = '\0';
    int indent = 0;
    for (int i = 0; i < jsonStr.length(); i++) {
      last = current;
      current = jsonStr.charAt(i);
      switch (current) {
        case '{':
        case '[':
          sb.append(current);
          sb.append('\n');
          indent++;
          addIndentBlank(sb, indent);
          break;
        case '}':
        case ']':
          sb.append('\n');
          indent--;
          addIndentBlank(sb, indent);
          sb.append(current);
          break;
        case ',':
          sb.append(current);
          if (last != '\\') {
            sb.append('\n');
            addIndentBlank(sb, indent);
          }
          break;
        default:
          sb.append(current);
      }
    }

    return sb.toString();
  }

  /**
   * add space
   */
  private static void addIndentBlank(StringBuilder sb, int indent) {
    for (int i = 0; i < indent; i++) {
      sb.append('\t');
    }
  }
}
