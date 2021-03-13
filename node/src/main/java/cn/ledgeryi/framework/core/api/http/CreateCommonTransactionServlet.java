package cn.ledgeryi.framework.core.api.http;

import cn.ledgeryi.chainbase.actuator.TransactionFactory;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.core.LedgerYi;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j(topic = "API")
public class CreateCommonTransactionServlet extends RateLimiterServlet {

  @Autowired
  private LedgerYi wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(contract);
      boolean visible = Util.getVisiblePost(contract);
      ContractType type = ContractType.valueOf(Util.getContractType(contract));
      Message.Builder build = getBuilder(type);
      JsonFormat.merge(contract, build, visible);
      Transaction tx = wallet.createTransactionCapsule(build.build(), type).getInstance();
      JSONObject jsonObject = JSONObject.parseObject(contract);
      tx = Util.setTransactionPermissionId(jsonObject, tx);
      response.getWriter().println(Util.printCreateTransaction(tx, visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private Message.Builder getBuilder(ContractType type) throws NoSuchMethodException,
      IllegalAccessException, InvocationTargetException, InstantiationException, ContractValidateException {
    Class clazz = TransactionFactory.getContract(type);
    if (clazz != null) {
      Constructor<GeneratedMessageV3> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      GeneratedMessageV3 generatedMessageV3 = constructor.newInstance();
      return generatedMessageV3.toBuilder();
    } else {
      throw new ContractValidateException("don't have this type: " + type);
    }
  }
}
