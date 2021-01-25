package cn.ledgeryi.sdk.parse.event;

import cn.ledgeryi.protos.Protocol.TransactionInfo;
import cn.ledgeryi.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import cn.ledgeryi.sdk.common.utils.DecodeUtil;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class Log {

    private String contractAddress;
    private List<String> topics = new ArrayList<>();
    private BigInteger data;

    public static List<Log> parseLogInfo(List<TransactionInfo.Log> logList, ABI abi){
        List<Log> logs = new ArrayList<>();
        logList.forEach(log -> {
            Log tmp = new Log();
            tmp.setContractAddress(DecodeUtil.createReadableString(log.getAddress()));
            tmp.setData(DataWord.of(log.getData().toByteArray()).value());

            List<String> topics = new ArrayList<>();
            for (int i = 0; i < log.getTopicsCount(); i++){
                if (i == 0){
                    ByteString topic = log.getTopics(i);
                    String function = function(abi, topic.toByteArray());
                    topics.add(function);
                    continue;
                }
                //address length is 20 bytes
                topics.add(DataWord.of(log.getTopics(i).toByteArray()).toHexString().substring(24));
            }
            tmp.setTopics(topics);

            logs.add(tmp);
        });
        return logs;
    }

    private static String function(ABI abi, byte[] hash){
        JSONObject jsonObject = JSONObject.parseObject(JsonFormatUtil.printABI(abi));
        String abiJson = jsonObject.getString("entrys");
        CallTransaction.Contract contract = new CallTransaction.Contract(abiJson.replaceAll("'", "\""));
        CallTransaction.Function function = contract.getBySignatureHash(hash);
        return function.formatSignature();
    }
}
