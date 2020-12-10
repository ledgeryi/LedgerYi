package cn.ledgeryi.framework.common.application;

import org.springframework.context.ApplicationContext;

public class ApplicationFactory {

  /**
   * Build a new cli application.
   */
  public static Application create(ApplicationContext ctx) {
    return ctx.getBean(ApplicationImpl.class);
  }

  /**
   * Build a new application.
   */
  public Application createApplication() {
    return new ApplicationImpl();
  }
}
