package cn.ledgeryi.framework.core.permission.aop;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.common.utils.ByteArray;
import cn.ledgeryi.framework.core.exception.AuthorizeException;
import cn.ledgeryi.framework.core.permission.PermissionManager;
import cn.ledgeryi.framework.core.permission.constant.RoleTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j(topic = "permission")
@Aspect
@Component
public class PushBlockAspect {

    @Autowired
    private PermissionManager permissionManager;

    @Pointcut("execution(* cn.ledgeryi.framework.core.db.Manager.pushBlock(..)) && args(block)")
    public void pointPushBlock(BlockCapsule block) {

    }

    @Before("pointPushBlock(block)")
    public void doAuthentication(BlockCapsule block) throws AuthorizeException {
        if (block.generatedByMyself) {
            return;
        }
        String producer = ByteArray.toHexString(block.getMasterAddress().toByteArray());
        boolean hasConsensusRole = permissionManager.hasRole(producer, RoleTypeEnum.CONSENSUS.getType());
        if (!hasConsensusRole) {
            log.warn("Block cannot be produced without the role of producing block");
          throw new AuthorizeException("Block cannot be produced without the role of producing block");
        }
    }
}
