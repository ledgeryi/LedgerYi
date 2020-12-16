package cn.ledgeryi.chainbase.common.utils;

import cn.ledgeryi.chainbase.core.capsule.AccountCapsule;
import cn.ledgeryi.chainbase.core.store.AccountStore;
import cn.ledgeryi.common.core.exception.BalanceInsufficientException;
import cn.ledgeryi.common.utils.DecodeUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "AdjustBalanceUtil")
public class AdjustBalanceUtil {

  public static void adjustBalance(AccountStore accountStore, byte[] accountAddress, long amount) throws BalanceInsufficientException {
    AccountCapsule account = accountStore.getUnchecked(accountAddress);
    adjustBalance(accountStore, account, amount);
  }

  private static void adjustBalance(AccountStore accountStore, AccountCapsule account, long amount) throws BalanceInsufficientException {
    long balance = account.getBalance();
    if (amount == 0) {
      return;
    }
    if (amount < 0 && balance < -amount) {
      throw new BalanceInsufficientException(DecodeUtil.createReadableString(account.createDbKey()) + " insufficient balance");
    }
    account.setBalance(Math.addExact(balance, amount));
    accountStore.put(account.getAddress().toByteArray(), account);
  }
}
