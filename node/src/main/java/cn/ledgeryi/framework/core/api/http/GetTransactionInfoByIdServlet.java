package cn.ledgeryi.framework.core.api.http;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.framework.core.LedgerYi;
import cn.ledgeryi.protos.Protocol;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "API")
public class GetTransactionInfoByIdServlet extends RateLimiterServlet {

    @Autowired
    private LedgerYi wallet;

    private static String convertLogAddressToTronAddress(Protocol.TransactionInfo transactionInfo) {
        return JsonFormat.printToString(transactionInfo);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            boolean visible = Util.getVisible(request);
            String input = request.getParameter("value");
            Protocol.TransactionInfo reply = wallet.getTransactionInfoById(ByteString.copyFrom(ByteArray.fromHexString(input)));
            if (reply != null) {
                response.getWriter().println(convertLogAddressToTronAddress(reply));
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
            GrpcAPI.BytesMessage.Builder build = GrpcAPI.BytesMessage.newBuilder();
            JsonFormat.merge(input, build, visible);
            Protocol.TransactionInfo reply = wallet.getTransactionInfoById(build.getValue());
            if (reply != null) {
                response.getWriter().println(convertLogAddressToTronAddress(reply));
            } else {
                response.getWriter().println("{}");
            }
        } catch (Exception e) {
            Util.processError(e, response);
        }
    }
}

