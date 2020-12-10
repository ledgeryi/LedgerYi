package cn.ledgeryi.framework.core.services.ratelimiter.adapter;

import cn.ledgeryi.framework.core.services.ratelimiter.RuntimeData;

public interface IRateLimiter {

  boolean acquire(RuntimeData data);

}
