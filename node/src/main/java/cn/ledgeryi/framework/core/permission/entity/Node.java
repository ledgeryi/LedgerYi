package cn.ledgeryi.framework.core.permission.entity;

import lombok.Data;

@Data
public class Node {
    private String nodeOwner;
    private String netAddress;
    private short status;
}
