package cn.ledgeryi.common.runtime.vm;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import org.spongycastle.util.encoders.Hex;
import cn.ledgeryi.protos.Protocol.TransactionInfo.Log;
import cn.ledgeryi.common.utils.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LogInfo {

  private byte[] address = new byte[]{};
  private List<DataWord> topics = new ArrayList<>();
  private byte[] data = new byte[]{};

  public LogInfo(byte[] address, List<DataWord> topics, byte[] data) {
    this.address = (address != null) ? address : new byte[]{};
    this.topics = (topics != null) ? topics : new ArrayList<DataWord>();
    this.data = (data != null) ? data : new byte[]{};
  }

  public static Log buildLog(LogInfo logInfo) {
    List<ByteString> topics = Lists.newArrayList();
    logInfo.getTopics().forEach(topic -> {
      topics.add(ByteString.copyFrom(topic.getData()));
    });
    ByteString address = ByteString.copyFrom(logInfo.getAddress());
    ByteString data = ByteString.copyFrom(logInfo.getData());
    return Log.newBuilder().setAddress(address).addAllTopics(topics).setData(data).build();
  }

  public byte[] getAddress() {
    return address;
  }

  public List<DataWord> getTopics() {
    return topics;
  }

  public List<String> getHexTopics() {
    List<String> list = new LinkedList<>();
    if (topics != null && !topics.isEmpty()) {
      for (DataWord bytes : topics) {
        list.add(bytes.toHexString());
      }
    }
    return list;
  }

  public List<byte[]> getClonedTopics() {
    List<byte[]> list = new LinkedList<>();
    if (topics != null && topics.size() > 0) {
      for (DataWord dataword : topics) {
        list.add(dataword.getClonedData());
      }
    }
    return list;
  }

  public String getHexData() {
    return Hex.toHexString(data);
  }

  public byte[] getClonedData() {
    return ByteUtil.cloneBytes(data);
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public String toString() {

    StringBuilder topicsStr = new StringBuilder();
    topicsStr.append("[");

    for (DataWord topic : topics) {
      String topicStr = Hex.toHexString(topic.getData());
      topicsStr.append(topicStr).append(" ");
    }
    topicsStr.append("]");

    return "LogInfo{"
        + "address=" + Hex.toHexString(address)
        + ", topics=" + topicsStr
        + ", data=" + Hex.toHexString(data)
        + '}';
  }

}
