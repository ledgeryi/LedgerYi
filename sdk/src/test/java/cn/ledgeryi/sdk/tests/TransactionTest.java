package cn.ledgeryi.sdk.tests;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.utils.JsonFormat;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import cn.ledgeryi.sdk.serverapi.data.RequestUserInfo;
import cn.ledgeryi.sdk.serverapi.data.TransactionInformation;
import com.alibaba.fastjson.JSONArray;
import org.junit.Before;
import org.junit.Test;

public class TransactionTest {

    private LedgerYiApiService ledgerYiApiService;

    @Before
    public void start(){
        ledgerYiApiService = new LedgerYiApiService();
    }

    @Test
    public void getNowBlock(){
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(ledgerYiApiService.getNowBlock(),true)));
    }

    @Test
    public void getBlockByNum(){
        long num = 0;
        String address = "9bd34d14acc715a37bcf77da13322526258bbb2d";
        RequestUserInfo requestUserInfo = RequestUserInfo.builder().address(address).roleId(1).build();
        GrpcAPI.BlockExtention block = ledgerYiApiService.getBlock(num,requestUserInfo);
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(block, true)));
    }

    @Test
    public void getBlockByLimitNext(){
        long start = 1;
        long end = 2;
        GrpcAPI.BlockListExtention blockByLimitNext = ledgerYiApiService.getBlockByLimitNext(start, end);
        blockByLimitNext.getBlockList().forEach(
                block -> System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(block, true))));
    }

    @Test
    public void getTransactionById(){
        String hash = "a74ccc7a6a920f76db8a0cb702796cb75868c3fc68310f38f0524b4341df0504";
        Protocol.Transaction transaction = ledgerYiApiService.getTransactionById(hash);
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(transaction, true)));
    }

    @Test
    public void getTransactionInfoById(){
        String hash = "01170b8dd0aae11b3706958bb0bdf95155aecde4fdc229c62825e8a546f2994e";
        Protocol.TransactionInfo transactionInfo = ledgerYiApiService.getTransactionInfoById(hash);
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(transactionInfo, true)));
    }

    @Test
    public void getTransactionInfoReadableById(){
        String hash = "f7c3ea49ca37eccdadc2413eff0653e6c84a2a3c0efe2a9d60c447ece5cc52ba";
        TransactionInformation infoReadable = ledgerYiApiService.getTransactionInfoReadable(hash);
        Object obj = JSONArray.toJSON(infoReadable);
        String json = obj.toString();
        System.out.println(JsonFormatUtil.formatJson(json));
    }


    @Test
    public void getTransactionCountByBlockNum(){
        long blockNum = 2;
        GrpcAPI.NumberMessage transactionCountByBlockNum = ledgerYiApiService.getTransactionCountByBlockNum(blockNum);
        System.out.println(transactionCountByBlockNum.getNum());
    }
}
