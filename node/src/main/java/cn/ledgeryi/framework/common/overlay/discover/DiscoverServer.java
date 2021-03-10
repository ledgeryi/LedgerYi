package cn.ledgeryi.framework.common.overlay.discover;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.net.udp.handler.MessageHandler;
import cn.ledgeryi.framework.common.net.udp.handler.PacketDecoder;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;
import cn.ledgeryi.framework.common.overlay.server.WireTrafficStats;
import cn.ledgeryi.framework.core.config.args.Args;

@Slf4j(topic = "discover")
@Component
public class DiscoverServer {

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private WireTrafficStats stats;

  private Channel channel;
  private Args args = Args.getInstance();
  private volatile boolean shutdown = false;
  private int port = args.getNodeListenPort();
  private DiscoveryExecutor discoveryExecutor;

  @Autowired
  public DiscoverServer(final NodeManager nodeManager) {
    if (Args.getInstance().isPermissionNet()){
      return;
    }
    this.nodeManager = nodeManager;
    if (args.isNodeDiscoveryEnable() && !args.isFastForward()) {
      if (port == 0) {
        log.error("Discovery can't be started while listen port == 0");
      } else {
        new Thread(() -> {
          try {
            start();
          } catch (Exception e) {
            log.error("Discovery server start failed.", e);
          }
        }, "DiscoverServer").start();
      }
    }
  }

  public void start() throws Exception {
    NioEventLoopGroup group = new NioEventLoopGroup(args.getUdpNettyWorkThreadNum());
    try {
      discoveryExecutor = new DiscoveryExecutor(nodeManager);
      discoveryExecutor.start();
      while (!shutdown) {
        Bootstrap b = new Bootstrap();
        b.group(group)
            .channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<NioDatagramChannel>() {
              @Override
              public void initChannel(NioDatagramChannel ch)
                  throws Exception {
                ch.pipeline().addLast(stats.udp);
                ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                ch.pipeline().addLast(new PacketDecoder());
                MessageHandler messageHandler = new MessageHandler(ch, nodeManager);
                nodeManager.setMessageSender(messageHandler);
                ch.pipeline().addLast(messageHandler);
              }
            });

        channel = b.bind(port).sync().channel();

        log.info("Discovery server started, bind port {}", port);

        channel.closeFuture().sync();
        if (shutdown) {
          log.info("Shutdown discovery server");
          break;
        }
        log.warn(" Restart discovery server after 5 sec pause...");
        Thread.sleep(5000);
      }
    } catch (Exception e) {
      log.error("Start discovery server with port {} failed.", port, e);
    } finally {
      group.shutdownGracefully().sync();
    }
  }

  public void close() {
    log.info("Closing discovery server...");
    shutdown = true;
    if (channel != null) {
      try {
        channel.close().await(10, TimeUnit.SECONDS);
      } catch (Exception e) {
        log.info("Closing discovery server failed.", e);
      }
    }

    if (discoveryExecutor != null) {
      try {
        discoveryExecutor.close();
      } catch (Exception e) {
        log.info("Closing discovery executor failed.", e);
      }
    }
  }

}
