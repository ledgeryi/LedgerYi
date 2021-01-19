package cn.ledgeryi.sdk.demo;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.utils.JsonFormat;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
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
        long num = 6;
        GrpcAPI.BlockExtention block = ledgerYiApiService.getBlock(num);
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
        String hash = "a74ccc7a6a920f76db8a0cb702796cb75868c3fc68310f38f0524b4341df0504";
        Protocol.TransactionInfo transactionInfo = ledgerYiApiService.getTransactionInfoById(hash);
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(transactionInfo, true)));
    }

    @Test
    public void getTransactionCountByBlockNum(){
        long blockNum = 2;
        GrpcAPI.NumberMessage transactionCountByBlockNum = ledgerYiApiService.getTransactionCountByBlockNum(blockNum);
        System.out.println(transactionCountByBlockNum.getNum());
    }
}
