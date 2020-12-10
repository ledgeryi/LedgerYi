package cn.ledgeryi.chainbase.core.db.common;

import cn.ledgeryi.chainbase.common.storage.WriteOptionsWrapper;

import java.util.Map;


public interface BatchSourceInter<K, V> extends SourceInter<K, V> {

  void updateByBatch(Map<K, V> rows);

  void updateByBatch(Map<K, V> rows, WriteOptionsWrapper writeOptions);
}
