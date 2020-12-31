package cn.ledgeryi.sdk.demo;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.utils.JsonFormat;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.serverapi.RequestNodeApi;
import org.junit.Test;

public class TransactionTest {

    @Test
    public void getNowBlock(){
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(RequestNodeApi.getNowBlock(),true)));
    }

    @Test
    public void getBlockByNum(){
        long num = 0;
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
