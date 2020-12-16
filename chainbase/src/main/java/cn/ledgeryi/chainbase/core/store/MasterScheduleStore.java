package cn.ledgeryi.chainbase.core.store;

import cn.ledgeryi.chainbase.core.capsule.BytesCapsule;
import cn.ledgeryi.chainbase.core.db.LedgerYiStoreWithRevoking;
import cn.ledgeryi.common.utils.DecodeUtil;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import cn.ledgeryi.common.utils.ByteArray;

@Slf4j(topic = "DB")
@Component
public class MasterScheduleStore extends LedgerYiStoreWithRevoking<BytesCapsule> {

  private static final byte[] ACTIVE_MASTERS = "active_masters".getBytes();

  private static final int ADDRESS_BYTE_ARRAY_LENGTH = DecodeUtil.ADDRESS_SIZE / 2;

  @Autowired
  private MasterScheduleStore(@Value("master_schedule") String dbName) {
    super(dbName);
  }

  private void saveData(byte[] species, List<ByteString> mastersAddressList) {
    byte[] ba = new byte[mastersAddressList.size() * ADDRESS_BYTE_ARRAY_LENGTH];
    int i = 0;
    for (ByteString address : mastersAddressList) {
      System.arraycopy(address.toByteArray(), 0, ba, i * ADDRESS_BYTE_ARRAY_LENGTH, ADDRESS_BYTE_ARRAY_LENGTH);
      i++;
    }
    this.put(species, new BytesCapsule(ba));
  }

  private List<ByteString> getData(byte[] species) {
    List<ByteString> mastersAddressList = new ArrayList<>();
    return Optional.ofNullable(getUnchecked(species))
        .map(BytesCapsule::getData)
        .map(ba -> {
          int len = ba.length / ADDRESS_BYTE_ARRAY_LENGTH;
          for (int i = 0; i < len; ++i) {
            byte[] b = new byte[ADDRESS_BYTE_ARRAY_LENGTH];
            System.arraycopy(ba, i * ADDRESS_BYTE_ARRAY_LENGTH, b, 0, ADDRESS_BYTE_ARRAY_LENGTH);
            mastersAddressList.add(ByteString.copyFrom(b));
          }
          //log.info("getMasters:" + ByteArray.toStr(species) + mastersAddressList);
          return mastersAddressList;
        }).orElseThrow(
            () -> new IllegalArgumentException(
                "not found " + ByteArray.toStr(species) + "Masters"));
  }

  public void saveActiveMasters(List<ByteString> mastersAddressList) {
    saveData(ACTIVE_MASTERS, mastersAddressList);
  }

  public List<ByteString> getActiveMasters() {
    return getData(ACTIVE_MASTERS);
  }
}
