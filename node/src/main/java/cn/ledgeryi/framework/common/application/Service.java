package cn.ledgeryi.framework.common.application;

import cn.ledgeryi.framework.core.config.args.Args;

public interface Service {

  void init();

  void init(Args args);

  void start();

  void stop();
}
