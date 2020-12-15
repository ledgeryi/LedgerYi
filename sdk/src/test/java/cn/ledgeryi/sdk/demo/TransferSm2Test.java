package cn.ledgeryi.sdk.demo;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignUtils;
import cn.ledgeryi.crypto.sm2.SM2;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.crypto.Sha256Sm3Hash;
import cn.ledgeryi.sdk.common.utils.CommonUtils;
import cn.ledgeryi.sdk.common.utils.JsonFormat;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.common.utils.TransactionUtils;
import cn.ledgeryi.sdk.serverapi.RequestNodeApi;
import com.google.protobuf.ByteString;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.security.SignatureException;
import java.util.Arrays;

public class TransferSm2Test {
    @Test
    public void sm2SignAndVerify() throws SignatureException {
        SM2 sm2 = new SM2();
        byte[] tmp = sm2.getAddress();
        System.out.println("address: " + CommonUtils.encode58Check(tmp));
        System.out.println("privateKey: " + Hex.toHexString(sm2.getPrivateKey()));

        String boBAddress = "YSanr1P38VwY3Q4Hg8kqzaWRBZocCeeRuA";
        String boBPrivateKey = "768ef623149e6215468864d5020cc7860e90a5cf734d1680acac8f3e170ab551";
        SignInterface signInterface = SignUtils.fromPrivate(Hex.decode(boBPrivateKey), false);

        if (!boBPrivateKey.equals(Hex.toHexString(signInterface.getPrivateKey()))) {
            System.out.println(Hex.toHexString(signInterface.getPrivateKey()));
        }

        if (!boBAddress.equals(CommonUtils.encode58Check(signInterface.getAddress()))) {
            System.out.println("address: " + CommonUtils.encode58Check(signInterface.getAddress()));
        }

        String signHash = signInterface.signHash(Sha256Sm3Hash.of(boBAddress.getBytes()).getBytes());
        ByteString signBytes = ByteString.copyFrom(signInterface.Base64toBytes(signHash));

        byte[] address = SignUtils.signatureToAddress(
                Sha256Sm3Hash.of(boBAddress.getBytes()).getBytes(),
                TransactionUtils.getBase64FromByteString(signBytes), false);
        if (!Arrays.equals(address, CommonUtils.decodeFromBase58Check(boBAddress))) {
            System.out.println("verify signature failed!");
        }
   }

    @Test
    public void createTransfer(){
        String gensisAddress = "YWgD62Eo96tyVpJPhvRgpfp1xiL7orersr";
        String gensisPrivateKey = "9d774de7889b13ea3c5124ffce7f2f179cc05d803049b357fc4a374456aa74fb";
        SignInterface signInterface = SignUtils.fromPrivate(Hex.decode(gensisPrivateKey), true);

        if (!gensisPrivateKey.equals(Hex.toHexString(signInterface.getPrivateKey()))) {
            System.out.println(Hex.toHexString(signInterface.getPrivateKey()));
            return;
        }

        String burnAddress = "YkPbEcezkrvbxNByaajAhcczBK9E5iiruA";
        RequestNodeApi.createTransfer(CommonUtils.decodeFromBase58Check(gensisAddress),
                CommonUtils.decodeFromBase58Check(burnAddress), 1, signInterface.getPrivateKey());
    }

    @Test
    public void batchTransfer(){
        long start = System.currentTimeMillis();
        for (int i = 1; i <= 900; i++){
            createTransfer();
        }
        System.out.println("cast time: " + (System.currentTimeMillis() - start));//133058
    }

    @Test
    public void createAddress(){
        SM2 sm2 = new SM2();
        byte[] address = sm2.getAddress();
        System.out.println("AddressStr: " + Hex.toHexString(address));
        System.out.println("AddressBase58: " + CommonUtils.encode58Check(address));
        System.out.println("privateStr: " + Hex.toHexString(sm2.getPrivateKey()));
        //System.out.println("AddressStr: " + Hex.toHexString(CommonUtils.decode58Check(CommonUtils.encode58Check(address))));
    }

    @Test
    public void queryAccount(){
        String gensis = "YSanr1P38VwY3Q4Hg8kqzaWRBZocCeeRuA";
        String burn = "YWgD62Eo96tyVpJPhvRgpfp1xiL7orersr";
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(queryAccount(CommonUtils.decodeFromBase58Check(gensis)), true)));
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(queryAccount(CommonUtils.decodeFromBase58Check(burn)), true)));
    }

    private Protocol.Account queryAccount(byte[] address){
        return RequestNodeApi.queryAccount(address);
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