package cn.ledgeryi.framework.core;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.*;
import cn.ledgeryi.api.GrpcAPI.Return.response_code;
import cn.ledgeryi.chainbase.actuator.Actuator;
import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.capsule.*;
import cn.ledgeryi.chainbase.core.store.AccountIdIndexStore;
import cn.ledgeryi.chainbase.core.store.AccountStore;
import cn.ledgeryi.common.core.exception.*;
import cn.ledgeryi.common.utils.Base58;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignUtils;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeHandler;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;
import cn.ledgeryi.framework.common.utils.Utils;
import cn.ledgeryi.framework.core.actuator.ActuatorFactory;
import cn.ledgeryi.framework.core.capsule.TransactionInfoCapsule;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.framework.core.exception.DupTransactionException;
import cn.ledgeryi.framework.core.exception.TaposException;
import cn.ledgeryi.framework.core.exception.TooBigTransactionException;
import cn.ledgeryi.framework.core.exception.TransactionExpirationException;
import cn.ledgeryi.framework.core.net.LedgerYiNetDelegate;
import cn.ledgeryi.framework.core.net.LedgerYiNetService;
import cn.ledgeryi.framework.core.net.message.TransactionMessage;
import cn.ledgeryi.protos.Protocol.Account;
import cn.ledgeryi.protos.Protocol.Block;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.ledgeryi.chainbase.common.utils.Commons.ADDRESS_SIZE;
import static cn.ledgeryi.chainbase.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

@Slf4j
@Component
public class Wallet {

  private int minEffectiveConnection = Args.getInstance().getMinEffectiveConnection();
  @Getter
  private final SignInterface cryptoEngine;
  @Autowired
  private LedgerYiNetService ledgerYiNetService;
  @Autowired
  private LedgerYiNetDelegate ledgerYiNetDelegate;
  @Autowired
  private Manager dbManager;
  @Autowired
  private NodeManager nodeManager;

  /**
   * Creates a new Wallet with a random ECKey.
   */
  public Wallet() {
    this.cryptoEngine = SignUtils.getGeneratedRandomSign(Utils.getRandom(),
        Args.getInstance().isECKeyCryptoEngine());
  }

  /**
   * Creates a Wallet with an existing ECKey.
   */
  public Wallet(final SignInterface cryptoEngine) {
    this.cryptoEngine = cryptoEngine;
    log.info("wallet address: {}", ByteArray.toHexString(this.cryptoEngine.getAddress()));
  }

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      log.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != ADDRESS_SIZE / 2) {
      log.warn(
          "Warning: Address length requires " + ADDRESS_SIZE + " but " + address.length + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  private static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(DBConfig.isECKeyCryptoEngine(), decodeData);
    byte[] hash1 = Sha256Hash.hash(DBConfig.isECKeyCryptoEngine(), hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
        hash1[1] == decodeCheck[decodeData.length + 1] &&
        hash1[2] == decodeCheck[decodeData.length + 2] &&
        hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }


  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      log.warn("Warning: Address is empty !!");
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (address == null) {
      return null;
    }
    if (!addressValid(address)) {
      return null;
    }
    return address;
  }

  public byte[] getAddress() {
    return cryptoEngine.getAddress();
  }

  public Account getAccount(Account account) {
    AccountStore accountStore = dbManager.getAccountStore();
    AccountCapsule accountCapsule = accountStore.get(account.getAddress().toByteArray());
    if (accountCapsule == null) {
      return null;
    }
    long genesisTimeStamp = dbManager.getGenesisBlock().getTimeStamp();
    accountCapsule.setLatestConsumeTime(genesisTimeStamp + BLOCK_PRODUCED_INTERVAL * accountCapsule.getLatestConsumeTime());
    return accountCapsule.getInstance();
  }

  public Account getAccountById(Account account) {
    AccountStore accountStore = dbManager.getAccountStore();
    AccountIdIndexStore accountIdIndexStore = dbManager.getAccountIdIndexStore();
    byte[] address = accountIdIndexStore.get(account.getAccountId());
    if (address == null) {
      return null;
    }
    AccountCapsule accountCapsule = accountStore.get(address);
    if (accountCapsule == null) {
      return null;
    }

    long genesisTimeStamp = dbManager.getGenesisBlock().getTimeStamp();
    accountCapsule.setLatestConsumeTime(genesisTimeStamp + BLOCK_PRODUCED_INTERVAL * accountCapsule.getLatestConsumeTime());
    return accountCapsule.getInstance();
  }

  private TransactionCapsule createTransactionCapsuleWithoutValidate(com.google.protobuf.Message message,
                                                                     ContractType contractType) {
    TransactionCapsule tx = new TransactionCapsule(message, contractType);
    try {
      BlockCapsule.BlockId blockId = dbManager.getHeadBlockId();
      if ("solid".equals(Args.getInstance().getTxReferenceBlock())) {
        blockId = dbManager.getSolidBlockId();
      }
      tx.setReference(blockId.getNum(), blockId.getBytes());
      long expiration = dbManager.getHeadBlockTimeStamp() + Args.getInstance().getTxExpirationTimeInMilliseconds();
      tx.setExpiration(expiration);
      tx.setTimestamp();
    } catch (Exception e) {
      log.error("Create transaction capsule failed.", e);
    }
    return tx;
  }

  public TransactionCapsule createTransactionCapsule(com.google.protobuf.Message message, ContractType contractType)
          throws ContractValidateException {
    TransactionCapsule tx = new TransactionCapsule(message, contractType);
    List<Actuator> actList = ActuatorFactory.createActuator(tx, dbManager);
    for (Actuator act : actList) {
      act.validate();
    }
    return createTransactionCapsuleWithoutValidate(message,contractType);
  }

  /**
   * Broadcast a transaction.
   */
  public GrpcAPI.Return broadcastTransaction(Transaction signaturedTransaction) {
    GrpcAPI.Return.Builder builder = GrpcAPI.Return.newBuilder();
    TransactionCapsule tx = new TransactionCapsule(signaturedTransaction);
    tx.setTime(System.currentTimeMillis());
    try {
      Message message = new TransactionMessage(signaturedTransaction.toByteArray());
      if (minEffectiveConnection != 0) {
        if (ledgerYiNetDelegate.getActivePeer().isEmpty()) {
          log.warn("Broadcast transaction {} has failed, no connection.", tx.getTransactionId());
          return builder.setResult(false).setCode(response_code.NO_CONNECTION)
              .setMessage(ByteString.copyFromUtf8("no connection")).build();
        }

        int count = (int) ledgerYiNetDelegate.getActivePeer().stream()
            .filter(p -> !p.isNeedSyncFromUs() && !p.isNeedSyncFromPeer()).count();

        if (count < minEffectiveConnection) {
          String info = "effective connection:" + count + " lt minEffectiveConnection:" + minEffectiveConnection;
          log.warn("Broadcast transaction {} has failed, {}.", tx.getTransactionId(), info);
          return builder.setResult(false).setCode(response_code.NOT_ENOUGH_EFFECTIVE_CONNECTION)
              .setMessage(ByteString.copyFromUtf8(info)).build();
        }
      }

      if (dbManager.isTooManyPending()) {
        log.warn("Broadcast transaction {} has failed, too many pending.", tx.getTransactionId());
        return builder.setResult(false).setCode(response_code.SERVER_BUSY).build();
      }

      if (dbManager.getTransactionIdCache().getIfPresent(tx.getTransactionId()) != null) {
        log.warn("Broadcast transaction {} has failed, it already exists.", tx.getTransactionId());
        return builder.setResult(false).setCode(response_code.DUP_TRANSACTION_ERROR).build();
      } else {
        dbManager.getTransactionIdCache().put(tx.getTransactionId(), true);
      }

      dbManager.pushTransaction(tx);
      ledgerYiNetService.broadcast(message);
      return builder.setResult(true).setCode(response_code.SUCCESS).build();
    } catch (ValidateSignatureException e) {
      log.error("Broadcast transaction {} failed, {}.", tx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.SIGERROR)
          .setMessage(ByteString.copyFromUtf8("validate signature error " + e.getMessage()))
          .build();
    } catch (ContractValidateException e) {
      log.error("Broadcast transaction {} failed, {}.", tx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8("contract validate error : " + e.getMessage()))
          .build();
    } catch (ContractExeException e) {
      log.error("Broadcast transaction {} failed, {}.", tx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.CONTRACT_EXE_ERROR)
          .setMessage(ByteString.copyFromUtf8("contract execute error : " + e.getMessage()))
          .build();
    } catch (AccountResourceInsufficientException e) {
      log.error("Broadcast transaction {} failed, {}.", tx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.BANDWITH_ERROR)
          .setMessage(ByteString.copyFromUtf8("AccountResourceInsufficient error"))
          .build();
    } catch (DupTransactionException e) {
      log.error("Broadcast transaction {} failed, {}.", tx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.DUP_TRANSACTION_ERROR)
          .setMessage(ByteString.copyFromUtf8("dup transaction"))
          .build();
    } catch (TaposException e) {
      log.error("Broadcast transaction {} failed, {}.", tx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.TAPOS_ERROR)
          .setMessage(ByteString.copyFromUtf8("Tapos check error"))
          .build();
    } catch (TooBigTransactionException e) {
      log.error("Broadcast transaction {} failed, {}.", tx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.TOO_BIG_TRANSACTION_ERROR)
          .setMessage(ByteString.copyFromUtf8("transaction size is too big"))
          .build();
    } catch (TransactionExpirationException e) {
      log.error("Broadcast transaction {} failed, {}.", tx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.TRANSACTION_EXPIRATION_ERROR)
          .setMessage(ByteString.copyFromUtf8("transaction expired"))
          .build();
    } catch (Exception e) {
      log.error("Broadcast transaction {} failed, {}.", tx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8("other error : " + e.getMessage()))
          .build();
    }
  }

  public Block getNowBlock() {
    List<BlockCapsule> blockList = dbManager.getBlockStore().getBlockByLatestNum(1);
    if (CollectionUtils.isEmpty(blockList)) {
      return null;
    } else {
      return blockList.get(0).getInstance();
    }
  }

  public Block getBlockByNum(long blockNum) {
    try {
      return dbManager.getBlockByNum(blockNum).getInstance();
    } catch (StoreException e) {
      log.info(e.getMessage());
      return null;
    }
  }

  public long getTransactionCountByBlockNum(long blockNum) {
    long count = 0;
    try {
      Block block = dbManager.getBlockByNum(blockNum).getInstance();
      count = block.getTransactionsCount();
    } catch (StoreException e) {
      log.error(e.getMessage());
    }
    return count;
  }

  public MastersList getMastersList() {
    MastersList.Builder builder = MastersList.newBuilder();
    List<MasterCapsule> masterCapsuleList = dbManager.getMasterStore().getAllMasteres();
    masterCapsuleList.forEach(masterCapsule -> builder.addMasters(masterCapsule.getInstance()));
    return builder.build();
  }

  public Block getBlockById(ByteString blockId) {
    if (Objects.isNull(blockId)) {
      return null;
    }
    Block block = null;
    try {
      block = dbManager.getBlockStore().get(blockId.toByteArray()).getInstance();
    } catch (StoreException e) {
    }
    return block;
  }

  public BlockList getBlocksByLimitNext(long number, long limit) {
    if (limit <= 0) {
      return null;
    }
    BlockList.Builder blockListBuilder = BlockList.newBuilder();
    dbManager.getBlockStore().getLimitNumber(number, limit).forEach(
        blockCapsule -> blockListBuilder.addBlock(blockCapsule.getInstance()));
    return blockListBuilder.build();
  }

  public Transaction getTransactionById(ByteString transactionId) {
    if (Objects.isNull(transactionId)) {
      return null;
    }
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = dbManager.getTransactionStore().get(transactionId.toByteArray());
    } catch (StoreException e) {
      return null;
    }
    if (transactionCapsule != null) {
      return transactionCapsule.getInstance();
    }
    return null;
  }

  public NodeList listNodes() {
    List<NodeHandler> handlerList = nodeManager.dumpActiveNodes();
    Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
    for (NodeHandler handler : handlerList) {
      String key = handler.getNode().getHexId() + handler.getNode().getHost();
      nodeHandlerMap.put(key, handler);
    }
    NodeList.Builder nodeListBuilder = NodeList.newBuilder();
    nodeHandlerMap.entrySet().stream().forEach(v -> {
          cn.ledgeryi.framework.common.overlay.discover.node.Node node = v.getValue().getNode();
          nodeListBuilder.addNodes(Node.newBuilder().setAddress(
              Address.newBuilder().setHost(ByteString.copyFrom(ByteArray.fromString(node.getHost()))).setPort(node.getPort())));
        });
    return nodeListBuilder.build();
  }

}