package cn.ledgeryi.framework.core.services.http;

import com.google.protobuf.ByteString;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.api.GrpcAPI.BytesMessage;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.framework.core.LedgerYi;
import cn.ledgeryi.protos.Protocol.Transaction;


@Component
@Slf4j(topic = "API")
public class GetTransactionByIdServlet extends RateLimiterServlet {

  @Autowired
  private LedgerYi wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter("value");
      Transaction reply = wallet.getTransactionById(ByteString.copyFrom(ByteArray.fromHexString(input)));
      if (reply != null) {
        response.getWriter().println(Util.printTransaction(reply, visible));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      Transaction reply = wallet.getTransactionById(build.getValue());
      if (reply != null) {
        response.getWriter().println(Util.printTransaction(reply, visible));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}