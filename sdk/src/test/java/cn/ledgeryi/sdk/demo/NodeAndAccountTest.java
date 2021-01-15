package cn.ledgeryi.sdk.demo;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.utils.JsonFormat;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.serverapi.RequestNodeAPI;
import org.junit.Test;

public class NodeAndAccountTest {

    @Test
    public void getAccount(){
        String address = "851577327413881a6e47def36d54b4978609ca05";
        Protocol.Account account = RequestNodeAPI.getAccount(address);
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(account, true)));
    }

    @Test
    public void getMasters(){
        GrpcAPI.MastersList masters = RequestNodeAPI.getMasters();
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(masters, true)));
    }

    @Test
    public void getConnectNodes(){
        GrpcAPI.NodeList connectNodes = RequestNodeAPI.getConnectNodes();
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(connectNodes, true)));
    }

    @Test
    public void getNodeInfo(){
        Protocol.NodeInfo nodeInfo = RequestNodeAPI.getNodeInfo();
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(nodeInfo, true)));
    }
}
