package cn.ledgeryi.framework.core.api.ratelimiter.adapter;

import cn.ledgeryi.framework.core.api.ratelimiter.RuntimeData;

public interface IRateLimiter {

  boolean acquire(RuntimeData data);

}
