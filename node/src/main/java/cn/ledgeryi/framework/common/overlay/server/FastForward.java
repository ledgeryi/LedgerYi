package cn.ledgeryi.framework.common.overlay.server;

import cn.ledgeryi.chainbase.core.store.MasterScheduleStore;
import com.google.protobuf.ByteString;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.backup.BackupManager;
import cn.ledgeryi.framework.common.backup.BackupManager.BackupStatusEnum;
import cn.ledgeryi.framework.common.overlay.discover.node.Node;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class FastForward {

  @Autowired
  private ApplicationContext ctx;

  private ChannelManager channelManager;

  private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  private Args args = Args.getInstance();
  private List<Node> fastForwardNodes = args.getFastForwardNodes();
  private ByteString masterAddress = ByteString.copyFrom(Args.getInstance().
          getLocalMasters().getMasterAccountAddress(Args.getInstance().isEccCryptoEngine()));
  private int keySize = args.getLocalMasters().getPrivateKeys().size();

  public void init() {

    log.info("Fast forward config, isMaster: {}, keySize: {}, fastForwardNodes: {}",
        args.isMaster(), keySize, fastForwardNodes.size());

    if (!args.isMaster() || keySize == 0 || fastForwardNodes.isEmpty()) {
      return;
    }

    channelManager = ctx.getBean(ChannelManager.class);
    BackupManager backupManager = ctx.getBean(BackupManager.class);
    MasterScheduleStore masterScheduleStore = ctx.getBean(MasterScheduleStore.class);

    executorService.scheduleWithFixedDelay(() -> {
      try {
        if (masterScheduleStore.getActiveMasters().contains(masterAddress) &&
            backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
          connect();
        } else {
          disconnect();
        }
      } catch (Exception e) {
        log.info("Execute failed.", e);
      }
    }, 30, 100, TimeUnit.SECONDS);
  }

  private void connect() {
    fastForwardNodes.forEach(node -> {
      InetSocketAddress socketAddress = new InetSocketAddress(node.getHost(), node.getPort());
      //InetAddress address = socketAddress.getAddress();
      channelManager.getActiveNodes().put(socketAddress, node);
    });
  }

  private void disconnect() {
    fastForwardNodes.forEach(node -> {
      InetSocketAddress socketAddress = new InetSocketAddress(node.getHost(), node.getPort());
      InetAddress address = socketAddress.getAddress();
      channelManager.getActiveNodes().remove(socketAddress);
      channelManager.getActivePeers().forEach(channel -> {
        if (channel.getInetAddress().equals(address)) {
          channel.disconnect(ReasonCode.RESET);
        }
      });
    });
  }
}
