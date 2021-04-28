package cn.ledgeryi.framework.core.api.http;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.framework.common.entity.Log;
import cn.ledgeryi.framework.common.entity.TransactionInformation;
import cn.ledgeryi.framework.core.LedgerYi;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "API")
public class GetTransactionInfoByIdServlet extends RateLimiterServlet {

    @Autowired
    private LedgerYi wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            TransactionInformation information = queryTransactionInformation(request.getParameter("value"));

            if (information != null) {
                response.getWriter().println(JSONObject.toJSONString(information));
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
            TransactionInformation transactionInformation = queryTransactionInformation(ByteUtil.toHexString(build.getValue().toByteArray()));
            if (transactionInformation != null) {
                response.getWriter().println(JSONObject.toJSONString(transactionInformation));
            } else {
                response.getWriter().println("{}");
            }
        } catch (Exception e) {
            Util.processError(e, response);
        }
    }

    private TransactionInformation queryTransactionInformation(String txHash) throws InvalidProtocolBufferException {
        if (Objects.isNull(txHash)) {
            return null;
        }

        Protocol.TransactionInfo transactionInfo = wallet.getTransactionInfoById(ByteString.copyFrom(ByteArray.fromHexString(txHash)));
        if (Objects.isNull(transactionInfo)) {
            return null;
        }

        SmartContractOuterClass.SmartContract contract = wallet.getContract(transactionInfo.getContractAddress().toByteArray());
        TransactionInformation information = TransactionInformation.parseTransactionInfo(transactionInfo);

        information.setLogs(Log.parseLogInfo(transactionInfo.getLogList(), contract.getAbi()));

        return information;
    }
}

