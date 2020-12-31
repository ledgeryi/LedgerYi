package cn.ledgeryi.framework.core.services;

import cn.ledgeryi.api.DatabaseGrpc;
import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.*;
import cn.ledgeryi.api.GrpcAPI.Return.response_code;
import cn.ledgeryi.api.WalletGrpc;
import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.capsule.*;
import cn.ledgeryi.chainbase.core.db.TransactionContext;
import cn.ledgeryi.chainbase.core.store.ContractStore;
import cn.ledgeryi.chainbase.core.store.StoreFactory;
import cn.ledgeryi.common.core.exception.ContractExeException;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.core.exception.StoreException;
import cn.ledgeryi.common.core.exception.VMIllegalException;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.contract.vm.VMActuator;
import cn.ledgeryi.framework.common.application.Service;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeHandler;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;
import cn.ledgeryi.framework.common.storage.DepositImpl;
import cn.ledgeryi.framework.core.Wallet;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.framework.core.exception.HeaderNotFound;
import cn.ledgeryi.framework.core.services.ratelimiter.RateLimiterInterceptor;
import cn.ledgeryi.protos.Protocol.*;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;
import cn.ledgeryi.protos.contract.InnerContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j(topic = "API")
public class RpcApiService implements Service {

  private static final String CONTRACT_VALIDATE_EXCEPTION = "ContractValidateException: {}";
  private static final String CONTRACT_VALIDATE_ERROR = "contract validate error : ";
  private static final long BLOCK_LIMIT_NUM = 100;
  private int port = Args.getInstance().getRpcPort();
  private Server apiServer;
  @Autowired
  private Manager dbManager;
  @Autowired
  private NodeManager nodeManager;
  @Autowired
  private Wallet wallet;
  @Autowired
  private NodeInfoService nodeInfoService;
  @Autowired
  private RateLimiterInterceptor rateLimiterInterceptor;
  @Getter
  private DatabaseApi databaseApi = new DatabaseApi();
  private WalletApi walletApi = new WalletApi();

  @Override
  public void init() {

  }

  @Override
  public void init(Args args) {
  }

  @Override
  public void start() {
    try {
      NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(port).addService(databaseApi);
      Args args = Args.getInstance();

      if (args.getRpcThreadNum() > 0) {
        serverBuilder = serverBuilder.executor(Executors.newFixedThreadPool(args.getRpcThreadNum()));
      }

      serverBuilder = serverBuilder.addService(walletApi);

      // Set configs from config.conf or default value
      serverBuilder
          .maxConcurrentCallsPerConnection(args.getMaxConcurrentCallsPerConnection())
          .flowControlWindow(args.getFlowControlWindow())
          .maxConnectionIdle(args.getMaxConnectionIdleInMillis(), TimeUnit.MILLISECONDS)
          .maxConnectionAge(args.getMaxConnectionAgeInMillis(), TimeUnit.MILLISECONDS)
          .maxMessageSize(args.getMaxMessageSize())
          .maxHeaderListSize(args.getMaxHeaderListSize());

      // add a ratelimiter interceptor
      serverBuilder.intercept(rateLimiterInterceptor);

      apiServer = serverBuilder.build();
      rateLimiterInterceptor.init(apiServer);

      apiServer.start();
    } catch (IOException e) {
      log.debug(e.getMessage(), e);
    }

    log.info("RpcApiService started, listening on " + port);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.err.println("*** shutting down gRPC server since JVM is shutting down");
      //server.this.stop();
      System.err.println("*** server shut down");
    }));
  }

  private TransactionCapsule createTransactionCapsule(com.google.protobuf.Message message, ContractType contractType) throws ContractValidateException {
    return wallet.createTransactionCapsule(message, contractType);
  }

  private TransactionExtention transaction2Extention(Transaction transaction) {
    if (transaction == null) {
      return null;
    }
    TransactionExtention.Builder txExtBuilder = TransactionExtention.newBuilder();
    Return.Builder retBuilder = Return.newBuilder();
    txExtBuilder.setTransaction(transaction);
    txExtBuilder.setTxid(Sha256Hash.of(DBConfig.isECKeyCryptoEngine(),
        transaction.getRawData().toByteArray()).getByteString());
    retBuilder.setResult(true).setCode(response_code.SUCCESS);
    txExtBuilder.setResult(retBuilder);
    return txExtBuilder.build();
  }

  private BlockExtention block2Extention(Block block) {
    if (block == null) {
      return null;
    }
    BlockExtention.Builder builder = BlockExtention.newBuilder();
    BlockCapsule blockCapsule = new BlockCapsule(block);
    builder.setBlockHeader(block.getBlockHeader());
    builder.setBlockid(ByteString.copyFrom(blockCapsule.getBlockId().getBytes()));
    for (int i = 0; i < block.getTransactionsCount(); i++) {
      Transaction transaction = block.getTransactions(i);
      builder.addTransactions(transaction2Extention(transaction));
    }
    return builder.build();
  }

  private StatusRuntimeException getRunTimeException(Exception e) {
    if (e != null) {
      return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
    } else {
      return Status.INTERNAL.withDescription("unknown").asRuntimeException();
    }
  }

  @Override
  public void stop() {
    if (apiServer != null) {
      apiServer.shutdown();
    }
  }

  public void blockUntilShutdown() {
    if (apiServer != null) {
      try {
        apiServer.awaitTermination();
      } catch (InterruptedException e) {
        log.warn("{}", e);
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * DatabaseApi.
   */
  public class DatabaseApi extends DatabaseGrpc.DatabaseImplBase {

    @Override
    public void getBlockReference(EmptyMessage request, StreamObserver<cn.ledgeryi.api.GrpcAPI.BlockReference> responseObserver) {
      long headBlockNum = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
      byte[] blockHeaderHash = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes();
      BlockReference ref = BlockReference.newBuilder().setBlockHash(ByteString.copyFrom(blockHeaderHash)).setBlockNum(headBlockNum).build();
      responseObserver.onNext(ref);
      responseObserver.onCompleted();
    }

    @Override
    public void getNowBlock(EmptyMessage request, StreamObserver<Block> responseObserver) {
      Block block = null;
      try {
        block = dbManager.getHead().getInstance();
      } catch (StoreException e) {
        log.error(e.getMessage());
      }
      responseObserver.onNext(block);
      responseObserver.onCompleted();
    }

    @Override
    public void getBlockByNum(NumberMessage request, StreamObserver<Block> responseObserver) {
      Block block = null;
      try {
        block = dbManager.getBlockByNum(request.getNum()).getInstance();
      } catch (StoreException e) {
        log.error(e.getMessage());
      }
      responseObserver.onNext(block);
      responseObserver.onCompleted();
    }

    @Override
    public void getDynamicProperties(EmptyMessage request, StreamObserver<DynamicProperties> responseObserver) {
      DynamicProperties.Builder builder = DynamicProperties.newBuilder();
      builder.setLastSolidityBlockNum(dbManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum());
      DynamicProperties dynamicProperties = builder.build();
      responseObserver.onNext(dynamicProperties);
      responseObserver.onCompleted();
    }
  }

  public class WalletApi extends WalletGrpc.WalletImplBase {

    private BlockListExtention blocklist2Extention(BlockList blockList) {
      if (blockList == null) {
        return null;
      }
      BlockListExtention.Builder builder = BlockListExtention.newBuilder();
      for (Block block : blockList.getBlockList()) {
        builder.addBlock(block2Extention(block));
      }
      return builder.build();
    }

    @Override
    public void createTransaction(InnerContract.SystemContract systemContract, StreamObserver<TransactionExtention> responseObserver) {
      ContractType contractType = systemContract.getType();
      Any contractAny = systemContract.getContract();
      TransactionExtention.Builder txExtBuilder = TransactionExtention.newBuilder();
      Return.Builder retBuilder = Return.newBuilder();
      try {
        TransactionCapsule tx = createTransactionCapsule(contractAny, contractType);
        txExtBuilder.setTransaction(tx.getInstance());
        txExtBuilder.setTxid(tx.getTransactionId().getByteString());
        retBuilder.setResult(true).setCode(response_code.SUCCESS);
      } catch (ContractValidateException e) {
        retBuilder.setResult(false)
                .setCode(response_code.CONTRACT_VALIDATE_ERROR)
                .setMessage(ByteString.copyFromUtf8(CONTRACT_VALIDATE_ERROR + e.getMessage()));
        log.debug(CONTRACT_VALIDATE_EXCEPTION, e.getMessage());
      } catch (Exception e) {
        retBuilder.setResult(false)
                .setCode(response_code.OTHER_ERROR)
                .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
        log.info("exception caught" + e.getMessage());
      }
      txExtBuilder.setResult(retBuilder);
      responseObserver.onNext(txExtBuilder.build());
      responseObserver.onCompleted();
    }

    @Override
    public void broadcastTransaction(Transaction req, StreamObserver<GrpcAPI.Return> responseObserver) {
      responseObserver.onNext(wallet.broadcastTransaction(req));
      responseObserver.onCompleted();
    }

    @Override
    public void getAccount(Account req, StreamObserver<Account> responseObserver) {
      ByteString addressBs = req.getAddress();
      if (addressBs != null) {
        Account reply = wallet.getAccount(req);
        responseObserver.onNext(reply);
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }

    @Override
    public void getNowBlock(EmptyMessage request, StreamObserver<BlockExtention> responseObserver) {
      responseObserver.onNext(block2Extention(wallet.getNowBlock()));
      responseObserver.onCompleted();
    }

    @Override
    public void getBlockByNum(NumberMessage request, StreamObserver<BlockExtention> responseObserver) {
      Block block = wallet.getBlockByNum(request.getNum());
      responseObserver.onNext(block2Extention(block));
      responseObserver.onCompleted();
    }

    @Override
    public void getTransactionCountByBlockNum(NumberMessage request, StreamObserver<NumberMessage> responseObserver) {
      NumberMessage.Builder builder = NumberMessage.newBuilder();
      try {
        Block block = dbManager.getBlockByNum(request.getNum()).getInstance();
        builder.setNum(block.getTransactionsCount());
      } catch (StoreException e) {
        log.error(e.getMessage());
        builder.setNum(-1);
      }
      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
    }

    @Override
    public void getNodes(EmptyMessage request, StreamObserver<NodeList> responseObserver) {
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
                Address.newBuilder().setHost(ByteString.copyFrom(ByteArray.fromString(node.getHost())))
                    .setPort(node.getPort())));
          });

      responseObserver.onNext(nodeListBuilder.build());
      responseObserver.onCompleted();
    }

    @Override
    public void getBlockById(BytesMessage request, StreamObserver<Block> responseObserver) {
      ByteString blockId = request.getValue();
      if (Objects.nonNull(blockId)) {
        responseObserver.onNext(wallet.getBlockById(blockId));
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }

    @Override
    public void getBlockByLimitNext(BlockLimit request, StreamObserver<BlockListExtention> responseObserver) {
      long startNum = request.getStartNum();
      long endNum = request.getEndNum();

      if (endNum > 0 && endNum > startNum && endNum - startNum <= BLOCK_LIMIT_NUM) {
        responseObserver.onNext(blocklist2Extention(wallet.getBlocksByLimitNext(startNum, endNum - startNum)));
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }

    @Override
    public void getTransactionById(BytesMessage request, StreamObserver<Transaction> responseObserver) {
      ByteString transactionId = request.getValue();
      if (Objects.nonNull(transactionId)) {
        responseObserver.onNext(wallet.getTransactionById(transactionId));
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }

    @Override
    public void getMasters(EmptyMessage request, StreamObserver<MastersList> responseObserver) {
      responseObserver.onNext(wallet.getMastersList());
      responseObserver.onCompleted();
    }

    @Override
    public void getNodeInfo(EmptyMessage request, StreamObserver<NodeInfo> responseObserver) {
      try {
        responseObserver.onNext(nodeInfoService.getNodeInfo().transferToProtoEntity());
      } catch (Exception e) {
        responseObserver.onError(getRunTimeException(e));
      }
      responseObserver.onCompleted();
    }

    @Override
    public void deployContract(SmartContractOuterClass.CreateSmartContract request,
                               io.grpc.stub.StreamObserver<TransactionExtention> responseObserver) {
      createTransactionExtention(request, ContractType.CreateSmartContract, responseObserver);
    }

    private void createTransactionExtention(Message request, ContractType contractType,
                                            StreamObserver<TransactionExtention> responseObserver) {
      TransactionExtention.Builder txExtBuilder = TransactionExtention.newBuilder();
      Return.Builder retBuilder = Return.newBuilder();
      try {
        TransactionCapsule tx = createTransactionCapsule(request, contractType);
        txExtBuilder.setTransaction(tx.getInstance());
        txExtBuilder.setTxid(tx.getTransactionId().getByteString());
        retBuilder.setResult(true).setCode(response_code.SUCCESS);
      } catch (ContractValidateException e) {
        retBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
                .setMessage(ByteString.copyFromUtf8(CONTRACT_VALIDATE_ERROR + e.getMessage()));
        log.debug(CONTRACT_VALIDATE_EXCEPTION, e.getMessage());
      } catch (Exception e) {
        retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
                .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
        log.info("exception caught" + e.getMessage());
      }
      txExtBuilder.setResult(retBuilder);
      responseObserver.onNext(txExtBuilder.build());
      responseObserver.onCompleted();
    }

    @Override
    public void clearContractABI(SmartContractOuterClass.ClearABIContract request,
                                 StreamObserver<TransactionExtention> responseObserver) {
      createTransactionExtention(request, ContractType.ClearABIContract, responseObserver);
    }

    @Override
    public void getContract(BytesMessage request, StreamObserver<SmartContractOuterClass.SmartContract> responseObserver) {
      SmartContractOuterClass.SmartContract contract = wallet.getContract(request);
      responseObserver.onNext(contract);
      responseObserver.onCompleted();
    }

    @Override
    public void triggerContract(SmartContractOuterClass.TriggerSmartContract request,
                                StreamObserver<TransactionExtention> responseObserver) {
      callContract(request, responseObserver, false);
    }

    @Override
    public void triggerConstantContract(SmartContractOuterClass.TriggerSmartContract request,
                                        StreamObserver<TransactionExtention> responseObserver) {
      callContract(request, responseObserver, true);
    }


    private void callContract(SmartContractOuterClass.TriggerSmartContract request,
                              StreamObserver<TransactionExtention> responseObserver, boolean isConstant) {
      TransactionExtention.Builder txExtBuilder = TransactionExtention.newBuilder();
      Return.Builder retBuilder = Return.newBuilder();
      try {
        TransactionCapsule txCap = createTransactionCapsule(request, ContractType.TriggerSmartContract);
        Transaction tx;
        if (isConstant) {
          tx = wallet.triggerConstantContract(request, txCap, txExtBuilder, retBuilder);
        } else {
          tx = wallet.triggerContract(request, txCap, txExtBuilder, retBuilder);
        }
        txExtBuilder.setTransaction(tx);
        txExtBuilder.setTxid(txCap.getTransactionId().getByteString());
        retBuilder.setResult(true).setCode(response_code.SUCCESS);
        txExtBuilder.setResult(retBuilder);
      } catch (ContractValidateException | VMIllegalException e) {
        retBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
                .setMessage(ByteString.copyFromUtf8(CONTRACT_VALIDATE_ERROR + e.getMessage()));
        txExtBuilder.setResult(retBuilder);
        log.warn(CONTRACT_VALIDATE_EXCEPTION, e.getMessage());
      } catch (RuntimeException e) {
        retBuilder.setResult(false).setCode(response_code.CONTRACT_EXE_ERROR)
                .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
        txExtBuilder.setResult(retBuilder);
        log.warn("When run constant call in VM, have RuntimeException: " + e.getMessage());
      } catch (Exception e) {
        retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
                .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
        txExtBuilder.setResult(retBuilder);
        log.warn("unknown exception caught: " + e.getMessage(), e);
      } finally {
        responseObserver.onNext(txExtBuilder.build());
        responseObserver.onCompleted();
      }
    }


  }
}
