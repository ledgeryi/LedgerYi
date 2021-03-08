package cn.ledgeryi.framework.core.permission.constant;

import lombok.Getter;

// 1 - read only
// 2 - transaction only
// 3 - deploy contract
// 4 - consensus
public enum RoleTypeEnum {

    READ_ONLY("1"),
    TRANSACTION_ONLY("2"),
    DEPLOY_CONTRACT("3"),
    CONSENSUS("4");

    @Getter
    private String type;

    RoleTypeEnum(String type){
        this.type = type;
    }
}