package cn.ledgeryi.framework.core.services.ratelimiter.adapter;

public interface IPreemptibleRateLimiter extends IRateLimiter {

  void release();
}
