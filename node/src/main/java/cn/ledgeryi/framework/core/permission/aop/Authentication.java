package cn.ledgeryi.framework.core.permission.aop;

import cn.ledgeryi.framework.core.permission.constant.RoleTypeEnum;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Authentication {
    RoleTypeEnum role() default RoleTypeEnum.READ_ONLY;

    //String[] roles() default  {"1"};

    // 1 - read only
    // 2 - transaction only
    // 3 - deploy contract
    // 4 - consensus
}
