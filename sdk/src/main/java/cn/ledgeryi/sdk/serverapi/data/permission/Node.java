package cn.ledgeryi.sdk.serverapi.data.permission;

import lombok.Builder;
import lombok.Data;

/**
 * @author Brian
 * @date 2021/3/15 14:10
 */
@Data
@Builder
public class Node {
    private String nodeId;
    private String owner;
    private String host;
    private int port;
}
