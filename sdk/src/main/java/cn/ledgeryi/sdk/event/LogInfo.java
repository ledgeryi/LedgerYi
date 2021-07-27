/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package cn.ledgeryi.sdk.event;

import cn.ledgeryi.protos.Protocol;
import cn.ledgeryi.sdk.common.utils.ByteUtil;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Roman Mandeleil
 * @since 19.11.2014
 */
public class LogInfo {

  private byte[] address = new byte[]{};
  private List<DataWord> topics = new ArrayList<>();
  private byte[] data = new byte[]{};

  public LogInfo(byte[] address, List<DataWord> topics, byte[] data) {
    this.address = (address != null) ? address : new byte[]{};
    this.topics = (topics != null) ? topics : new ArrayList<>();
    this.data = (data != null) ? data : new byte[]{};
  }

  public static Protocol.TransactionInfo.Log buildLog(LogInfo logInfo) {
    List<ByteString> topics = Lists.newArrayList();
    logInfo.getTopics().forEach(topic -> {
      topics.add(ByteString.copyFrom(topic.getData()));
    });
    ByteString address = ByteString.copyFrom(logInfo.getAddress());
    ByteString data = ByteString.copyFrom(logInfo.getData());
    return Protocol.TransactionInfo.Log.newBuilder().setAddress(address).addAllTopics(topics).setData(data).build();
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
