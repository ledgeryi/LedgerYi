package cn.ledgeryi.framework.core.permission.constant;

import lombok.Getter;

public enum NetTypeEnum {
    PERMISSIONED_BLOCKCHAIN("1"),
    PUBLIC_BLOCKCHAIN("2");

    @Getter
    private String value;

    NetTypeEnum(String value) {
      this.value = value;
    }
  }