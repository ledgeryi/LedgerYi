package cn.ledgeryi.sdk.tests;

import cn.ledgeryi.protos.contract.SmartContractOuterClass;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.parse.event.CallTransaction;
import cn.ledgeryi.sdk.parse.event.DataWord;
import cn.ledgeryi.sdk.parse.event.LogInfo;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class ParseLogTest {

    private LedgerYiApiService ledgerYiApiService;

    private static String contractAddres = "9bd34d14acc715a37bcf77da13322526258bbb2d";

    @Before
    public void start(){
        ledgerYiApiService = new LedgerYiApiService();
    }

    @Test
    public void parseEventTest(){
        SmartContractOuterClass.SmartContract smartContract= ledgerYiApiService.getContract(DecodeUtil.decode(contractAddres));

        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(smartContract.getAbi()));
        String abiJson = jsonObject.getString("entrys");
        System.out.println(abiJson);

        CallTransaction.Contract contract = new CallTransaction.Contract(abiJson.replaceAll("'", "\""));
        List<DataWord> topics = new ArrayList<>();
        topics.add(DataWord.of("ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"));
        topics.add(DataWord.of("000000000000000000000000ada95a8734256b797efcd862e0b208529283ac56"));
        topics.add(DataWord.of("0000000000000000000000003d21f860eabb8cf18b9c1c37d4133a9ed15cb7b4"));
        byte[] data = Hex.decode("0000000000000000000000000000000000000000000000000000000000000004");
        LogInfo logInfo = new LogInfo(Hex.decode("9bd34d14acc715a37bcf77da13322526258bbb2d"), topics, data);
        CallTransaction.Invocation e = contract.parseEvent(logInfo);
        System.out.println(e.toString());
    }
}
