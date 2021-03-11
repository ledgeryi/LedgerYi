package cn.ledgeryi.framework.core.services.http;

import cn.ledgeryi.framework.core.LedgerYi;
import cn.ledgeryi.protos.Protocol.Account;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;


@Component
@Slf4j(topic = "API")
public class GetAccountServlet extends RateLimiterServlet {

  @Autowired
  private LedgerYi wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String address = request.getParameter("address");
      Account.Builder build = Account.newBuilder();
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("address", address);
      JsonFormat.merge(jsonObject.toJSONString(), build, visible);

      Account reply = wallet.getAccount(build.build());
      if (reply != null) {
        if (visible) {
          response.getWriter().println(JsonFormat.printToString(reply, true));
        }
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String account = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(account);
      boolean visible = Util.getVisiblePost(account);
      Account.Builder build = Account.newBuilder();
      JsonFormat.merge(account, build, visible);

      Account reply = wallet.getAccount(build.build());
      if (reply != null) {
        if (visible) {
          response.getWriter().println(JsonFormat.printToString(reply, true));
        }
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
