package cn.ledgeryi.framework.core.api;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.GrpcRequest;
import cn.ledgeryi.api.GrpcAPI.TransactionExtention;
import cn.ledgeryi.api.PermissionGrpc;
import cn.ledgeryi.framework.core.LedgerYi;
import cn.ledgeryi.protos.Protocol;
import com.google.protobuf.Any;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PermissionApi extends PermissionGrpc.PermissionImplBase {

    @Autowired
    private LedgerYiApi walletApi;

    @Autowired
    private LedgerYi wallet;

    @Override
    public void addNewRole(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Override
    public void inactiveRole(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Override
    public void assignRoleForUser(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Override
    public void revokeRoleOfUser(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Override
    public void hasRole(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Override
    public void addNewNode(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Override
    public void broadcastTransaction(GrpcRequest request, StreamObserver<GrpcAPI.Return> responseObserver) {
        try{
            Any param = request.getParam();
            Protocol.Transaction transaction = param.unpack(Protocol.Transaction.class);
            responseObserver.onNext(wallet.broadcastTransaction(transaction));
        } catch (Exception e) {
            responseObserver.onError(getRunTimeException(e));
        }
        responseObserver.onCompleted();
    }

    private StatusRuntimeException getRunTimeException(Exception e) {
        if (e != null) {
            return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
        } else {
            return Status.INTERNAL.withDescription("unknown").asRuntimeException();
        }
    }
}
