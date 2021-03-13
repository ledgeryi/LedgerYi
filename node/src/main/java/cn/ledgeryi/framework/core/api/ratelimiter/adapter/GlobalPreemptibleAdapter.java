package cn.ledgeryi.framework.core.api.ratelimiter.adapter;


import cn.ledgeryi.framework.core.api.ratelimiter.RuntimeData;
import cn.ledgeryi.framework.core.api.ratelimiter.strategy.GlobalPreemptibleStrategy;

public class GlobalPreemptibleAdapter implements IPreemptibleRateLimiter {

  private GlobalPreemptibleStrategy strategy;

  public GlobalPreemptibleAdapter(String paramString) {

    strategy = new GlobalPreemptibleStrategy(paramString);
  }

  @Override
  public void release() {
    strategy.release();
  }

  @Override
  public boolean acquire(RuntimeData data) {
    return strategy.acquire();
  }

}