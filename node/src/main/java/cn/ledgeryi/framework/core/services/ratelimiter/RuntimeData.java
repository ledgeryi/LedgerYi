package cn.ledgeryi.framework.core.services.ratelimiter;


import io.grpc.Grpc;
import io.grpc.ServerCall;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeData {


  private String address = "";

  public RuntimeData(Object o) {
    if (o instanceof HttpServletRequest) {
      address = ((HttpServletRequest) o).getRemoteAddr();
    } else if (o instanceof ServerCall) {
      try {
        address = ((ServerCall) o).getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR).toString();
      } catch (Exception npe) {
        log.warn("the address get from the runtime data is a null value unexpected.");
      }
    }

    if (address == null) {
      log.warn("assign the address with an empty string.");
      address = "";
    }
  }

  public String getRemoteAddr() {
    return address;
  }

}