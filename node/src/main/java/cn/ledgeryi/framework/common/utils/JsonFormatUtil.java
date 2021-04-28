package cn.ledgeryi.framework.common.utils;

import cn.ledgeryi.framework.core.api.http.JsonFormat;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonFormatUtil {

  public static String printABI(SmartContractOuterClass.SmartContract.ABI abi){
    return printMessage(abi);
  }

  private static String printMessage(Message message){
    String smartStr = JsonFormat.printToString(message, true);
    JSONObject smartJsonObject = JSONObject.parseObject(smartStr);
    return JsonFormatUtil.formatJson(smartJsonObject.toJSONString());
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
