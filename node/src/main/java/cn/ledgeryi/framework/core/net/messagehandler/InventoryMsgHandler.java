package cn.ledgeryi.framework.core.net.messagehandler;

import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.protos.Protocol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.core.net.LedgerYiNetDelegate;
import cn.ledgeryi.framework.core.net.message.InventoryMessage;
import cn.ledgeryi.framework.core.net.message.LedgerYiMessage;
import cn.ledgeryi.framework.core.net.peer.Item;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;
import cn.ledgeryi.framework.core.net.service.AdvService;

@Slf4j(topic = "net")
@Component
public class InventoryMsgHandler implements LedgerYiMsgHandler {

  @Autowired
  private LedgerYiNetDelegate ledgerYiNetDelegate;

  @Autowired
  private AdvService advService;

  @Autowired
  private TransactionsMsgHandler transactionsMsgHandler;

  private int maxCountIn10s = 10_000;

  @Override
  public void processMessage(PeerConnection peer, LedgerYiMessage msg) {
    InventoryMessage inventoryMessage = (InventoryMessage) msg;
    Protocol.Inventory.InventoryType type = inventoryMessage.getInventoryType();

    if (!check(peer, inventoryMessage)) {
      return;
    }

    for (Sha256Hash id : inventoryMessage.getHashList()) {
      Item item = new Item(id, type);
      peer.getAdvInvReceive().put(item, System.currentTimeMillis());
      advService.addInv(item);
    }
  }

  private boolean check(PeerConnection peer, InventoryMessage inventoryMessage) {
    Protocol.Inventory.InventoryType type = inventoryMessage.getInventoryType();
    int size = inventoryMessage.getHashList().size();

    if (peer.isNeedSyncFromPeer() || peer.isNeedSyncFromUs()) {
      log.warn("Drop inv: {} size: {} from Peer {}, syncFromUs: {}, syncFromPeer: {}.",
          type, size, peer.getInetAddress(), peer.isNeedSyncFromUs(), peer.isNeedSyncFromPeer());
      return false;
    }

    if (type.equals(Protocol.Inventory.InventoryType.TX)) {
      int count = peer.getNodeStatistics().messageStatistics.ledgerYiInTxInventoryElement.getCount(10);
      if (count > maxCountIn10s) {
        log.warn("Drop inv: {} size: {} from Peer {}, Inv count: {} is overload.",
            type, size, peer.getInetAddress(), count);
        return false;
      }

      if (transactionsMsgHandler.isBusy()) {
        log.warn("Drop inv: {} size: {} from Peer {}, transactionsMsgHandler is busy.",
            type, size, peer.getInetAddress());
        return false;
      }
    }

    return true;
  }
}
