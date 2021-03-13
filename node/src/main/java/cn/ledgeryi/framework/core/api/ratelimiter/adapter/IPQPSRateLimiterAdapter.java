package cn.ledgeryi.framework.core.api.ratelimiter.adapter;


import cn.ledgeryi.framework.core.api.ratelimiter.RuntimeData;
import cn.ledgeryi.framework.core.api.ratelimiter.strategy.IPQpsStrategy;

public class IPQPSRateLimiterAdapter implements IRateLimiter {

  private IPQpsStrategy strategy;

  public IPQPSRateLimiterAdapter(String paramString) {
    strategy = new IPQpsStrategy(paramString);
  }

  @Override
  public boolean acquire(RuntimeData data) {
    return strategy.acquire(data.getRemoteAddr());
  }

}