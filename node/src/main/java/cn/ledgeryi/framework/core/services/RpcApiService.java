package cn.ledgeryi.framework.core.services;

import cn.ledgeryi.framework.common.application.Service;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.services.ratelimiter.RateLimiterInterceptor;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j(topic = "API")
public class RpcApiService implements Service {

  private Server apiServer;

  private int port = Args.getInstance().getRpcPort();

  @Autowired
  private RateLimiterInterceptor rateLimiterInterceptor;

  @Autowired
  private WalletApi walletApi;

  @Autowired
  private PermissionApi permissionApi;

  @Override
  public void init() {
  }

  @Override
  public void start() {
    try {
      Args args = Args.getInstance();
      NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(port).addService(walletApi);
      //permission net
      if (args.isPermissionNet()){
        serverBuilder.addService(permissionApi);
      }
      if (args.getRpcThreadNum() > 0) {
        serverBuilder = serverBuilder.executor(Executors.newFixedThreadPool(args.getRpcThreadNum()));
      }
      serverBuilder = serverBuilder.addService(walletApi);
      // Set configs from config.conf or default value
      serverBuilder
          .maxConcurrentCallsPerConnection(args.getMaxConcurrentCallsPerConnection())
          .flowControlWindow(args.getFlowControlWindow())
          .maxConnectionIdle(args.getMaxConnectionIdleInMillis(), TimeUnit.MILLISECONDS)
          .maxConnectionAge(args.getMaxConnectionAgeInMillis(), TimeUnit.MILLISECONDS)
          .maxMessageSize(args.getMaxMessageSize())
          .maxHeaderListSize(args.getMaxHeaderListSize());

      // add a ratelimiter interceptor
      serverBuilder.intercept(rateLimiterInterceptor);
      apiServer = serverBuilder.build();
      rateLimiterInterceptor.init(apiServer);
      apiServer.start();
    } catch (IOException e) {
      log.debug(e.getMessage(), e);
    }
    log.info("RpcApiService started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.err.println("*** shutting down gRPC server since JVM is shutting down");
      //server.this.stop();
      System.err.println("*** server shut down");
    }));
  }

  @Override
  public void stop() {
    if (apiServer != null) {
      apiServer.shutdown();
    }
  }

  public void blockUntilShutdown() {
    if (apiServer != null) {
      try {
        apiServer.awaitTermination();
      } catch (InterruptedException e) {
        log.warn("{}", e);
        Thread.currentThread().interrupt();
      }
    }
  }
}
