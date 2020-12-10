package cn.ledgeryi.chainbase.core.db2.core;

import cn.ledgeryi.common.core.exception.BadItemException;
import cn.ledgeryi.common.core.exception.ItemNotFoundException;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map.Entry;
import cn.ledgeryi.common.utils.Quitable;

public interface ILedgerYiBase<T> extends Iterable<Entry<byte[], T>>, Quitable {

  /**
   * reset the database.
   */
  void reset();

  /**
   * close the database.
   */
  void close();

  void put(byte[] key, T item);

  void delete(byte[] key);

  T get(byte[] key) throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException, ItemNotFoundException, BadItemException;

  T getUnchecked(byte[] key);

  boolean has(byte[] key);

  String getName();

  String getDbName();

}
