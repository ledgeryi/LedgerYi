package cn.ledgeryi.framework.core.api.http;

import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.api.GrpcAPI.NumberMessage;
import cn.ledgeryi.framework.core.LedgerYi;


@Component
@Slf4j(topic = "API")
public class GetTransactionCountByBlockNumServlet extends RateLimiterServlet {

  @Autowired
  private LedgerYi wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      long num = Long.parseLong(request.getParameter("num"));
      long count = wallet.getTransactionCountByBlockNum(num);
      response.getWriter().println("{\"count\": " + count + "}");
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);
      NumberMessage.Builder build = NumberMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      long count = wallet.getTransactionCountByBlockNum(build.getNum());
      response.getWriter().println("{\"count\": " + count + "}");
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}