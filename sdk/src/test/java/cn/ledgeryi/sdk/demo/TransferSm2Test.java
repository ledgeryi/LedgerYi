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

        String boBAddress = "vDmsrxJqQ1U4D1remyeNNTQAAHkMVDDPpQ";
        String boBPrivateKey = "bfa0dfd9b18fdec35235770c820cbeea3ffc972e69422722f58608cf17462661";
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
        String gensisAddress = "2W4ArDTMcr89nDVQpHdQ6erYxryDEjcLcXh";
        String gensisPrivateKey = "d6c44b8c178c3edc3f50f1a90ff1043155b4f46d97dba2b99b06ac8539e99e35";
        SignInterface signInterface = SignUtils.fromPrivate(Hex.decode(gensisPrivateKey), true);

        if (!gensisPrivateKey.equals(Hex.toHexString(signInterface.getPrivateKey()))) {
            System.out.println(Hex.toHexString(signInterface.getPrivateKey()));
            return;
        }

        String burnAddress = "26c624xyzFmJWJ3RCWzTMozfRaxdLFoptfE";
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
    }

    @Test
    public void queryAccount(){
        String gensis = "26c624xyzFmJWJ3RCWzTMozfRaxdLFoptfE";
        String burn = "2W4ArDTMcr89nDVQpHdQ6erYxryDEjcLcXh";
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(queryAccount(CommonUtils.decodeFromBase58Check(gensis)), true)));
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(queryAccount(CommonUtils.decodeFromBase58Check(burn)), true)));
    }

    @Test
    public void addressBase58ConvertHexString(){
        String burnAddress = "26c624xyzFmJWJ3RCWzTMozfRaxdLFoptfE";
        byte[] byteAdress = CommonUtils.decode58Check(burnAddress);
        System.out.println("hexStringAddress: " + Hex.toHexString(byteAdress));
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