package cn.ledgeryi.sdk.serverapi.data.permission;

import lombok.Builder;
import lombok.Data;

/**
 * @author Brian
 * @date 2021/3/15 13:58
 */
@Data
@Builder
public class User {
    private String userId;
    private int roleId;
    private String address;
    private boolean active;
}
