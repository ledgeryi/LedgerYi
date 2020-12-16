package cn.ledgeryi.chainbase.core.store;

import cn.ledgeryi.chainbase.common.utils.AdjustBalanceUtil;
import cn.ledgeryi.chainbase.core.db.LedgerYiStoreWithRevoking;
import cn.ledgeryi.chainbase.core.db.accountstate.AccountStateCallBackUtils;
import cn.ledgeryi.common.utils.DecodeUtil;
import com.typesafe.config.ConfigObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import cn.ledgeryi.chainbase.core.capsule.AccountCapsule;

@Slf4j(topic = "DB")
@Component
public class AccountStore extends LedgerYiStoreWithRevoking<AccountCapsule> {

  private static Map<String, byte[]> assertsAddress = new HashMap<>(); // key = name , value = address

  @Autowired
  private AccountStateCallBackUtils accountStateCallBackUtils;

  @Autowired
  private AccountStore(@Value("account") String dbName) {
    super(dbName);
  }

  public static void setAccount(com.typesafe.config.Config config) {
    List list = config.getObjectList("genesis.block.assets");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = (ConfigObject) list.get(i);
      String accountName = obj.get("accountName").unwrapped().toString();
      byte[] address = DecodeUtil.decode(obj.get("address").unwrapped().toString());
      assertsAddress.put(accountName, address);
    }
  }

  @Override
  public AccountCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountCapsule(value);
  }

  @Override
  public void put(byte[] key, AccountCapsule item) {
    super.put(key, item);
    accountStateCallBackUtils.accountCallBack(key, item);
  }

  /**
   * Min TX account.
   */
  public AccountCapsule getBlackhole() {
    return getUnchecked(assertsAddress.get("burn"));
  }


  @Override
  public void close() {
    super.close();
  }
}
