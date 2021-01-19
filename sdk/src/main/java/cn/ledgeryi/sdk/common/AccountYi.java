package cn.ledgeryi.sdk.common;

import cn.ledgeryi.protos.Protocol;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountYi {
    private String address;
    private String publicKeyStr;
    private String privateKeyStr;
    private Protocol.AccountType accountType;
}
