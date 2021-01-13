package cn.ledgeryi.framework.core;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.*;
import cn.ledgeryi.api.GrpcAPI.Return.response_code;
import cn.ledgeryi.chainbase.actuator.Actuator;
import cn.ledgeryi.chainbase.common.message.Message;
import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.common.utils.ContractUtils;
import cn.ledgeryi.chainbase.core.capsule.*;
import cn.ledgeryi.chainbase.core.db.TransactionContext;
import cn.ledgeryi.chainbase.core.store.AccountStore;
import cn.ledgeryi.chainbase.core.store.ContractStore;
import cn.ledgeryi.chainbase.core.store.StoreFactory;
import cn.ledgeryi.common.core.exception.*;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.contract.vm.LedgerYiVmActuator;
import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignUtils;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeHandler;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;
import cn.ledgeryi.framework.common.utils.Utils;
import cn.ledgeryi.framework.core.actuator.ActuatorFactory;
import cn.ledgeryi.framework.core.capsule.TransactionInfoCapsule;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.framework.core.exception.*;
import cn.ledgeryi.framework.core.net.LedgerYiNetDelegate;
import cn.ledgeryi.framework.core.net.LedgerYiNetService;
import cn.ledgeryi.framework.core.net.message.TransactionMessage;
import cn.ledgeryi.protos.Protocol.Account;
import cn.ledgeryi.protos.Protocol.Block;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;
import cn.ledgeryi.protos.Protocol.Transaction.Result;
import cn.ledgeryi.protos.Protocol.TransactionInfo;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.TriggerSmartContract;
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
    this.cryptoEngine = SignUtils.getGeneratedRandomSign(Utils.getRandom(), Args.getInstance().isECKeyCryptoEngine());
  }


  public Wallet(final SignInterface cryptoEngine) {
    this.cryptoEngine = cryptoEngine;
    log.info("wallet address: {}", ByteArray.toHexString(this.cryptoEngine.getAddress()));
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
    return accountCapsule.getInstance();
  }

  public TransactionCapsule createTransactionCapsule(com.google.protobuf.Message message, ContractType contractType)
          throws ContractValidateException {
    TransactionCapsule tx = new TransactionCapsule(message, contractType);
    if (contractType != ContractType.CreateSmartContract && contractType != ContractType.TriggerSmartContract) {
      // for ContractType.ClearABIContract
      List<Actuator> actList = ActuatorFactory.createActuator(tx, dbManager);
      for (Actuator act : actList) {
        act.validate();
      }
    }
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

  public TransactionInfo getTransactionInfoById(ByteString transactionId) {
    if (Objects.isNull(transactionId)) {
      return null;
    }
    TransactionInfoCapsule transactionInfoCapsule;
    try {
      transactionInfoCapsule = dbManager.getTransactionHistoryStore().get(transactionId.toByteArray());
    } catch (StoreException e) {
      return null;
    }
    if (transactionInfoCapsule != null) {
      return transactionInfoCapsule.getInstance();
    }
    try {
      transactionInfoCapsule = dbManager.getTransactionRetStore().getTransactionInfo(transactionId.toByteArray());
    } catch (BadItemException e) {
      return null;
    }
    return transactionInfoCapsule == null ? null : transactionInfoCapsule.getInstance();
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

  public Transaction triggerConstantContract(TriggerSmartContract triggerSmartContract,
                                             TransactionCapsule trxCap, TransactionExtention.Builder builder, Return.Builder retBuilder)
          throws ContractValidateException, HeaderNotFound {
    ContractStore contractStore = dbManager.getContractStore();
    byte[] contractAddress = triggerSmartContract.getContractAddress().toByteArray();
    //contractStore.listContract();
    byte[] isContractExist = contractStore.findContractByHash(contractAddress);
    if (ArrayUtils.isEmpty(isContractExist)) {
      throw new ContractValidateException("No contract or not a smart contract");
    }
    return callConstantContract(trxCap, builder, retBuilder);
  }

  public Transaction callConstantContract(TransactionCapsule trxCap, TransactionExtention.Builder builder, Return.Builder retBuilder)
          throws ContractValidateException, HeaderNotFound {
    Block headBlock;
    List<BlockCapsule> blockCapsuleList = dbManager.getBlockStore().getBlockByLatestNum(1);
    if (CollectionUtils.isEmpty(blockCapsuleList)) {
      throw new HeaderNotFound("latest block not found");
    } else {
      headBlock = blockCapsuleList.get(0).getInstance();
    }

    TransactionContext context = new TransactionContext(new BlockCapsule(headBlock),
            trxCap, StoreFactory.getInstance(), true);

    LedgerYiVmActuator ledgerYiVmActuator = new LedgerYiVmActuator(true);
    ledgerYiVmActuator.validate(context);
    ledgerYiVmActuator.execute(context);

    ProgramResult result = context.getProgramResult();
    if (result.getException() != null) {
      RuntimeException e = result.getException();
      log.warn("Constant call has error {}", e.getMessage());
      throw e;
    }
    TransactionResultCapsule ret = new TransactionResultCapsule();
    builder.addConstantResult(ByteString.copyFrom(result.getHReturn()));
    ret.setStatus(Result.code.SUCESS);
    if (StringUtils.isNoneEmpty(result.getRuntimeError())) {
      ret.setStatus(Result.code.FAILED);
      retBuilder.setMessage(ByteString.copyFromUtf8(result.getRuntimeError())).build();
    }
    if (result.isRevert()) {
      ret.setStatus(Result.code.FAILED);
      retBuilder.setMessage(ByteString.copyFromUtf8("REVERT opcode executed")).build();
    }
    trxCap.setResult(ret);
    return trxCap.getInstance();
  }

  public SmartContract getContract(GrpcAPI.BytesMessage bytesMessage) {
    byte[] address = bytesMessage.getValue().toByteArray();
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(address);
    if (accountCapsule == null) {
      log.error("Get contract failed, the account does not exist or the account does not have a code hash!");
      return null;
    }
    ContractCapsule contractCapsule = dbManager.getContractStore().get(bytesMessage.getValue().toByteArray());
    if (Objects.nonNull(contractCapsule)) {
      return contractCapsule.getInstance();
    }
    return null;
  }

  public Transaction triggerContract(TriggerSmartContract triggerSmartContract,
                                     TransactionCapsule trxCap, TransactionExtention.Builder builder,
                                     Return.Builder retBuilder)
          throws ContractValidateException, HeaderNotFound {
    ContractStore contractStore = dbManager.getContractStore();
    byte[] contractAddress = triggerSmartContract.getContractAddress().toByteArray();
    SmartContract.ABI abi = contractStore.getABI(contractAddress);
    if (abi == null || abi.getEntrysList().isEmpty()) {
      throw new ContractValidateException("No contract or not a valid smart contract");
    }
    byte[] selector = getSelector(triggerSmartContract.getData().toByteArray());
    if (ContractUtils.isConstant(abi, selector)) {
      return callConstantContract(trxCap, builder, retBuilder);
    } else {
      return trxCap.getInstance();
    }
  }

  private static byte[] getSelector(byte[] data) {
    if (data == null || data.length < 4) {
      return null;
    }
    byte[] ret = new byte[4];
    System.arraycopy(data, 0, ret, 0, 4);
    return ret;
  }
}