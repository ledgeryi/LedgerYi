package cn.ledgeryi.framework.core.net.service;

import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.common.utils.Time;
import cn.ledgeryi.protos.Protocol;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.overlay.discover.node.statistics.MessageCount;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.net.LedgerYiNetDelegate;
import cn.ledgeryi.framework.core.net.message.BlockMessage;
import cn.ledgeryi.framework.core.net.message.FetchInvDataMessage;
import cn.ledgeryi.framework.core.net.message.InventoryMessage;
import cn.ledgeryi.framework.core.net.message.TransactionMessage;
import cn.ledgeryi.framework.core.net.peer.Item;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;

import static cn.ledgeryi.chainbase.core.config.Parameter.NetConstants.MAX_TX_FETCH_PER_PEER;
import static cn.ledgeryi.chainbase.core.config.Parameter.NetConstants.MSG_CACHE_DURATION_IN_BLOCKS;
import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

@Slf4j(topic = "net")
@Component
public class AdvService {

    @Autowired
    private LedgerYiNetDelegate ledgerYiNetDelegate;

    private ConcurrentHashMap<Item, Long> invToFetch = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Item, Long> invToSpread = new ConcurrentHashMap<>();

    private Cache<Item, Long> invToFetchCache = CacheBuilder.newBuilder()
            .maximumSize(100_000).expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

    private Cache<Item, Message> txCache = CacheBuilder.newBuilder()
            .maximumSize(50_000).expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

    private Cache<Item, Message> blockCache = CacheBuilder.newBuilder()
            .maximumSize(10).expireAfterWrite(1, TimeUnit.MINUTES).recordStats().build();

    private ScheduledExecutorService spreadExecutor = Executors.newSingleThreadScheduledExecutor();

    private ScheduledExecutorService fetchExecutor = Executors.newSingleThreadScheduledExecutor();

    @Getter
    private MessageCount txCount = new MessageCount();

    private int maxSpreadSize = 5_000;

    private boolean fastForward = Args.getInstance().isFastForward();

    public void init() {
        /*if (fastForward) {
          return;
        }*/
        spreadExecutor.scheduleWithFixedDelay(() -> {
            try {
                consumerInvToSpread();
            } catch (Exception exception) {
                log.error("Spread thread error.", exception.getMessage());
            }
        }, 100, 30, TimeUnit.MILLISECONDS);

        fetchExecutor.scheduleWithFixedDelay(() -> {
            try {
                consumerInvToFetch();
            } catch (Exception exception) {
                log.error("Fetch thread error.", exception.getMessage());
            }
        }, 100, 30, TimeUnit.MILLISECONDS);
    }

    public void close() {
        spreadExecutor.shutdown();
        fetchExecutor.shutdown();
    }

    public synchronized void addInvToCache(Item item) {
        invToFetchCache.put(item, System.currentTimeMillis());
        invToFetch.remove(item);
    }

    public synchronized boolean addInv(Item item) {

        if (fastForward && item.getType().equals(Protocol.Inventory.InventoryType.TX)) {
            return false;
        }

        if (invToFetchCache.getIfPresent(item) != null) {
            return false;
        }

        if (item.getType().equals(Protocol.Inventory.InventoryType.TX)) {
            if (txCache.getIfPresent(item) != null) {
                return false;
            }
        } else {
            if (blockCache.getIfPresent(item) != null) {
                return false;
            }
        }

        invToFetchCache.put(item, System.currentTimeMillis());
        invToFetch.put(item, System.currentTimeMillis());

        if (Protocol.Inventory.InventoryType.BLOCK.equals(item.getType())) {
            consumerInvToFetch();
        }

        return true;
    }

    public Message getMessage(Item item) {
        if (item.getType() == Protocol.Inventory.InventoryType.TX) {
            return txCache.getIfPresent(item);
        } else {
            return blockCache.getIfPresent(item);
        }
    }

    /**
     * 广播交易或区块
     */
    public void broadcast(Message msg) {
        /*if (fastForward) {
          return;
        }*/
        if (invToSpread.size() > maxSpreadSize) {
            log.warn("Drop message, type: {}, ID: {}.", msg.getType(), msg.getMessageId());
            return;
        }
        Item item;
        if (msg instanceof BlockMessage) {
            BlockMessage blockMsg = (BlockMessage) msg;
            item = new Item(blockMsg.getMessageId(), Protocol.Inventory.InventoryType.BLOCK);
            log.info("Ready to broadcast block {}", blockMsg.getBlockId().getString());
            blockMsg.getBlockCapsule().getTransactions().forEach(transactionCapsule -> {
                Sha256Hash tid = transactionCapsule.getTransactionId();
                invToSpread.remove(tid);
                txCache.put(new Item(tid, Protocol.Inventory.InventoryType.TX), new TransactionMessage(transactionCapsule.getInstance()));
            });
            blockCache.put(item, msg);
        } else if (msg instanceof TransactionMessage) {
            TransactionMessage txMsg = (TransactionMessage) msg;
            log.debug("Ready to broadcast tx {}", txMsg.getMessageId());
            item = new Item(txMsg.getMessageId(), Protocol.Inventory.InventoryType.TX);
            txCount.add();
            txCache.put(item, new TransactionMessage(txMsg.getTransactionCapsule().getInstance()));
        } else {
            log.error("Adv item is neither block nor tx, type: {}", msg.getType());
            return;
        }
        invToSpread.put(item, System.currentTimeMillis());
        if (Protocol.Inventory.InventoryType.BLOCK.equals(item.getType())) {
            consumerInvToSpread();
        }
    }

    public void fastForward(BlockMessage msg) {
        Item item = new Item(msg.getBlockId(), Protocol.Inventory.InventoryType.BLOCK);
        List<PeerConnection> peers = ledgerYiNetDelegate.getActivePeer().stream()
                .filter(peer -> !peer.isNeedSyncFromPeer() && !peer.isNeedSyncFromUs())
                .filter(peer -> peer.getAdvInvReceive().getIfPresent(item) == null
                        && peer.getAdvInvSpread().getIfPresent(item) == null)
                .collect(Collectors.toList());

        if (!fastForward) {
            peers = peers.stream().filter(peer -> peer.isFastForwardPeer()).collect(Collectors.toList());
        }

        peers.forEach(peer -> {
            peer.sendMessage(msg);
            peer.getAdvInvSpread().put(item, System.currentTimeMillis());
            peer.setFastForwardBlock(msg.getBlockId());
        });
    }


    public void onDisconnect(PeerConnection peer) {
        if (!peer.getAdvInvRequest().isEmpty()) {
            peer.getAdvInvRequest().keySet().forEach(item -> {
                if (ledgerYiNetDelegate.getActivePeer().stream()
                        .anyMatch(p -> !p.equals(peer) && p.getAdvInvReceive().getIfPresent(item) != null)) {
                    invToFetch.put(item, System.currentTimeMillis());
                } else {
                    invToFetchCache.invalidate(item);
                }
            });
        }

        if (invToFetch.size() > 0) {
            consumerInvToFetch();
        }
    }

    private synchronized void consumerInvToFetch() {
        Collection<PeerConnection> peers = ledgerYiNetDelegate.getActivePeer().stream()
                .filter(peer -> peer.isIdle()).collect(Collectors.toList());

        if (invToFetch.isEmpty() || peers.isEmpty()) {
            return;
        }

        InvSender invSender = new InvSender();
        long now = System.currentTimeMillis();
        invToFetch.forEach((item, time) -> {
            if (time < now - MSG_CACHE_DURATION_IN_BLOCKS * BLOCK_PRODUCED_INTERVAL) {
                log.info("This obj is too late to fetch, type: {} hash: {}.", item.getType(), item.getHash());
                invToFetch.remove(item);
                invToFetchCache.invalidate(item);
                return;
            }
            peers.stream().filter(peer -> peer.getAdvInvReceive().getIfPresent(item) != null
                    && invSender.getSize(peer) < MAX_TX_FETCH_PER_PEER).sorted(
                    Comparator.comparingInt(peer -> invSender.getSize(peer))).findFirst().ifPresent(
                    peer -> {
                        invSender.add(item, peer);
                        peer.getAdvInvRequest().put(item, now);
                        invToFetch.remove(item);
                    });
        });

        invSender.sendFetch();
    }

    /**
     * 广播区块或交易给active peer
     */
    private synchronized void consumerInvToSpread() {
        List<PeerConnection> peers = ledgerYiNetDelegate.getActivePeer().stream()
                .filter(peer -> !peer.isNeedSyncFromPeer() && !peer.isNeedSyncFromUs())
                .collect(Collectors.toList());

        if (invToSpread.isEmpty() || peers.isEmpty()) {
            return;
        }
        InvSender invSender = new InvSender();
        invToSpread.forEach((item, time) -> peers.forEach(peer -> {
            if (peer.getAdvInvReceive().getIfPresent(item) == null
                    && peer.getAdvInvSpread().getIfPresent(item) == null
                    && !(item.getType().equals(Protocol.Inventory.InventoryType.BLOCK)
                    && System.currentTimeMillis() - time > BLOCK_PRODUCED_INTERVAL)) {
                peer.getAdvInvSpread().put(item, Time.getCurrentMillis());
                invSender.add(item, peer);
            }
            invToSpread.remove(item);
        }));

        invSender.sendInv();
    }

    class InvSender {

        private HashMap<PeerConnection, HashMap<Protocol.Inventory.InventoryType, LinkedList<Sha256Hash>>> send = new HashMap<>();

        public void clear() {
            this.send.clear();
        }

        public void add(Map.Entry<Sha256Hash, Protocol.Inventory.InventoryType> id, PeerConnection peer) {
            if (send.containsKey(peer) && !send.get(peer).containsKey(id.getValue())) {
                send.get(peer).put(id.getValue(), new LinkedList<>());
            } else if (!send.containsKey(peer)) {
                send.put(peer, new HashMap<>());
                send.get(peer).put(id.getValue(), new LinkedList<>());
            }
            send.get(peer).get(id.getValue()).offer(id.getKey());
        }

        public void add(Item id, PeerConnection peer) {
            if (send.containsKey(peer) && !send.get(peer).containsKey(id.getType())) {
                send.get(peer).put(id.getType(), new LinkedList<>());
            } else if (!send.containsKey(peer)) {
                send.put(peer, new HashMap<>());
                send.get(peer).put(id.getType(), new LinkedList<>());
            }
            send.get(peer).get(id.getType()).offer(id.getHash());
        }

        public int getSize(PeerConnection peer) {
            if (send.containsKey(peer)) {
                return send.get(peer).values().stream().mapToInt(LinkedList::size).sum();
            }
            return 0;
        }

        public void sendInv() {
            send.forEach((peer, ids) -> ids.forEach((key, value) -> {
                if (peer.isFastForwardPeer() && key.equals(Protocol.Inventory.InventoryType.TX)) {
                    return;
                }
                if (key.equals(Protocol.Inventory.InventoryType.BLOCK)) {
                    value.sort(Comparator.comparingLong(value1 -> new BlockCapsule.BlockId(value1).getNum()));
                }
                //TYPE : INVENTORY
                InventoryMessage inventoryMessage = new InventoryMessage(value, key);
                peer.sendMessage(inventoryMessage);
            }));
        }

        void sendFetch() {
            send.forEach((peer, ids) -> ids.forEach((key, value) -> {
                if (key.equals(Protocol.Inventory.InventoryType.BLOCK)) {
                    value.sort(Comparator.comparingLong(value1 -> new BlockCapsule.BlockId(value1).getNum()));
                }
                peer.sendMessage(new FetchInvDataMessage(value, key));
            }));
        }
    }

}
