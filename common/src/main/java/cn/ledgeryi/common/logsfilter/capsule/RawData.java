package cn.ledgeryi.common.logsfilter.capsule;

import cn.ledgeryi.common.runtime.vm.DataWord;
import lombok.Data;
import lombok.Getter;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

@Data
public class RawData {

  // for mongodb
  @Getter
  private String address;
  @Getter
  private List<DataWord> topics;
  @Getter
  private String data;

  public RawData(byte[] address, List<DataWord> topics, byte[] data) {
    this.address = (address != null) ? Hex.toHexString(address) : "";
    this.topics = (address != null) ? topics : new ArrayList<>();
    this.data = (data != null) ? Hex.toHexString(data) : "";
  }
}
