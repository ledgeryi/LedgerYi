package cn.ledgeryi.framework.core.permission.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewNode {
    private String nodeOwner;//address
    private String host;//127.0.0.1
    private int port;//20051
    private boolean master;
    //private int status;
}
