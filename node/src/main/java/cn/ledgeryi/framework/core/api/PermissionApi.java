package cn.ledgeryi.framework.core.api;

import cn.ledgeryi.api.GrpcAPI.*;
import cn.ledgeryi.api.GrpcAPI.Return.response_code;
import cn.ledgeryi.api.PermissionGrpc;
import cn.ledgeryi.chainbase.core.capsule.TransactionCapsule;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.framework.common.utils.AbiUtil;
import cn.ledgeryi.framework.core.LedgerYi;
import cn.ledgeryi.framework.core.exception.AuthorizeException;
import cn.ledgeryi.framework.core.permission.PermissionService;
import cn.ledgeryi.protos.Protocol.Transaction;
import cn.ledgeryi.protos.Protocol.Transaction.Contract.ContractType;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.CreateSmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.Strings;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PermissionApi extends PermissionGrpc.PermissionImplBase {

    @Autowired
    private LedgerYi ledgerYi;

    @Autowired
    private PermissionService permissionService;

    @Override
    public void disableRole(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "disableRole(uint32)";
        callContract(permissionService.getPermissionMgrAddress(), method, false, request, responseObserver);
    }

    @Override
    public void getRole(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "getRole(uint256)";
        callContract(permissionService.getPermissionMgrAddress(), method, true, request, responseObserver);
    }

    @Override
    public void enableRole(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "enableRole(uint32)";
        callContract(permissionService.getPermissionMgrAddress(), method, false, request, responseObserver);
    }

    @Override
    public void assignRoleForUser(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "assignRoleForUser(address,uint8)";
        callContract(permissionService.getPermissionMgrAddress(), method, false, request, responseObserver);
    }

    @Override
    public void reassignRoleForUser(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "reassignRoleForUser(address,uint32)";
        callContract(permissionService.getPermissionMgrAddress(), method, false, request, responseObserver);
    }

    @Override
    public void removeUser(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "removeUser(address)";
        callContract(permissionService.getPermissionMgrAddress(), method, true, request, responseObserver);
    }

    @Override
    public void hasRole(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "hasRole(address,uint8)";
        callContract(permissionService.getPermissionMgrAddress(), method, true, request, responseObserver);
    }

    @Override
    public void queryUserInfo(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "queryUserInfo(address)";
        callContract(permissionService.getPermissionMgrAddress(), method, true, request, responseObserver);
    }

    @Override
    public void queryAllUserInfo(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "queryAllUserInfo(uint32,uint8)";
        callContract(permissionService.getPermissionMgrAddress(), method, true, request, responseObserver);
    }

    @Override
    public void queryAllUserInfoByPage(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "queryAllUserInfoByPage()";
        callContract(permissionService.getPermissionMgrAddress(), method, true, request, responseObserver);
    }

    @Override
    public void init(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "init(address)";
        callContract(permissionService.getNodeMgrAddress(), method, false, request, responseObserver);
        callContract(permissionService.getAdminMgrAddress(), method, false, request, responseObserver);
    }

    @Override
    public void addDefaultAdmin(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "addAdmin(address)";
        callContract(permissionService.getAdminStorageAddress(), method, false, request, responseObserver);
    }

    @Override
    public void queryNodeInfo(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        String method = "queryNodeInfo(address)";
        callContract(permissionService.getNodeMgrAddress(), method, true, request, responseObserver);
    }

    @Override
    public void deployContract(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        createContract(request, responseObserver);
    }

    @Override
    public void getContract(GrpcRequest request, StreamObserver<SmartContract> responseObserver) {
        try {
            Any requestParam = request.getParam();
            ContractCallParam contractCallParam = requestParam.unpack(ContractCallParam.class);
            byte[] caller = contractCallParam.getCallerBytes().toByteArray();

            //check caller is guardian account
            String guardianAccount = permissionService.getGuardianAccount();
            if (!Strings.fromByteArray(caller).equals(guardianAccount)) {
                throw new AuthorizeException("getContract is called without permission");
            }
            String contractAddress = contractCallParam.getArgs(0);
            ByteString contractAddressByteString = ByteString.copyFrom(DecodeUtil.decode(contractAddress));
            BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(contractAddressByteString).build();
            SmartContract contract = ledgerYi.getContract(bytesMessage);
            responseObserver.onNext(contract);
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastTransaction(GrpcRequest request, StreamObserver<Return> responseObserver) {
        try {
            Any param = request.getParam();
            Transaction transaction = param.unpack(Transaction.class);
            responseObserver.onNext(ledgerYi.broadcastTransaction(transaction));
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    private void callContract(String contactAddress,
                              String method,
                              boolean isConstant,
                              GrpcRequest request,
                              StreamObserver<TransactionExtention> responseObserver) {
        Any requestParam = request.getParam();
        try {
            ContractCallParam callParam = requestParam.unpack(ContractCallParam.class);
            byte[] caller = callParam.getCallerBytes().toByteArray();

            //check caller is guardian account
            String guardianAccount = permissionService.getGuardianAccount();
            if (!Strings.fromByteArray(caller).equals(guardianAccount)) {
                throw new AuthorizeException("method " + method + "is called without permission");
            }

            byte[] selector = Hex.decode(AbiUtil.parseMethod(method, parseArgs(callParam)));
            TriggerSmartContract param = triggerSmartContract(DecodeUtil.decode(Strings.fromByteArray(caller)),
                    DecodeUtil.decode(contactAddress), selector);
            request = request.toBuilder().setParam(Any.pack(param)).build();
            triggerContract(request, isConstant, responseObserver);
        } catch (InvalidProtocolBufferException | AuthorizeException e) {
            responseObserver.onError(e);
            responseObserver.onCompleted();
        }
    }

    /**
     * trigger contract
     */
    private void triggerContract(GrpcRequest request,
                                 boolean isConstant,
                                 StreamObserver<TransactionExtention> responseObserver) {
        createTransaction(request, isConstant, ContractType.TriggerSmartContract, responseObserver);
    }

    /**
     * create contract
     */
    private void createContract(GrpcRequest request,
                                StreamObserver<TransactionExtention> responseObserver) {
        TransactionExtention.Builder txExtBuilder = TransactionExtention.newBuilder();
        Return.Builder retBuilder = Return.newBuilder();
        Any param = request.getParam();
        try {
            CreateSmartContract createSmartContract = param.unpack(CreateSmartContract.class);
            TransactionCapsule txCap = ledgerYi.createTransactionCapsule(createSmartContract, ContractType.CreateSmartContract);
            txExtBuilder.setTransaction(txCap.getInstance());
            txExtBuilder.setTxid(txCap.getTransactionId().getByteString());
            retBuilder.setResult(true).setCode(Return.response_code.SUCCESS);
        } catch (ContractValidateException e) {
            retBuilder.setResult(false)
                    .setCode(Return.response_code.CONTRACT_VALIDATE_ERROR)
                    .setMessage(ByteString.copyFromUtf8("contract validate error : " + e.getMessage()));
            log.debug("ContractValidateException: {}", e.getMessage());
        } catch (Exception e) {
            retBuilder.setResult(false)
                    .setCode(Return.response_code.OTHER_ERROR)
                    .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
            log.info("exception caught" + e.getMessage());
        }
        txExtBuilder.setResult(retBuilder);
        responseObserver.onNext(txExtBuilder.build());
        responseObserver.onCompleted();
    }

    /**
     * create tx: trigger/create contract
     */
    private void createTransaction(GrpcRequest request,
                                   boolean isConstant,
                                   ContractType contractType,
                                   StreamObserver<TransactionExtention> responseObserver) {
        TransactionExtention.Builder txExtBuilder = TransactionExtention.newBuilder();
        Return.Builder retBuilder = Return.newBuilder();
        try {
            Any param = request.getParam();
            TriggerSmartContract triggerSmartContract = param.unpack(TriggerSmartContract.class);
            TransactionCapsule txCap = ledgerYi.createTransactionCapsule(triggerSmartContract, contractType);
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
            retBuilder.setResult(false)
                    .setCode(response_code.CONTRACT_VALIDATE_ERROR)
                    .setMessage(ByteString.copyFromUtf8("contract validate error : " + e.getMessage()));
            txExtBuilder.setResult(retBuilder);
            log.warn("ContractValidateException: {}", e.getMessage());
        } catch (RuntimeException e) {
            retBuilder.setResult(false)
                    .setCode(Return.response_code.CONTRACT_EXE_ERROR)
                    .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
            txExtBuilder.setResult(retBuilder);
            log.warn("When run constant call in VM, have RuntimeException: " + e.getMessage());
        } catch (Exception e) {
            retBuilder.setResult(false)
                    .setCode(Return.response_code.OTHER_ERROR)
                    .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
            txExtBuilder.setResult(retBuilder);
            log.warn("unknown exception caught: " + e.getMessage(), e);
        } finally {
            responseObserver.onNext(txExtBuilder.build());
            responseObserver.onCompleted();
        }

    }

    private TriggerSmartContract triggerSmartContract(byte[] callerAddress,
                                                      byte[] contractAddress,
                                                      byte[] selector) {
        TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(callerAddress));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(selector));
        return builder.build();
    }

    private List<Object> parseArgs(ContractCallParam callParam) {
        List<Object> args = new ArrayList<>();
        //List<ByteString> tmp = callParam.getArgsList().asByteStringList();
        ProtocolStringList paramArgsList = callParam.getArgsList();
        args.addAll(paramArgsList);

        /*for (ByteString arg : tmp) {
            args.add(DecodeUtil.createReadableString(arg));
        }*/
        return args;
    }

    private StatusRuntimeException getRunTimeException(Exception e) {
        if (e != null) {
            return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
        } else {
            return Status.INTERNAL.withDescription("unknown").asRuntimeException();
        }
    }
}
