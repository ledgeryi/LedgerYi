package cn.ledgeryi.sdk.tests;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.crypto.utils.Hash;
import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.AccountYi;
import cn.ledgeryi.sdk.common.crypto.Sha256Sm3Hash;
import cn.ledgeryi.sdk.common.utils.JsonFormat;
import cn.ledgeryi.sdk.common.utils.JsonFormatUtil;
import cn.ledgeryi.sdk.common.utils.LedgerYiUtils;
import cn.ledgeryi.sdk.serverapi.LedgerYiApiService;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;

public class NodeAndAccountTest {

    private LedgerYiApiService ledgerYiApiService;

    @Before
    public void start(){
        ledgerYiApiService = new LedgerYiApiService();
    }

    @Test
    public void createDefaultAccount(){
        AccountYi defaultAccount = ledgerYiApiService.createDefaultAccount();
        System.out.println("address: " + defaultAccount.getAddress());
        System.out.println("privateKey: " + defaultAccount.getPrivateKeyStr());
        System.out.println("publicKey: " + defaultAccount.getPublicKeyStr());
        System.out.println("type: " + defaultAccount.getAccountType());
    }

    @Test
    public void getMasters(){
        GrpcAPI.MastersList masters = ledgerYiApiService.getMasters();
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(masters, true)));
    }

    @Test
    public void getConnectNodes(){
        GrpcAPI.NodeList connectNodes = ledgerYiApiService.getConnectNodes();
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(connectNodes, true)));
    }

    @Test
    public void getNodeInfo(){
        Protocol.NodeInfo nodeInfo = ledgerYiApiService.getNodeInfo();
        System.out.println(JsonFormatUtil.formatJson(JsonFormat.printToString(nodeInfo, true)));
    }

    @Test
    public void hashTest() {
        String hash = LedgerYiUtils.of("asfsdf".getBytes());
        System.out.println(hash);
    }
}
