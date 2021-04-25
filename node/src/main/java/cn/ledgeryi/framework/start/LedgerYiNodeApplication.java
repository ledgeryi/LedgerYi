package cn.ledgeryi.framework.start;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import cn.ledgeryi.common.core.Constant;
import cn.ledgeryi.framework.common.application.Application;
import cn.ledgeryi.framework.common.application.ApplicationFactory;
import cn.ledgeryi.framework.common.application.LedgerYiApplicationContext;
import cn.ledgeryi.framework.core.config.DefaultConfig;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.permission.PermissionService;
import cn.ledgeryi.framework.core.api.service.RpcApiService;
import cn.ledgeryi.framework.core.api.service.LedgerYiNodeHttpApiService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.io.File;

@Slf4j(topic = "app")
public class LedgerYiNodeApplication {

  private static void load(String path) {
    try {
      File file = new File(path);
      if (!file.exists() || !file.isFile() || !file.canRead()) {
        return;
      }
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(lc);
      lc.reset();
      configurator.doConfigure(file);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  /**
   * Start the LedgerYiNodeApplication.
   */
  public static void main(String[] args) {
    log.info("LedgerYi node running.");
    Args.setParam(args, Constant.MAINNET_CONF);
    Args cfgArgs = Args.getInstance();

    load(cfgArgs.getLogbackPath());

    if (cfgArgs.isHelp()) {
      log.info("Here is the help message.");
      return;
    }

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    LedgerYiApplicationContext context = new LedgerYiApplicationContext(beanFactory);
    context.register(DefaultConfig.class);

    context.refresh();
    Application appT = ApplicationFactory.create(context);
    shutdown(appT);

    // grpc api server
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);

    // http api server
    if (Args.getInstance().ledgerYiNodeHttpEnable) {
      LedgerYiNodeHttpApiService httpApiService = context.getBean(LedgerYiNodeHttpApiService.class);
      appT.addService(httpApiService);
    }

    //permissioned blockchain
    if (Args.getInstance().isPermissionNet()){
      PermissionService permissionService = context.getBean(PermissionService.class);
      appT.addService(permissionService);
    }

    //appT.initServices(cfgArgs);
    try {
      appT.startServices();
    } catch (Exception e) {
      log.error("Unable to Start Service ",e);
      appT.shutdown();
      appT.shutdownServices();
      System.exit(0);
    }
    appT.startup();

    rpcApiService.blockUntilShutdown();
  }

  public static void shutdown(final Application app) {
    log.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }
}
