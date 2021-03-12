package cn.ledgeryi.framework.core.services;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.*;
import cn.ledgeryi.api.WalletGrpc;
import cn.ledgeryi.chainbase.common.utils.DBConfig;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.core.exception.StoreException;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.common.utils.Sha256Hash;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeHandler;
import cn.ledgeryi.framework.common.overlay.discover.node.NodeManager;
import cn.ledgeryi.framework.core.LedgerYi;
import cn.ledgeryi.framework.core.db.Manager;
import cn.ledgeryi.framework.core.permission.aop.Authentication;
import cn.ledgeryi.framework.core.permission.constant.RoleTypeEnum;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.protos.Protocol.Block;
import cn.ledgeryi.protos.Protocol.NodeInfo;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.Protocol.TransactionInfo;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.*;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class LedgerYiApi extends WalletGrpc.WalletImplBase {

    @Autowired
    private LedgerYi ledgerYi;

    @Autowired
    private Manager dbManager;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private NodeInfoService nodeInfoService;

    private static final String CONTRACT_VALIDATE_EXCEPTION = "ContractValidateException: {}";
    private static final String CONTRACT_VALIDATE_ERROR = "contract validate error : ";
    private static final long BLOCK_LIMIT_NUM = 100;

    private GrpcAPI.BlockListExtention blocklistExtention(GrpcAPI.BlockList blockList) {
        if (blockList == null) {
            return null;
        }
        GrpcAPI.BlockListExtention.Builder builder = GrpcAPI.BlockListExtention.newBuilder();
        for (Protocol.Block block : blockList.getBlockList()) {
            builder.addBlock(blockExtention(block));
        }
        return builder.build();
    }

    private GrpcAPI.BlockExtention blockExtention(Protocol.Block block) {
        if (block == null) {
            return null;
        }
        GrpcAPI.BlockExtention.Builder builder = GrpcAPI.BlockExtention.newBuilder();
        BlockCapsule blockCapsule = new BlockCapsule(block);
        builder.setBlockHeader(block.getBlockHeader());
        builder.setBlockid(ByteString.copyFrom(blockCapsule.getBlockId().getBytes()));
        for (int i = 0; i < block.getTransactionsCount(); i++) {
            Protocol.Transaction transaction = block.getTransactions(i);
            builder.addTransactions(transactionExtention(transaction));
        }
        return builder.build();
    }

    private GrpcAPI.TransactionExtention transactionExtention(Protocol.Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        GrpcAPI.TransactionExtention.Builder txExtBuilder = GrpcAPI.TransactionExtention.newBuilder();
        GrpcAPI.Return.Builder retBuilder = GrpcAPI.Return.newBuilder();
        txExtBuilder.setTransaction(transaction);
        txExtBuilder.setTxid(Sha256Hash.of(DBConfig.isEccCryptoEngine(),
                transaction.getRawData().toByteArray()).getByteString());
        retBuilder.setResult(true).setCode(GrpcAPI.Return.response_code.SUCCESS);
        txExtBuilder.setResult(retBuilder);
        return txExtBuilder.build();
    }

    private StatusRuntimeException getRunTimeException(Exception e) {
        if (e != null) {
            return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
        } else {
            return Status.INTERNAL.withDescription("unknown").asRuntimeException();
        }
    }

    private TransactionCapsule createTransactionCapsule(Message message, Transaction.Contract.ContractType contractType)
            throws Exception {
        return ledgerYi.createTransactionCapsule(message, contractType);
    }

    @Authentication(roles = {
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void broadcastTransaction(GrpcRequest req, StreamObserver<GrpcAPI.Return> responseObserver) {
        try{
            Any param = req.getParam();
            Transaction transaction = param.unpack(Transaction.class);
            responseObserver.onNext(ledgerYi.broadcastTransaction(transaction));
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    /*@Override
    public void getAccount(Account req, StreamObserver<Account> responseObserver) {
      ByteString addressBs = req.getAddress();
      if (addressBs != null) {
        Account reply = ledgerYi.getAccount(req);
        responseObserver.onNext(reply);
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }*/

    @Authentication(roles = {
            RoleTypeEnum.READ_ONLY,
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void getNowBlock(GrpcRequest request, StreamObserver<GrpcAPI.BlockExtention> responseObserver) {
        try {
            responseObserver.onNext(blockExtention(ledgerYi.getNowBlock()));
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    @Authentication(roles = {
            RoleTypeEnum.READ_ONLY,
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void getBlockByNum(GrpcRequest request, StreamObserver<GrpcAPI.BlockExtention> responseObserver) {
        try {
            Any requestParam = request.getParam();
            GrpcAPI.NumberMessage numberMessage = requestParam.unpack(GrpcAPI.NumberMessage.class);
            Protocol.Block block = ledgerYi.getBlockByNum(numberMessage.getNum());
            responseObserver.onNext(blockExtention(block));
        } catch (InvalidProtocolBufferException e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    @Authentication(roles = {
            RoleTypeEnum.READ_ONLY,
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void getTransactionCountByBlockNum(GrpcRequest request, StreamObserver<GrpcAPI.NumberMessage> responseObserver) {
        GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
        try {
            Any requestParam = request.getParam();
            GrpcAPI.NumberMessage numberMessage = requestParam.unpack(GrpcAPI.NumberMessage.class);
            Protocol.Block block = dbManager.getBlockByNum(numberMessage.getNum()).getInstance();
            builder.setNum(block.getTransactionsCount());
        } catch (StoreException | InvalidProtocolBufferException e) {
            log.error(e.getMessage());
            builder.setNum(-1);
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Authentication(roles = {
            RoleTypeEnum.READ_ONLY,
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void getNodes(GrpcRequest request, StreamObserver<GrpcAPI.NodeList> responseObserver) {
        try{
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
            responseObserver.onNext(nodeListBuilder.build());
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    @Authentication(roles = {
            RoleTypeEnum.READ_ONLY,
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void getBlockById(GrpcRequest request, StreamObserver<Block> responseObserver) {
        try{
            Any requestParam = request.getParam();
            GrpcAPI.BytesMessage bytesMessage = requestParam.unpack(GrpcAPI.BytesMessage.class);
            ByteString blockId = bytesMessage.getValue();
            if (Objects.nonNull(blockId)) {
                responseObserver.onNext(ledgerYi.getBlockById(blockId));
            }
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    @Authentication(roles = {
            RoleTypeEnum.READ_ONLY,
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void getBlockByLimitNext(GrpcRequest request, StreamObserver<BlockListExtention> responseObserver) {
        try {
            Any requestParam = request.getParam();
            GrpcAPI.BlockLimit blockLimit = requestParam.unpack(GrpcAPI.BlockLimit.class);
            long startNum = blockLimit.getStartNum();
            long endNum = blockLimit.getEndNum();
            if (endNum > 0 && endNum > startNum && endNum - startNum <= BLOCK_LIMIT_NUM) {
                responseObserver.onNext(blocklistExtention(ledgerYi.getBlocksByLimitNext(startNum, endNum - startNum)));
            }
        } catch (Exception e){
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    @Authentication(roles = {
            RoleTypeEnum.READ_ONLY,
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void getTransactionById(GrpcRequest request, StreamObserver<Transaction> responseObserver) {
        try{
            Any requestParam = request.getParam();
            GrpcAPI.BytesMessage bytesMessage = requestParam.unpack(GrpcAPI.BytesMessage.class);
            ByteString transactionId = bytesMessage.getValue();
            if (Objects.nonNull(transactionId)) {
                responseObserver.onNext(ledgerYi.getTransactionById(transactionId));
            }
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    @Authentication(roles = {
            RoleTypeEnum.READ_ONLY,
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void getTransactionInfoById(GrpcRequest request, StreamObserver<TransactionInfo> responseObserver) {
        try{
            Any requestParam = request.getParam();
            GrpcAPI.BytesMessage bytesMessage = requestParam.unpack(GrpcAPI.BytesMessage.class);
            ByteString id = bytesMessage.getValue();
            if (null != id) {
                TransactionInfo reply = ledgerYi.getTransactionInfoById(id);
                responseObserver.onNext(reply);
            }
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    @Authentication(roles = {
            RoleTypeEnum.READ_ONLY,
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void getMasters(GrpcRequest request, StreamObserver<MastersList> responseObserver) {
        try{
            responseObserver.onNext(ledgerYi.getMastersList());
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    @Authentication(roles = {
            RoleTypeEnum.READ_ONLY,
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void getNodeInfo(GrpcRequest request, StreamObserver<NodeInfo> responseObserver) {
        try {
            responseObserver.onNext(nodeInfoService.getNodeInfo().transferToProtoEntity());
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    @Authentication(roles = {
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void deployContract(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        CreateSmartContract createSmartContract = null;
        try{
            Any requestParam = request.getParam();
            createSmartContract = requestParam.unpack(CreateSmartContract.class);
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
            responseObserver.onCompleted();
        }
        createTransaction(createSmartContract, Transaction.Contract.ContractType.CreateSmartContract, responseObserver);
    }

    private void createTransaction(Message request, Transaction.Contract.ContractType contractType, StreamObserver<TransactionExtention> responseObserver) {
        TransactionExtention.Builder txExtBuilder = TransactionExtention.newBuilder();
        Return.Builder retBuilder = Return.newBuilder();
        try {
            TransactionCapsule tx = createTransactionCapsule(request, contractType);
            txExtBuilder.setTransaction(tx.getInstance());
            txExtBuilder.setTxid(tx.getTransactionId().getByteString());
            retBuilder.setResult(true).setCode(Return.response_code.SUCCESS);
        } catch (ContractValidateException e) {
            retBuilder.setResult(false).setCode(Return.response_code.CONTRACT_VALIDATE_ERROR)
                    .setMessage(ByteString.copyFromUtf8(CONTRACT_VALIDATE_ERROR + e.getMessage()));
            log.debug(CONTRACT_VALIDATE_EXCEPTION, e.getMessage());
        } catch (Exception e) {
            retBuilder.setResult(false).setCode(Return.response_code.OTHER_ERROR)
                    .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
            log.info("exception caught" + e.getMessage());
        }
        txExtBuilder.setResult(retBuilder);
        responseObserver.onNext(txExtBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void clearContractABI(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        //createTransaction(request, ContractType.ClearABIContract, responseObserver);
    }

    @Authentication(roles = {
            RoleTypeEnum.READ_ONLY,
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void getContract(GrpcRequest request, StreamObserver<SmartContract> responseObserver) {
        try{
            Any requestParam = request.getParam();
            GrpcAPI.BytesMessage bytesMessage = requestParam.unpack(GrpcAPI.BytesMessage.class);
            SmartContract contract = ledgerYi.getContract(bytesMessage);
            responseObserver.onNext(contract);
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    @Authentication(roles = {
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void triggerContract(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        callContract(request, responseObserver, false);
    }

    @Authentication(roles = {
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void triggerConstantContract(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        callContract(request, responseObserver, true);
    }

    private void callContract(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver, boolean isConstant) {
        TransactionExtention.Builder txExtBuilder = TransactionExtention.newBuilder();
        Return.Builder retBuilder = Return.newBuilder();
        try {
            Any param = request.getParam();
            TriggerSmartContract triggerSmartContract = param.unpack(TriggerSmartContract.class);
            TransactionCapsule txCap = createTransactionCapsule(triggerSmartContract, Transaction.Contract.ContractType.TriggerSmartContract);
            Transaction tx;
            if (isConstant) {
                tx = ledgerYi.triggerConstantContract(triggerSmartContract, txCap, txExtBuilder, retBuilder);
            } else {
                tx = ledgerYi.triggerContract(triggerSmartContract, txCap, txExtBuilder, retBuilder);
            }
            txExtBuilder.setTransaction(tx);
            txExtBuilder.setTxid(txCap.getTransactionId().getByteString());
            retBuilder.setResult(true).setCode(Return.response_code.SUCCESS);
            txExtBuilder.setResult(retBuilder);
        } catch (ContractValidateException e) {
            retBuilder.setResult(false).setCode(Return.response_code.CONTRACT_VALIDATE_ERROR)
                    .setMessage(ByteString.copyFromUtf8(CONTRACT_VALIDATE_ERROR + e.getMessage()));
            txExtBuilder.setResult(retBuilder);
            log.warn(CONTRACT_VALIDATE_EXCEPTION, e.getMessage());
        } catch (RuntimeException e) {
            retBuilder.setResult(false).setCode(Return.response_code.CONTRACT_EXE_ERROR)
                    .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
            txExtBuilder.setResult(retBuilder);
            log.warn("When run constant call in VM, have RuntimeException: " + e.getMessage());
        } catch (Exception e) {
            retBuilder.setResult(false).setCode(Return.response_code.OTHER_ERROR)
                    .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
            txExtBuilder.setResult(retBuilder);
            log.warn("unknown exception caught: " + e.getMessage(), e);
        } finally {
            responseObserver.onNext(txExtBuilder.build());
            responseObserver.onCompleted();
        }
    }
}