package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.crypto.ecdsa.ECKey;
import cn.ledgeryi.crypto.sm2.SM2;
import cn.ledgeryi.sdk.common.crypto.Sha256Sm3Hash;
import cn.ledgeryi.sdk.common.utils.Base58;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.Utils;
import cn.ledgeryi.sdk.exception.CipherException;
import cn.ledgeryi.sdk.keystore.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Brian
 * @date 2021/7/22 11:06
 */
@Slf4j
public class LedgerYiApi {
    private List<WalletFile> walletFile = new ArrayList<>();
    private static final String FilePath = "Wallet";
    private static boolean isEckey = true;
    private boolean loginState = false;

    @Getter
    private byte[] address;

    public String importWallet(char[] password, byte[] priKey)
            throws CipherException, IOException {
        if (!passwordValid(password)) {
            return null;
        }
        if (!priKeyValid(priKey)) {
            return null;
        }
        byte[] passwd = StringUtils.char2Byte(password);
        WalletFile walletFile = createWalletFile(passwd, priKey);
        StringUtils.clear(passwd);
        String keystoreName = store2Keystore(walletFile);
        logout();
        return keystoreName;
    }

    public static WalletFile createWalletFile(byte[] password)
            throws CipherException {
        WalletFile walletFile = null;
        if (isEckey) {
            ECKey ecKey = new ECKey(Utils.getRandom());
            walletFile = Wallet.createStandard(password, ecKey);
        } else {
            SM2 sm2 = new SM2(Utils.getRandom());
            walletFile = Wallet.createStandard(password, sm2);
        }
        return walletFile;
    }

    public static WalletFile createWalletFile(byte[] password, byte[] priKey)
            throws CipherException {
        WalletFile walletFile;
        if (isEckey) {
            ECKey ecKey = ECKey.fromPrivate(priKey);
            walletFile = Wallet.createStandard(password, ecKey);
        } else {
            SM2 sm2 = SM2.fromPrivate(priKey);
            walletFile = Wallet.createStandard(password, sm2);
        }
        return walletFile;
    }

    public boolean changePassword(char[] oldPassword, char[] newPassword)
            throws IOException, CipherException {
        logout();
        if (!passwordValid(newPassword)) {
            log.warn("Warning: Change password failed, NewPassword is invalid !!");
            return false;
        }

        byte[] oldPasswd = StringUtils.char2Byte(oldPassword);
        byte[] newPasswd = StringUtils.char2Byte(newPassword);

        boolean result = changeKeystorePassword(oldPasswd, newPasswd);
        StringUtils.clear(oldPasswd);
        StringUtils.clear(newPasswd);
        return result;
    }

    public static byte[] exportAddress() throws IOException {
        LedgerYiApi ledgerYiApi = loadWalletFromKeystore();
        return ledgerYiApi.getAddress();
    }

    public byte[] exportPrivateKey(String password)
            throws CipherException, IOException {
        byte[] pwd = StringUtils.char2Byte(password.toCharArray());
        return getPrivateBytes(pwd);
    }

    public static boolean priKeyValid(byte[] priKey) {
        if (ArrayUtils.isEmpty(priKey)) {
            log.warn("Warning: PrivateKey is empty !!");
            return false;
        }
        if (priKey.length != 32) {
            log.warn("Warning: PrivateKey length need 64 but " + priKey.length + " !!");
            return false;
        }
        // Other rule;
        return true;
    }

    public boolean isLoginState() {
        return loginState;
    }

    public void logout() {
        loginState = false;
        walletFile.clear();
        this.walletFile = null;
    }

    public void setLogin() {
        loginState = true;
    }

    public boolean checkPassword(byte[] passwd) throws CipherException {
        return Wallet.validPassword(passwd, this.walletFile.get(0));
    }

    public static boolean passwordValid(char[] password) {
        if (ArrayUtils.isEmpty(password)) {
            throw new IllegalArgumentException("password is empty");
        }
        if (password.length < 6) {
            log.warn("Warning: Password is too short !!");
            return false;
        }
        // Other rule;
        int level = CheckStrength.checkPasswordStrength(password);
        if (level <= 4) {
            log.warn("Your password is too weak!");
            log.warn("The password should be at least 8 characters.");
            log.warn("The password should contains uppercase, lowercase, numeric and other.");
            log.warn("The password should not contain more than 3 duplicate numbers or letters; For example: 1111.");
            log.warn("The password should not contain more than 3 consecutive Numbers or letters; For example: 1234.");
            log.warn("The password should not contain weak password combination; For example:");
            log.warn("ababab, abcabc, password, passw0rd, p@ssw0rd, admin1234, etc.");
            return false;
        }
        return true;
    }

    /**
     * Creates a Wallet with an existing ECKey.
     * */
    public LedgerYiApi(WalletFile walletFile) {
        if (this.walletFile.isEmpty()) {
            this.walletFile.add(walletFile);
        } else {
            this.walletFile.set(0, walletFile);
        }
        this.address = decodeFromBase58Check(walletFile.getAddress());
    }

    public static byte[] decodeFromBase58Check(String addressBase58) {
        if (StringUtils.isEmpty(addressBase58)) {
            log.warn("Warning: Address is empty !!");
            return null;
        }
        byte[] address = decode58Check(addressBase58);
        if (!DecodeUtil.addressValid(address)) {
            return null;
        }
        return address;
    }

    public static String encode58Check(byte[] input) {
        byte[] hash0 = Sha256Sm3Hash.hash(input);
        byte[] hash1 = Sha256Sm3Hash.hash(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }

    private static byte[] decode58Check(String input) {
        byte[] decodeCheck = Base58.decode(input);
        if (decodeCheck.length <= 4) {
            return null;
        }
        byte[] decodeData = new byte[decodeCheck.length - 4];
        System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
        byte[] hash0 = Sha256Sm3Hash.hash(decodeData);
        byte[] hash1 = Sha256Sm3Hash.hash(hash0);
        if (hash1[0] == decodeCheck[decodeData.length]
                && hash1[1] == decodeCheck[decodeData.length + 1]
                && hash1[2] == decodeCheck[decodeData.length + 2]
                && hash1[3] == decodeCheck[decodeData.length + 3]) {
            return decodeData;
        }
        return null;
    }

    public ECKey getEcKey(WalletFile walletFile, byte[] password) throws CipherException {
        return Wallet.decrypt(password, walletFile);
    }

    public SM2 getSM2(WalletFile walletFile, byte[] password) throws CipherException {
        return Wallet.decryptSM2(password, walletFile);
    }

    public ECKey getEcKey(byte[] key)  {
        return Wallet.decrypt(key);
    }

    public SM2 getSM2(byte[] key) {
        return Wallet.decryptSM2(key);
    }

    public static byte[] getPrivateBytes(byte[] password) throws CipherException, IOException {
        WalletFile walletFile = loadWalletFile();
        return Wallet.decrypt2PrivateBytes(password, walletFile);
    }

    public static String store2Keystore(WalletFile walletFile) throws IOException {
        if (walletFile == null) {
            log.warn("Warning: Store wallet failed, walletFile is null !!");
            return null;
        }
        File file = new File(FilePath);
        if (!file.exists()) {
            if (!file.mkdir()) {
                throw new IOException("Make directory failed!");
            }
        } else {
            if (!file.isDirectory()) {
                if (file.delete()) {
                    if (!file.mkdir()) {
                        throw new IOException("Make directory failed!");
                    }
                } else {
                    throw new IOException("File exists and can not be deleted!");
                }
            }
        }
        return WalletUtils.generateWalletFile(walletFile, file);
    }

    public static File selcetWalletFile() {
        File file = new File(FilePath);
        if (!file.exists() || !file.isDirectory()) {
            return null;
        }

        File[] wallets = file.listFiles();
        if (ArrayUtils.isEmpty(wallets)) {
            return null;
        }

        File wallet;
        if (wallets.length > 1) {
            for (int i = 0; i < wallets.length; i++) {
                log.warn("The " + (i + 1) + "th keystore file name is " + wallets[i].getName());
            }
            log.warn("Please choose between 1 and " + wallets.length);
            Scanner in = new Scanner(System.in);
            while (true) {
                String input = in.nextLine().trim();
                String num = input.split("\\s+")[0];
                int n;
                try {
                    n = new Integer(num);
                } catch (NumberFormatException e) {
                    log.warn("Invaild number of " + num);
                    log.warn("Please choose again between 1 and " + wallets.length);
                    continue;
                }
                if (n < 1 || n > wallets.length) {
                    log.warn("Please choose again between 1 and " + wallets.length);
                    continue;
                }
                wallet = wallets[n - 1];
                break;
            }
        } else {
            wallet = wallets[0];
        }

        return wallet;
    }

    public WalletFile selcetWalletFileE() throws IOException {
        File file = selcetWalletFile();
        if (file == null) {
            throw new IOException(
                    "No keystore file found, please use registerwallet or importwallet first!");
        }
        String name = file.getName();
        for (WalletFile wallet : this.walletFile) {
            String address = wallet.getAddress();
            if (name.contains(address)) {
                return wallet;
            }
        }

        WalletFile wallet = WalletUtils.loadWalletFile(file);
        this.walletFile.add(wallet);
        return wallet;
    }

    public static boolean changeKeystorePassword(byte[] oldPassword, byte[] newPassowrd)
            throws IOException, CipherException {
        File wallet = selcetWalletFile();
        if (wallet == null) {
            throw new IOException(
                    "No keystore file found, please use registerwallet or importwallet first!");
        }
        Credentials credentials = WalletUtils.loadCredentials(oldPassword, wallet);
        WalletUtils.updateWalletFile(newPassowrd, credentials.getPair(), wallet, true);
        return true;
    }

    private static WalletFile loadWalletFile() throws IOException {
        File wallet = selcetWalletFile();
        if (wallet == null) {
            throw new IOException(
                    "No keystore file found, please use registerwallet or importwallet first!");
        }
        return WalletUtils.loadWalletFile(wallet);
    }

    /**
     * load a Wallet from keystore
     * */
    public static LedgerYiApi loadWalletFromKeystore() throws IOException {
        WalletFile walletFile = loadWalletFile();
        LedgerYiApi walletApi = new LedgerYiApi(walletFile);
        return walletApi;
    }
}
