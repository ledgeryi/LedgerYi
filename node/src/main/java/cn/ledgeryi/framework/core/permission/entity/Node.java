package cn.ledgeryi.framework.core.permission.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Node {
    private String nodeOwner;//address
    private String host;//127.0.0.1
    private int port;//20051
    private int status;
}
