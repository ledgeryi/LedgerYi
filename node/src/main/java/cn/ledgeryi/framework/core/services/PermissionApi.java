package cn.ledgeryi.framework.core.services;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.api.GrpcAPI.GrpcRequest;
import cn.ledgeryi.api.GrpcAPI.TransactionExtention;
import cn.ledgeryi.api.PermissionGrpc;
import cn.ledgeryi.framework.core.permission.aop.Authentication;
import cn.ledgeryi.framework.core.permission.constant.RoleTypeEnum;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PermissionApi extends PermissionGrpc.PermissionImplBase {

    @Autowired
    private WalletApi walletApi;

    @Authentication(roles = {
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void addNewRole(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Authentication(roles = {
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void inactiveRole(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Authentication(roles = {
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void assignRoleForUser(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Authentication(roles = {
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void revokeRoleOfUser(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Authentication(roles = {
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void hasRole(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Authentication(roles = {
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void addNewNode(GrpcRequest request, StreamObserver<TransactionExtention> responseObserver) {
        walletApi.triggerContract(request,responseObserver);
    }

    @Authentication(roles = {
            RoleTypeEnum.CONTRACT_CALL,
            RoleTypeEnum.CONTRACT_DEPLOY,
            RoleTypeEnum.BLOCK_PRODUCE})
    @Override
    public void broadcastTransaction(GrpcRequest request, StreamObserver<GrpcAPI.Return> responseObserver) {
        walletApi.broadcastTransaction(request, responseObserver);
    }
}
