package cn.ledgeryi.framework.core.net;

import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.common.core.exception.P2pException;
import cn.ledgeryi.framework.common.overlay.server.ChannelManager;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.framework.core.net.message.LedgerYiMessage;
import cn.ledgeryi.framework.core.net.messagehandler.*;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;
import cn.ledgeryi.framework.core.net.peer.PeerStatusCheck;
import cn.ledgeryi.framework.core.net.service.AdvService;
import cn.ledgeryi.framework.core.net.service.SyncService;
import cn.ledgeryi.protos.Protocol.ReasonCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j(topic = "net")
@Component
public class LedgerYiNetService {

  @Autowired
  private ChannelManager channelManager;

  @Autowired
  private AdvService advService;

  @Autowired
  private SyncService syncService;

  @Autowired
  private PeerStatusCheck peerStatusCheck;

  @Autowired
  private SyncBlockChainMsgHandler syncBlockChainMsgHandler;

  @Autowired
  private ChainInventoryMsgHandler chainInventoryMsgHandler;

  @Autowired
  private InventoryMsgHandler inventoryMsgHandler;


  @Autowired
  private FetchInvDataMsgHandler fetchInvDataMsgHandler;

  @Autowired
  private BlockMsgHandler blockMsgHandler;

  @Autowired
  private TransactionsMsgHandler transactionsMsgHandler;

  @Autowired
  private Manager manager;

  public void start() {
    manager.setLedgerYiNetService(this);
    channelManager.init();
    advService.init();
    syncService.init();
    peerStatusCheck.init();
    transactionsMsgHandler.init();
    log.info("LedgerYiNetService start successfully.");
  }

  public void stop() {
    channelManager.close();
    advService.close();
    syncService.close();
    peerStatusCheck.close();
    transactionsMsgHandler.close();
    log.info("LedgerYiNetService closed successfully.");
  }

  /**
   * broadcast tx or block
   */
  public void broadcast(Message msg) {
    advService.broadcast(msg);
  }

  protected void onMessage(PeerConnection peer, LedgerYiMessage msg) {
    try {
      switch (msg.getType()) {
        case SYNC_BLOCK_CHAIN:
          syncBlockChainMsgHandler.processMessage(peer, msg);
          break;
        case BLOCK_CHAIN_INVENTORY:
          chainInventoryMsgHandler.processMessage(peer, msg);
          break;
        case INVENTORY:
          inventoryMsgHandler.processMessage(peer, msg);
          break;
        case FETCH_INV_DATA:
          fetchInvDataMsgHandler.processMessage(peer, msg);
          break;
        case BLOCK:
          blockMsgHandler.processMessage(peer, msg);
          break;
        case TXS:
          transactionsMsgHandler.processMessage(peer, msg);
          break;
        default:
          throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, msg.getType().toString());
      }
    } catch (Exception e) {
      processException(peer, msg, e);
    }
  }

  private void processException(PeerConnection peer, LedgerYiMessage msg, Exception ex) {
    ReasonCode code;
    if (ex instanceof P2pException) {
      P2pException.TypeEnum type = ((P2pException) ex).getType();
      switch (type) {
        case BAD_TX:
          code = ReasonCode.BAD_TX;
          break;
        case BAD_BLOCK:
          code = ReasonCode.BAD_BLOCK;
          break;
        case NO_SUCH_MESSAGE:
        case MESSAGE_WITH_WRONG_LENGTH:
        case BAD_MESSAGE:
          code = ReasonCode.BAD_PROTOCOL;
          break;
        case SYNC_FAILED:
          code = ReasonCode.SYNC_FAIL;
          break;
        case UNLINK_BLOCK:
          code = ReasonCode.UNLINKABLE;
          break;
        default:
          code = ReasonCode.UNKNOWN;
          break;
      }
      log.error("Message from {} process failed, {} \n type: {}, detail: {}.", peer.getInetAddress(), msg, type, ex.getMessage());
    } else {
      code = ReasonCode.UNKNOWN;
      log.error("Message from {} process failed, {}", peer.getInetAddress(), msg, ex);
    }
    peer.disconnect(code);
  }
}
