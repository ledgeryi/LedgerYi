package cn.ledgeryi.framework.core.api.ratelimiter;


import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.core.api.ratelimiter.adapter.IRateLimiter;

@Component
public class RateLimiterContainer {

  @Getter
  private Map<String, IRateLimiter> map = new HashMap<>();

  public void add(String prefix, String method, IRateLimiter rateLimiter) {
    map.put(prefix + method, rateLimiter);
  }

  public IRateLimiter get(String prefix, String method) {
    return map.get(prefix + method);
  }

}