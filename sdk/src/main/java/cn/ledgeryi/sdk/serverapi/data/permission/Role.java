package cn.ledgeryi.sdk.serverapi.data.permission;

import lombok.Builder;
import lombok.Data;

/**
 * @author Brian
 * @date 2021/3/15 11:59
 */
@Data
@Builder
public class Role {
    private int roleId;
    private boolean active;
}
