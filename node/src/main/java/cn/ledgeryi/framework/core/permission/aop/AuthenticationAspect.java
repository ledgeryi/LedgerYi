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
        int requestRole = request.getRequestRole();
        if (isContain(requestRole,authentication)) {
            boolean hasRole = permissionManager.hasRole(requestAddress, requestRole);
            if (hasRole) {
                return;
            }
        }
        log.warn("user [{}] call method [{}], refused.", requestAddress, methodName);
        responseObserver.onError(new StatusRuntimeException(Status.PERMISSION_DENIED));
    }

    private boolean isContain(int requestRole, Authentication authentication){
        RoleTypeEnum[] preSetRoles = authentication.roles();
        for (RoleTypeEnum preSetRole : preSetRoles) {
            if (preSetRole.getType() == requestRole){
                return true;
            }
        }
        return false;
    }
}
