package cn.ledgeryi.framework.core.permission.constant;

import lombok.Getter;

// 1 - read only
// 2 - transaction only
// 3 - deploy contract
// 4 - block produce
public enum RoleTypeEnum {

    READ_ONLY(1),
    CONTRACT_CALL(2),
    CONTRACT_DEPLOY(3),
    BLOCK_PRODUCE(4);

    @Getter
    private int type;

    RoleTypeEnum(int type){
        this.type = type;
    }
}