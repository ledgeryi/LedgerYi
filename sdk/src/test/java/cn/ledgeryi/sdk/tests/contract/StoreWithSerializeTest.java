package cn.ledgeryi.sdk.tests.contract;

import cn.ledgeryi.sdk.serverapi.data.TriggerContractReturn;
import cn.ledgeryi.sdk.tests.AbstractContractTest;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.Strings;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StoreWithSerializeTest extends AbstractContractTest {
    private static final String INIT_SYMBOL = "BSC";

    @Override
    protected String getPrivateKey() {
        return "e8b5177d5a69898dcc437d0e96a9343a37bac001cb9bec7a90b660eee66b4587";
    }

    @Override
    protected String getOwnerAddress() {
        return "ada95a8734256b797efcd862e0b208529283ac56";
    }

    @Override
    protected String getDeployedOwnerAddress() {
        return "9944efe20e4c7e13eb73919ede783e7de764e91c";
    }

    @Before
    public void init() {
        super.init();
        waitFiveSecondToCompileAndDeployContract(Paths.get("src","test","resources","StoreWithSerialize.sol"),
                "StoreWithSerialize",
                "constructor(string)",
                Lists.newArrayList(INIT_SYMBOL));
    }

    @Test
    public void testSetSymbolWithSpeicialCharacter() {
        Map<String, String> json = new HashMap<>();
        json.put("unit", "new_BSC");

        assertEqualsSetSymbolPerFiveSecond(writeObjectToJason(json));
        assertEqualsSetSymbolPerFiveSecond(writeObjectToJason(Arrays.asList("a,b,c")));
        assertEqualsSetSymbolPerFiveSecond("{\"unit\":\"new_BSC\"}");
        assertEqualsSetSymbolPerFiveSecond(",,,");
        assertEqualsSetSymbolPerFiveSecond("abd");
        assertEqualsSetSymbolPerFiveSecond("{abc}");
        assertEqualsSetSymbolPerFiveSecond("{abc}");
    }

    @SneakyThrows
    private void assertEqualsSetSymbolPerFiveSecond(String param) {
        //set
        triggerContract("setSymbol(string)", Arrays.asList(param),false);

        //wait board transaction
        Thread.sleep(5000);

        //get
        TriggerContractReturn result = triggerContract("getSymbol()", Arrays.asList(), true);

        //should be not null & equals
        Assert.assertNotNull(result.getCallResult());
        Assert.assertEquals(param,  Strings.fromByteArray(result.getCallResult().toByteArray()).trim());
    }
}
