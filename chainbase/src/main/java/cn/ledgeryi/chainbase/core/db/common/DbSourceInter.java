package cn.ledgeryi.chainbase.core.db.common;

import java.util.Map;
import java.util.Set;


public interface DbSourceInter<V> extends BatchSourceInter<byte[], V>,
    Iterable<Map.Entry<byte[], V>> {

  String getDBName();

  void setDBName(String name);

  void initDB();

  boolean isAlive();

  void closeDB();

  void resetDb();

  Set<byte[]> allKeys() throws RuntimeException;

  Set<byte[]> allValues() throws RuntimeException;

  long getTotal() throws RuntimeException;

}
