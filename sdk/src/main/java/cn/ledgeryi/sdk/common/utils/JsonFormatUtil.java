package cn.ledgeryi.sdk.common.utils;

import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.crypto.Sha256Sm3Hash;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonFormatUtil {

  public static final String PERMISSION_ID = "Permission_id";
  public static final String VALUE = "value";

  public static String printSmartContract(SmartContractOuterClass.SmartContract smartContract){
    return printMessage(smartContract);
  }

  public static String printABI(SmartContractOuterClass.SmartContract.ABI abi){
    return printMessage(abi);
  }


  private static String printMessage(Message message){
    String smartStr = JsonFormat.printToString(message, true);
    JSONObject smartJsonObject = JSONObject.parseObject(smartStr);
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

  public static SmartContractOuterClass.SmartContract.ABI jsonStr2ABI(String jsonStr) {
    if (jsonStr == null) {
      return null;
    }
    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
    JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
    SmartContractOuterClass.SmartContract.ABI.Builder abiBuilder = SmartContractOuterClass.SmartContract.ABI.newBuilder();
    for (int index = 0; index < jsonRoot.size(); index++) {
      JsonElement abiItem = jsonRoot.get(index);
      boolean anonymous = abiItem.getAsJsonObject().get("anonymous") != null
              ? abiItem.getAsJsonObject().get("anonymous")
              .getAsBoolean()
              : false;
      boolean constant = abiItem.getAsJsonObject().get("constant") != null
              ? abiItem.getAsJsonObject().get("constant")
              .getAsBoolean()
              : false;
      String name = abiItem.getAsJsonObject().get("name") != null
              ? abiItem.getAsJsonObject().get("name").getAsString()
              : null;
      JsonArray inputs = abiItem.getAsJsonObject().get("inputs") != null
              ? abiItem.getAsJsonObject().get("inputs")
              .getAsJsonArray()
              : null;
      JsonArray outputs = abiItem.getAsJsonObject().get("outputs") != null
              ? abiItem.getAsJsonObject().get("outputs")
              .getAsJsonArray()
              : null;
      String type = abiItem.getAsJsonObject().get("type") != null
              ? abiItem.getAsJsonObject().get("type").getAsString()
              : null;
      boolean payable = abiItem.getAsJsonObject().get("payable") != null
              ? abiItem.getAsJsonObject().get("payable")
              .getAsBoolean()
              : false;
      String stateMutability = abiItem.getAsJsonObject().get("stateMutability") != null
              ? abiItem.getAsJsonObject().get("stateMutability")
              .getAsString()
              : null;
      if (type == null) {
        log.error("No type!");
        return null;
      }
      if (!type.equalsIgnoreCase("fallback") && null == inputs) {
        log.error("No inputs!");
        return null;
      }
      SmartContractOuterClass.SmartContract.ABI.Entry.Builder entryBuilder = SmartContractOuterClass.SmartContract.ABI.Entry.newBuilder();
      entryBuilder.setAnonymous(anonymous);
      entryBuilder.setConstant(constant);
      if (name != null) {
        entryBuilder.setName(name);
      }
      /* { inputs : optional } since fallback function not requires inputs*/
      if (null != inputs) {
        for (int j = 0; j < inputs.size(); j++) {
          JsonElement inputItem = inputs.get(j);
          if (inputItem.getAsJsonObject().get("name") == null || inputItem.getAsJsonObject().get("type") == null) {
            log.error("Input argument invalid due to no name or no type!");
            return null;
          }
          String inputName = inputItem.getAsJsonObject().get("name").getAsString();
          String inputType = inputItem.getAsJsonObject().get("type").getAsString();
          Boolean inputIndexed = false;
          if (inputItem.getAsJsonObject().get("indexed") != null) {
            inputIndexed = Boolean.valueOf(inputItem.getAsJsonObject().get("indexed").getAsString());
          }
          SmartContractOuterClass.SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContractOuterClass.SmartContract.ABI.Entry.Param.newBuilder();
          paramBuilder.setIndexed(inputIndexed);
          paramBuilder.setName(inputName);
          paramBuilder.setType(inputType);
          entryBuilder.addInputs(paramBuilder.build());
        }
      }

      /* { outputs : optional } */
      if (outputs != null) {
        for (int k = 0; k < outputs.size(); k++) {
          JsonElement outputItem = outputs.get(k);
          if (outputItem.getAsJsonObject().get("name") == null || outputItem.getAsJsonObject().get("type") == null) {
            log.error("Output argument invalid due to no name or no type!");
            return null;
          }
          String outputName = outputItem.getAsJsonObject().get("name").getAsString();
          String outputType = outputItem.getAsJsonObject().get("type").getAsString();
          Boolean outputIndexed = false;
          if (outputItem.getAsJsonObject().get("indexed") != null) {
            outputIndexed = Boolean.valueOf(outputItem.getAsJsonObject().get("indexed").getAsString());
          }
          SmartContractOuterClass.SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContractOuterClass.SmartContract.ABI.Entry.Param.newBuilder();
          paramBuilder.setIndexed(outputIndexed);
          paramBuilder.setName(outputName);
          paramBuilder.setType(outputType);
          entryBuilder.addOutputs(paramBuilder.build());
        }
      }
      entryBuilder.setType(getEntryType(type));
      entryBuilder.setPayable(payable);
      if (stateMutability != null) {
        entryBuilder.setStateMutability(getStateMutability(stateMutability));
      }
      abiBuilder.addEntrys(entryBuilder.build());
    }
    return abiBuilder.build();
  }

  private static SmartContractOuterClass.SmartContract.ABI.Entry.EntryType getEntryType(String type) {
    switch (type) {
      case "constructor":
        return SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.Constructor;
      case "function":
        return SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.Function;
      case "event":
        return SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.Event;
      case "fallback":
        return SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.Fallback;
      default:
        return SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
    }
  }
  private static SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
          String stateMutability) {
    switch (stateMutability) {
      case "pure":
        return SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType.Pure;
      case "view":
        return SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType.View;
      case "nonpayable":
        return SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
      case "payable":
        return SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType.Payable;
      default:
        return SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
    }
  }
}
