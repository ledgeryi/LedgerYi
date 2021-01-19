package cn.ledgeryi.sdk.common;

import cn.ledgeryi.protos.Protocol;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountYi {
    private String address;
    private String publicKeyStr;
    private String privateKeyStr;
    private Protocol.AccountType accountType;
}
