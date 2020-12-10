package cn.ledgeryi.chainbase.core.db;

import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.common.core.exception.BadItemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import cn.ledgeryi.common.utils.Sha256Hash;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j(topic = "DB")
@Component
public class BlockStore extends LedgerYiStoreWithRevoking<BlockCapsule> {

  @Autowired
  private BlockStore(@Value("block") String dbName) {
    super(dbName);
  }

  public List<BlockCapsule> getLimitNumber(long startNumber, long limit) {
    BlockCapsule.BlockId startBlockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, startNumber);
    return revokingDB.getValuesNext(startBlockId.getBytes(), limit).stream()
        .map(bytes -> {
          try {
            return new BlockCapsule(bytes);
          } catch (BadItemException ignored) {
          }
          return null;
        })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(BlockCapsule::getNum))
        .collect(Collectors.toList());
  }

  public List<BlockCapsule> getBlockByLatestNum(long getNum) {

    return revokingDB.getlatestValues(getNum).stream()
        .map(bytes -> {
          try {
            return new BlockCapsule(bytes);
          } catch (BadItemException ignored) {
          }
          return null;
        })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(BlockCapsule::getNum))
        .collect(Collectors.toList());
  }
}
