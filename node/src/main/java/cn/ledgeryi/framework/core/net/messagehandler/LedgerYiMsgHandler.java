package cn.ledgeryi.framework.core.net.messagehandler;

import cn.ledgeryi.common.core.exception.P2pException;
import cn.ledgeryi.framework.core.net.message.LedgerYiMessage;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;

public interface LedgerYiMsgHandler {

  void processMessage(PeerConnection peer, LedgerYiMessage msg) throws P2pException, P2pException;

}
