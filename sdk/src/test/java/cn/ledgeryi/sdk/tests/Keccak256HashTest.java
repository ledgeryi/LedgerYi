package cn.ledgeryi.sdk.tests;

import cn.ledgeryi.crypto.utils.Hash;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import org.junit.Test;

public class Keccak256HashTest {

    @Test
    public void mathHash() {
        String methodSign = "./Math.sol:Math";
        byte[] selector = new byte[17];
        System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector,0, 17);
        System.out.println(DecodeUtil.createReadableString(selector));
    }
}
