package cn.ledgeryi.sdk.keystore;

import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.ecdsa.ECKey;
import cn.ledgeryi.sdk.common.utils.ByteArray;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;

/**
 * Credentials wrapper.
 */
public class CredentialsEckey implements Credentials {

    private final ECKey ecKeyPair;
    private final String address;

    private CredentialsEckey(ECKey ecKeyPair, String address) {
        this.ecKeyPair = ecKeyPair;
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public static CredentialsEckey create(ECKey ecKeyPair) {
        String address = DecodeUtil.createReadableString(ecKeyPair.getAddress());
        return new CredentialsEckey(ecKeyPair, address);
    }

    public static CredentialsEckey create(String privateKey) {
        ECKey eCkey = ECKey.fromPrivate(ByteArray.fromHexString(privateKey));
        return create(eCkey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CredentialsEckey that = (CredentialsEckey) o;

        if (ecKeyPair != null ? !ecKeyPair.equals(that.ecKeyPair) : that.ecKeyPair != null) {
            return false;
        }

        return address != null ? address.equals(that.address) : that.address == null;
    }

    @Override
    public int hashCode() {
        int result = ecKeyPair != null ? ecKeyPair.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }

    @Override
    public SignInterface getPair() {
        return ecKeyPair;
    }
}
