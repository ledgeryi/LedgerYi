package cn.ledgeryi.framework.core.permission.aop;

import cn.ledgeryi.api.GrpcAPI.GrpcRequest;
import cn.ledgeryi.framework.core.config.args.Args;
import cn.ledgeryi.framework.core.permission.PermissionManager;
import cn.ledgeryi.framework.core.permission.constant.RoleTypeEnum;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j(topic = "permission")
@Aspect
@Component
public class AuthenticationAspect {

    @Autowired
    private PermissionManager permissionManager;

    @Pointcut("@annotation(authentication) && args(request, responseObserver)")
    public void permission(Authentication authentication, GrpcRequest request, StreamObserver responseObserver){
    }

    @Before("permission(authentication, request, responseObserver)")
    public void doAuthentication(JoinPoint joinPoint,
                                 Authentication authentication, GrpcRequest request, StreamObserver responseObserver){

        if (Args.getInstance().isNotPermissionNet()) {
            return;
        }
        Signature signature = joinPoint.getSignature();
        String methodName = signature.getName();
        String requestAddress = request.getRequestAddress();
        String requestRole = request.getRequestRole();
        RoleTypeEnum preSetRole = authentication.role();
        //如果你的角色与我设置的角色不一样，就抛出AuthorizeException异常，反之就继续下一步验证
        //todo
        if (!preSetRole.getType().equals(requestRole)) {
            log.warn("user [{}] call method [{}], refused.", requestAddress, methodName);
            responseObserver.onError(new StatusRuntimeException(Status.PERMISSION_DENIED));
        }

        //如果你的角色与我设置的角色一样，就调用合约继续验证，验证失败就抛出AuthorizeException异常
        //todo
        boolean hasRole = permissionManager.hasRole(requestAddress, preSetRole.getType());
        if (!hasRole) {
            log.warn("user [{}] request method [{}], refused.", requestAddress, methodName);
            responseObserver.onError(new StatusRuntimeException(Status.PERMISSION_DENIED));
        }

        /**
         * 部署合约：
         * 调用合约（发起交易）：
         * 共识：
         * 只读：读区块，读交易，调用合约读
         */


    }




}
