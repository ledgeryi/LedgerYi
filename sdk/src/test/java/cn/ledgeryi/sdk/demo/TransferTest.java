package cn.ledgeryi.sdk.demo;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.common.utils.DecodeUtil;
import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignUtils;
import cn.ledgeryi.crypto.ecdsa.ECKey;
import cn.ledgeryi.crypto.sm2.SM2;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.crypto.Sha256Sm3Hash;
import cn.ledgeryi.sdk.common.utils.JsonFormat;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.common.utils.TransactionUtils;
import cn.ledgeryi.sdk.config.Configuration;
import cn.ledgeryi.sdk.serverapi.RequestNodeApi;
import com.google.protobuf.ByteString;
import org.junit.Test;

import java.security.SignatureException;
import java.util.Arrays;

public class TransferTest {

    private SignInterface createSignEngine(){
        if (Configuration.isEckey()) {
            return new ECKey();
        } else {
            return new SM2();
        }
    }

    @Test
    public void createAddressAndPrivateKey(){
        SignInterface signEngine = createSignEngine();
        byte[] address = signEngine.getAddress();
        System.out.println("AddressStr: " + DecodeUtil.createReadableString(address));
        System.out.println("privateStr: " + DecodeUtil.createReadableString(signEngine.getPrivateKey()));
        if (!Arrays.equals(address, DecodeUtil.decode(DecodeUtil.createReadableString(address)))) {
            System.out.println("address decode error!");
        }
    }

    @Test
    public void signAndVerify() throws SignatureException {
        SignInterface signEngine = createSignEngine();
        byte[] tmp = signEngine.getAddress();
        final String boBAddress = DecodeUtil.createReadableString(tmp);
        final String boBPrivateKey = DecodeUtil.createReadableString(signEngine.getPrivateKey());
        SignInterface signInterface = SignUtils.fromPrivate(DecodeUtil.decode(boBPrivateKey), Configuration.isEckey());
        String signHash = signInterface.signHash(Sha256Sm3Hash.of(boBAddress.getBytes()).getBytes());
        ByteString signBytes = ByteString.copyFrom(signInterface.Base64toBytes(signHash));
        byte[] address = SignUtils.signatureToAddress(Sha256Sm3Hash.of(boBAddress.getBytes()).getBytes(),
                TransactionUtils.getBase64FromByteString(signBytes), false);
        if (!Arrays.equals(address, DecodeUtil.decode(boBAddress))) {
            System.out.println("verify signature failed!");
        }
   }

    @Test
    public void createTransfer(){
        final String gensisAddress = "516a2989dd5d8dfa73aed093bdd14fce13a9c73d";
        final String gensisPrivateKey = "b1f8d41815c1291a99c62f2f6e61194305191cbbe375c7a7dedc0559bb333826";
        SignInterface signInterface = SignUtils.fromPrivate(DecodeUtil.decode(gensisPrivateKey), Configuration.isEckey());
        if (!gensisPrivateKey.equals(DecodeUtil.createReadableString(signInterface.getPrivateKey()))) {
            System.out.println(DecodeUtil.createReadableString(signInterface.getPrivateKey()));
            return;
        }
        final String burnAddress = "50bae38baeb4a99c2d271ac77294683ab4b3b5ed";
        //privateKey: d2ae983a039134fb46fd060c8d2dc3de83bb7984adede4f01b8ea9658cfc063a
        RequestNodeApi.createTransfer(DecodeUtil.decode(gensisAddress),
                DecodeUtil.decode(burnAddress), 1, signInterface.getPrivateKey());
    }

    @Test
    public void batchTransfer(){
        for (int i = 1; i <= 100; i++){
            createTransfer();
        }
    }

    @Test
    public void queryAccount(){
        final String gensis = "516a2989dd5d8dfa73aed093bdd14fce13a9c73d";
        final String burn = "50bae38baeb4a99c2d271ac77294683ab4b3b5ed";
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(RequestNodeApi.queryAccount(DecodeUtil.decode(gensis)), true)));
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(RequestNodeApi.queryAccount(DecodeUtil.decode(burn)), true)));
    }

    @Test
    public void getNowBlock(){
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(RequestNodeApi.getNowBlock(),true)));
    }

    @Test
    public void getBlockByNum(){
        long num = 2;
        GrpcAPI.BlockExtention block = RequestNodeApi.getBlock(num);
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(block, true)));
    }

    @Test
    public void getBlockByLimitNext(){
        long start = 1;
        long end = 2;
        GrpcAPI.BlockListExtention blockByLimitNext = RequestNodeApi.getBlockByLimitNext(start, end);
        blockByLimitNext.getBlockList().forEach(
                block -> System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(block, true))));
    }

    @Test
    public void getTransactionById(){
        String hash = "c5aa8a08cddc549670d211d9248daec519b80b800243ed1d8f62f0d041f13b91";
        Protocol.Transaction transaction = RequestNodeApi.getTransactionById(hash);
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(transaction, true)));
    }

    @Test
    public void getTransactionCountByBlockNum(){
        long blockNum = 2;
        GrpcAPI.NumberMessage transactionCountByBlockNum = RequestNodeApi.getTransactionCountByBlockNum(blockNum);
        System.out.println(transactionCountByBlockNum.getNum());
    }
}