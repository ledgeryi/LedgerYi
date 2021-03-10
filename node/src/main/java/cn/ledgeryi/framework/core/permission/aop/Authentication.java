package cn.ledgeryi.framework.core.permission.aop;

import cn.ledgeryi.framework.core.permission.constant.RoleTypeEnum;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Authentication {

    RoleTypeEnum[] roles() default {RoleTypeEnum.READ_ONLY};

    // 1 - read only
    // 2 - contract call
    // 3 - contract deploy
    // 4 - block produce
}
