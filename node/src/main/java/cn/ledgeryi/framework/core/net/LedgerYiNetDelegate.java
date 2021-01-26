package cn.ledgeryi.framework.core.net;


import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule.BlockId;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.chainbase.core.message.MessageTypes;
import cn.ledgeryi.chainbase.core.store.MasterScheduleStore;
import cn.ledgeryi.common.core.exception.*;
import cn.ledgeryi.common.core.exception.P2pException.TypeEnum;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.protos.Protocol;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.ledgeryi.framework.common.backup.BackupServer;
import cn.ledgeryi.framework.common.overlay.server.ChannelManager;
import cn.ledgeryi.framework.common.overlay.server.SyncPool;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.framework.core.exception.BadBlockException;
import cn.ledgeryi.framework.core.exception.ContractSizeNotEqualToOneException;
import cn.ledgeryi.framework.core.exception.DupTransactionException;
import cn.ledgeryi.framework.core.exception.TaposException;
import cn.ledgeryi.framework.core.exception.TooBigTransactionException;
import cn.ledgeryi.framework.core.exception.TransactionExpirationException;
import cn.ledgeryi.framework.core.exception.ValidateScheduleException;
import cn.ledgeryi.framework.core.net.message.BlockMessage;
import cn.ledgeryi.framework.core.net.message.TransactionMessage;
import cn.ledgeryi.framework.core.net.peer.PeerConnection;

import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

@Slf4j(topic = "net")
@Component
public class LedgerYiNetDelegate {

  @Autowired
  private SyncPool syncPool;

  @Autowired
  private ChannelManager channelManager;

  @Autowired
  private Manager dbManager;

  @Autowired
  private MasterScheduleStore masterScheduleStore;

  @Getter
  private Object blockLock = new Object();

  @Autowired
  private BackupServer backupServer;

  private volatile boolean backupServerStartFlag;

  private int blockIdCacheSize = 100;

  private Queue<BlockId> freshBlockId = new ConcurrentLinkedQueue<BlockId>() {
    @Override
    public boolean offer(BlockId blockId) {
      if (size() > blockIdCacheSize) {
        super.poll();
      }
      return super.offer(blockId);
    }
  };

  public void trustNode(PeerConnection peer) {
    channelManager.getTrustNodes().put(peer.getInetAddress(), peer.getNode());
  }

  public Collection<PeerConnection> getActivePeer() {
    return syncPool.getActivePeers();
  }

  public long getSyncBeginNumber() {
    return dbManager.getSyncBeginNumber();
  }

  public long getBlockTime(BlockId id) throws P2pException {
    try {
      return dbManager.getBlockById(id).getTimeStamp();
    } catch (BadItemException | ItemNotFoundException e) {
      throw new P2pException(P2pException.TypeEnum.DB_ITEM_NOT_FOUND, id.getString());
    }
  }

  public BlockId getHeadBlockId() {
    return dbManager.getHeadBlockId();
  }

  public BlockId getSolidBlockId() {
    return dbManager.getSolidBlockId();
  }

  public BlockId getGenesisBlockId() {
    return dbManager.getGenesisBlockId();
  }

  public BlockId getBlockIdByNum(long num) throws P2pException {
    try {
      return dbManager.getBlockIdByNum(num);
    } catch (ItemNotFoundException e) {
      throw new P2pException(TypeEnum.DB_ITEM_NOT_FOUND, "num: " + num);
    }
  }

  public BlockCapsule getGenesisBlock() {
    return dbManager.getGenesisBlock();
  }

  public long getHeadBlockTimeStamp() {
    return dbManager.getHeadBlockTimeStamp();
  }

  public boolean containBlock(BlockId id) {
    return dbManager.containBlock(id);
  }

  public boolean containBlockInMainChain(BlockId id) {
    return dbManager.containBlockInMainChain(id);
  }

  public List<BlockId> getBlockChainHashesOnFork(BlockId forkBlockHash) throws P2pException {
    try {
      return dbManager.getBlockChainHashesOnFork(forkBlockHash);
    } catch (NonCommonBlockException e) {
      throw new P2pException(TypeEnum.HARD_FORKED, forkBlockHash.getString());
    }
  }

  public boolean canChainRevoke(long num) {
    return num >= dbManager.getSyncBeginNumber();
  }

  public boolean contain(Sha256Hash hash, MessageTypes type) {
    if (type.equals(MessageTypes.BLOCK)) {
      return dbManager.containBlock(hash);
    } else if (type.equals(MessageTypes.TX)) {
      return dbManager.getTransactionStore().has(hash.getBytes());
    }
    return false;
  }

  public Message getData(Sha256Hash hash, Protocol.Inventory.InventoryType type) throws P2pException {
    try {
      switch (type) {
        case BLOCK:
          return new BlockMessage(dbManager.getBlockById(hash));
        case TX:
          TransactionCapsule tx = dbManager.getTransactionStore().get(hash.getBytes());
          if (tx != null) {
            return new TransactionMessage(tx.getInstance());
          }
          throw new StoreException();
        default:
          throw new StoreException();
      }
    } catch (StoreException e) {
      throw new P2pException(TypeEnum.DB_ITEM_NOT_FOUND,
          "type: " + type + ", hash: " + hash.getByteString());
    }
  }

  public void processBlock(BlockCapsule block) throws P2pException {
    BlockId blockId = block.getBlockId();
    synchronized (blockLock) {
      try {
        if (!freshBlockId.contains(blockId)) {
          if (block.getNum() <= getHeadBlockId().getNum()) {
            log.warn("Receive a fork block {} master {}, head {}",
                block.getBlockId().getString(),
                Hex.toHexString(block.getMasterAddress().toByteArray()),
                getHeadBlockId().getString());
          }
          dbManager.pushBlock(block);
          freshBlockId.add(blockId);
          log.debug("Success process block {}.", blockId.getString());
          if (!backupServerStartFlag && System.currentTimeMillis() - block.getTimeStamp() < BLOCK_PRODUCED_INTERVAL) {
            backupServerStartFlag = true;
            backupServer.initServer();
          }
        }
      } catch (ValidateSignatureException
          | ContractValidateException
          | ContractExeException
          | UnLinkedBlockException
          | ValidateScheduleException
          | TaposException
          | TooBigTransactionException
          | DupTransactionException
          | TransactionExpirationException
          | BadNumberBlockException
          | BadBlockException
          | NonCommonBlockException
          | ReceiptCheckErrException
           | VMIllegalException e) {
        log.error("Process block failed, {}, reason: {}.", blockId.getString(), e.getMessage());
        throw new P2pException(TypeEnum.BAD_BLOCK, e);
      }
    }
  }

  public void pushTransaction(TransactionCapsule tx) throws P2pException {
    try {
      tx.setTime(System.currentTimeMillis());
      dbManager.pushTransaction(tx);
    } catch (ContractSizeNotEqualToOneException e) {
      throw new P2pException(TypeEnum.BAD_TX, e);
    } catch (ContractValidateException
        | ValidateSignatureException
        | ContractExeException
        | DupTransactionException
        | TaposException
        | TooBigTransactionException
        | TransactionExpirationException
        | ReceiptCheckErrException
            | VMIllegalException e) {
      throw new P2pException(TypeEnum.TX_EXE_FAILED, e);
    }
  }

  public boolean validBlock(BlockCapsule block) throws P2pException {
    try {
      return masterScheduleStore.getActiveMasters().contains(block.getMasterAddress())
          && block.validateSignature(dbManager.getDynamicPropertiesStore(), dbManager.getAccountStore());
    } catch (ValidateSignatureException e) {
      throw new P2pException(TypeEnum.BAD_BLOCK, e);
    }
  }
}
