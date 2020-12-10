package cn.ledgeryi.framework.core.net.peer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.ledgeryi.chainbase.core.config.Parameter;
import cn.ledgeryi.protos.Protocol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.core.net.LedgerYiNetDelegate;

@Slf4j(topic = "net")
@Component
public class PeerStatusCheck {

  @Autowired
  private LedgerYiNetDelegate ledgerYiNetDelegate;

  private ScheduledExecutorService peerStatusCheckExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private int blockUpdateTimeout = 30_000;

  public void init() {
    peerStatusCheckExecutor.scheduleWithFixedDelay(() -> {
      try {
        statusCheck();
      } catch (Exception e) {
        log.error("Unhandled exception", e);
      }
    }, 5, 2, TimeUnit.SECONDS);
  }

  public void close() {
    peerStatusCheckExecutor.shutdown();
  }

  public void statusCheck() {

    long now = System.currentTimeMillis();

    ledgerYiNetDelegate.getActivePeer().forEach(peer -> {

      boolean isDisconnected = false;

      if (peer.isNeedSyncFromPeer()
          && peer.getBlockBothHaveUpdateTime() < now - blockUpdateTimeout) {
        log.warn("Peer {} not sync for a long time.", peer.getInetAddress());
        isDisconnected = true;
      }

      if (!isDisconnected) {
        isDisconnected = peer.getAdvInvRequest().values().stream()
            .anyMatch(time -> time < now - Parameter.NetConstants.ADV_TIME_OUT);
      }

      if (!isDisconnected) {
        isDisconnected = peer.getSyncBlockRequested().values().stream()
            .anyMatch(time -> time < now - Parameter.NetConstants.SYNC_TIME_OUT);
      }

      if (isDisconnected) {
        peer.disconnect(Protocol.ReasonCode.TIME_OUT);
      }
    });
  }

}
