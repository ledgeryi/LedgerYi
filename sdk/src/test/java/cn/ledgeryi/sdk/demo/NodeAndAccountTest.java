package cn.ledgeryi.sdk.demo;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.utils.JsonFormat;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.serverapi.RequestNodeApi;
import org.junit.Test;

public class NodeAndAccountTest {

    @Test
    public void getAccount(){
        String address = "851577327413881a6e47def36d54b4978609ca05";
        Protocol.Account account = RequestNodeApi.getAccount(address);
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(account, true)));
    }

    @Test
    public void getMasters(){
        GrpcAPI.MastersList masters = RequestNodeApi.getMasters();
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(masters, true)));
    }

    @Test
    public void getConnectNodes(){
        GrpcAPI.NodeList connectNodes = RequestNodeApi.getConnectNodes();
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(connectNodes, true)));
    }

    @Test
    public void getNodeInfo(){
        Protocol.NodeInfo nodeInfo = RequestNodeApi.getNodeInfo();
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(nodeInfo, true)));
    }
}
