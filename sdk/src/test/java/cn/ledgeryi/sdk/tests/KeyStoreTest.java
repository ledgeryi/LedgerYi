package cn.ledgeryi.sdk.tests;

import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.exception.CipherException;
import cn.ledgeryi.sdk.keystore.WalletUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;


/**
 * @author Brian
 * @date 2021/8/11 16:45
 */
public class KeyStoreTest {

    @Test
    public void createKeyStoreFile() throws IOException, CipherException {
        // String keyStoreFile = createKeyStoreFile("1qaz2wsx@");
        // System.out.println(keyStoreFile);
        File file = new File("./Wallet/c1fdf6f30dfc8eaeba1bf2161e3b59db3e8e3f02.json");
        String address = WalletUtils.exportAddress(file);
        System.out.println(address);
        byte[] bytes = WalletUtils.exportPrivateKey(file, "1qaz2wsx@");
        System.out.println(DecodeUtil.encode(bytes));
    }

    @Test
    public void importWallet() throws CipherException, IOException {
        File file = new File("./Wallet/c1fdf6f30dfc8eaeba1bf2161e3b59db3e8e3f02.json");
        byte[] keys = WalletUtils.exportPrivateKey(file, "1qaz2wsx@");
        String walletAndStore = WalletUtils.importWalletAndStore("1qaz2wsx3@".toCharArray(), keys);
        System.out.println(walletAndStore);
    }
}
