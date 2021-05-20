package cn.ledgeryi.sdk.serverapi.data.permission;

import lombok.Getter;

// 1 - read only
// 2 - contract call
// 3 - contract deploy
// 4 - block produce
public enum RoleTypeEnum {

    READ_ONLY(1, "Read Only"),
    CONTRACT_CALL(2, "Contract Call"),
    CONTRACT_DEPLOY(3, "Contract Deploy"),
    BLOCK_PRODUCE(4, "Block Produce");

    @Getter
    private int type;
    private String name;

    RoleTypeEnum(int type, String name){
        this.type = type;
        this.name = name;
    }
}