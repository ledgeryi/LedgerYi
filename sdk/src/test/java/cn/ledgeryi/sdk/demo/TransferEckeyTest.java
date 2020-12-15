package cn.ledgeryi.sdk.demo;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.crypto.SignInterface;
import cn.ledgeryi.crypto.SignUtils;
import cn.ledgeryi.crypto.ecdsa.ECKey;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.utils.CommonUtils;
import cn.ledgeryi.sdk.common.utils.JsonFormat;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.serverapi.RequestNodeApi;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

public class TransferEckeyTest {

    @Test
    public void createTransferToNotExist(){
        ECKey ecKey = new ECKey();
        byte[] to = ecKey.getAddress();
        System.out.println("toAddress: " + CommonUtils.encode58Check(to));

        String from = "Ycq5ho1cfrtdH5HNxmAtQwXseqFxYFprdA";
        String fromPrivateKey = "65bf254f32b0a19629954531265fdac1d29874d831ffd151ce44de9ac7daf2d";
        SignInterface signInterface = SignUtils.fromPrivate(Hex.decode(fromPrivateKey), true);

        if (!fromPrivateKey.equals(Hex.toHexString(signInterface.getPrivateKey()))) {
            System.out.println(Hex.toHexString(signInterface.getPrivateKey()));
            return;
        }

        RequestNodeApi.createTransfer(CommonUtils.decodeFromBase58Check(from),
                CommonUtils.decodeFromBase58Check(CommonUtils.encode58Check(to)), 1, signInterface.getPrivateKey());
    }

    @Test
    public void createTransfer(){
        String gensisAddress = "Ycq5ho1cfrtdH5HNxmAtQwXseqFxYFprdA";
        String gensisPrivateKey = "65bf254f32b0a19629954531265fdac1d29874d831ffd151ce44de9ac7daf2d";
        SignInterface signInterface = SignUtils.fromPrivate(Hex.decode(gensisPrivateKey), true);

        if (!gensisPrivateKey.equals(Hex.toHexString(signInterface.getPrivateKey()))) {
            System.out.println(Hex.toHexString(signInterface.getPrivateKey()));
            return;
        }

        String burnAddress = "YhKWnv7ribByKTjoSVbszYRNUzUf4EB78Q";
        RequestNodeApi.createTransfer(CommonUtils.decodeFromBase58Check(gensisAddress),
                CommonUtils.decodeFromBase58Check(burnAddress), 1, signInterface.getPrivateKey());
    }

    @Test
    public void batchTransfer(){
        for (int i = 1; i <= 5; i++){
            createTransfer();
        }
    }

    @Test
    public void createAddress(){
        ECKey ecKey = new ECKey();
        byte[] address = ecKey.getAddress();
        System.out.println("AddressStr: " + Hex.toHexString(address));
        System.out.println("AddressBase58: " + CommonUtils.encode58Check(address));
        //System.out.println("AddressStr: " + Hex.toHexString(CommonUtils.decode58Check(CommonUtils.encode58Check(address))));
        System.out.println("privateStr: " + Hex.toHexString(ecKey.getPrivateKey()));
    }

    @Test
    public void queryAccount(){
        String gensis = "Ycq5ho1cfrtdH5HNxmAtQwXseqFxYFprdA";
        String burn = "YhKWnv7ribByKTjoSVbszYRNUzUf4EB78Q";
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
        long num = 1;
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
        String hash = "97c6c6d187e9b735eba9f373022b001fc0f6033ae58c17821b6117ac8d44f8fa";
        Protocol.Transaction transaction = RequestNodeApi.getTransactionById(hash);
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(transaction, true)));
    }

    @Test
    public void getTransactionCountByBlockNum(){
        long blockNum = 3;
        GrpcAPI.NumberMessage transactionCountByBlockNum = RequestNodeApi.getTransactionCountByBlockNum(blockNum);
        System.out.println(transactionCountByBlockNum.getNum());
    }
}
