package cn.ledgeryi.framework.core.api.ratelimiter.adapter;


import cn.ledgeryi.framework.core.api.ratelimiter.RuntimeData;
import cn.ledgeryi.framework.core.api.ratelimiter.strategy.QpsStrategy;

public class QpsRateLimiterAdapter implements IRateLimiter {

  private QpsStrategy strategy;

  public QpsRateLimiterAdapter(String paramString) {
    strategy = new QpsStrategy(paramString);
  }

  @Override
  public boolean acquire(RuntimeData data) {
    return strategy.acquire();
  }

}