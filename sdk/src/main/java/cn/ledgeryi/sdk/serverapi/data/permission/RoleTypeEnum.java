package cn.ledgeryi.sdk.serverapi.data.permission;

import lombok.Getter;

// 1 - read only
// 2 - contract call
// 3 - contract deploy
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