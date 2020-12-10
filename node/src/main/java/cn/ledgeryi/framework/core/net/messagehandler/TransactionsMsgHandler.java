package cn.ledgeryi.framework.core.net.messagehandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.ledgeryi.common.core.exception.P2pException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.net.LedgerYiNetDelegate;
import cn.ledgeryi.framework.core.net.message.TransactionMessage;
import cn.ledgeryi.framework.core.net.message.TransactionsMessage;
import cn.ledgeryi.framework.core.net.message.LedgerYiMessage;
import cn.ledgeryi.framework.core.net.peer.Item;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;
import cn.ledgeryi.framework.core.net.service.AdvService;
import cn.ledgeryi.protos.Protocol.Inventory.InventoryType;
import cn.ledgeryi.protos.Protocol.ReasonCode;
import cn.ledgeryi.protos.Protocol.Transaction;

@Slf4j(topic = "net")
@Component
public class TransactionsMsgHandler implements LedgerYiMsgHandler {

  private static int MAX_TX_SIZE = 50_000;
  private static int MAX_SMART_CONTRACT_SUBMIT_SIZE = 100;
  @Autowired
  private LedgerYiNetDelegate ledgerYiNetDelegate;
  @Autowired
  private AdvService advService;

  //  private static int TIME_OUT = 10 * 60 * 1000;
  private BlockingQueue<TxEvent> smartContractQueue = new LinkedBlockingQueue(MAX_TX_SIZE);

  private BlockingQueue<Runnable> queue = new LinkedBlockingQueue();

  private int threadNum = Args.getInstance().getValidateSignThreadNum();

  private ExecutorService txHandlePool = new ThreadPoolExecutor(threadNum, threadNum, 0L, TimeUnit.MILLISECONDS, queue);

  private ScheduledExecutorService smartContractExecutor = Executors.newSingleThreadScheduledExecutor();

  public void init() {
    handleSmartContract();
  }

  public void close() {
    smartContractExecutor.shutdown();
  }

  public boolean isBusy() {
    return queue.size() + smartContractQueue.size() > MAX_TX_SIZE;
  }

  @Override
  public void processMessage(PeerConnection peer, LedgerYiMessage msg) throws P2pException {
    TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
    log.debug("[processTxMessage] [processMessage] {} ,{} from peer {}", msg,
            ((TransactionsMessage) msg).getTransactions(), peer.getInetAddress());
    check(peer, transactionsMessage);
    for (Transaction tx : transactionsMessage.getTransactions().getTransactionsList()) {
      txHandlePool.submit(() -> handleTransaction(peer, new TransactionMessage(tx)));
    }
  }

  private void check(PeerConnection peer, TransactionsMessage msg) throws P2pException {
    for (Transaction tx : msg.getTransactions().getTransactionsList()) {
      Item item = new Item(new TransactionMessage(tx).getMessageId(), InventoryType.TX);
      if (!peer.getAdvInvRequest().containsKey(item)) {
        throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "tx: " + msg.getMessageId() + " without request.");
      }
      peer.getAdvInvRequest().remove(item);
    }
  }

  private void handleSmartContract() {
    smartContractExecutor.scheduleWithFixedDelay(() -> {
      try {
        while (queue.size() < MAX_SMART_CONTRACT_SUBMIT_SIZE) {
          TxEvent event = smartContractQueue.take();
          txHandlePool.submit(() -> handleTransaction(event.getPeer(), event.getMsg()));
        }
      } catch (Exception e) {
        log.error("Handle smart contract exception.", e);
      }
    }, 1000, 20, TimeUnit.MILLISECONDS);
  }

  private void handleTransaction(PeerConnection peer, TransactionMessage tx) {
    if (peer.isDisconnect()) {
      log.warn("Drop tx {} from {}, peer is disconnect.", tx.getMessageId(), peer.getInetAddress());
      return;
    }
    if (advService.getMessage(new Item(tx.getMessageId(), InventoryType.TX)) != null) {
      return;
    }
    try {
      ledgerYiNetDelegate.pushTransaction(tx.getTransactionCapsule());
      advService.broadcast(tx);
    } catch (P2pException e) {
      log.warn("Tx {} from peer {} process failed. type: {}, reason: {}",
          tx.getMessageId(), peer.getInetAddress(), e.getType(), e.getMessage());
      if (e.getType().equals(P2pException.TypeEnum.BAD_TX)) {
        peer.disconnect(ReasonCode.BAD_TX);
      }
    } catch (Exception e) {
      log.error("Tx {} from peer {} process failed.", tx.getMessageId(), peer.getInetAddress(), e);
    }
  }

  class TxEvent {

    @Getter
    private PeerConnection peer;
    @Getter
    private TransactionMessage msg;
    @Getter
    private long time;

    public TxEvent(PeerConnection peer, TransactionMessage msg) {
      this.peer = peer;
      this.msg = msg;
      this.time = System.currentTimeMillis();
    }
  }
}