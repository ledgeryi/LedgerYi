package cn.ledgeryi.sdk.serverapi.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequestUserInfo {
    private String address;
    private int roleId;
}
