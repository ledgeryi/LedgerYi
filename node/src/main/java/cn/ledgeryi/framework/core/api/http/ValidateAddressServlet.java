package cn.ledgeryi.framework.core.api.http;

import cn.ledgeryi.common.utils.DecodeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.util.Base64;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import cn.ledgeryi.common.utils.ByteArray;


@Component
@Slf4j(topic = "API")
public class ValidateAddressServlet extends RateLimiterServlet {

  private String validAddress(String input) {
    byte[] address = null;
    boolean result = true;
    String msg;
    try {
      if (input.length() == DecodeUtil.ADDRESS_SIZE) {
        //hex
        address = ByteArray.fromHexString(input);
        msg = "Hex string format";
      } else if (input.length() == 28) {
        //base64
        address = Base64.getDecoder().decode(input);
        msg = "Base64 format";
      } else {
        result = false;
        msg = "Length error";
      }
      if (result) {
        result = DecodeUtil.addressValid(address);
        if (!result) {
          msg = "Invalid address";
        }
      }
    } catch (Exception e) {
      result = false;
      msg = e.getMessage();
    }

    JSONObject jsonAddress = new JSONObject();
    jsonAddress.put("result", result);
    jsonAddress.put("message", msg);

    return jsonAddress.toJSONString();
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    String input = request.getParameter("address");
    try {
      response.getWriter().println(validAddress(input));
    } catch (IOException e) {
      log.debug("IOException: {}", e.getMessage());
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      JSONObject jsonAddress = JSON.parseObject(input);
      response.getWriter().println(validAddress(jsonAddress.getString("address")));
    } catch (Exception e) {
      log.debug("Exception: {}", e.getMessage());
    }
  }
}