package cn.ledgeryi.framework.core.permission.entity;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class Role {
    private String roleId;
    private boolean active;
    private Map<String,Boolean> accounts;
}
