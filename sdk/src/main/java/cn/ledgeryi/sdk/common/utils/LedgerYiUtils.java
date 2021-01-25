package cn.ledgeryi.sdk.common.utils;

import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.ecdsa.ECKey;
import cn.ledgeryi.crypto.sm2.SM2;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.AccountYi;
import cn.ledgeryi.sdk.config.Configuration;

public class LedgerYiUtils {

    public static AccountYi createAccountYi() {
        SignInterface signInterface;
        if (Configuration.isEcc()) {
            signInterface = new ECKey();
        } else {
            signInterface = new SM2();
        }
        return AccountYi.builder()
                .privateKeyStr(DecodeUtil.createReadableString(signInterface.getPrivateKey()))
                .publicKeyStr(DecodeUtil.createReadableString(signInterface.getPubKey()))
                .address(DecodeUtil.createReadableString(signInterface.getAddress()))
                .accountType(Protocol.AccountType.Normal)
                .build();
    }


}