package cn.ledgeryi.contract.vm.program;

import cn.ledgeryi.chainbase.core.capsule.StorageRowCapsule;
import cn.ledgeryi.chainbase.core.store.StorageRowStore;
import cn.ledgeryi.common.runtime.vm.DataWord;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.crypto.utils.Hash;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import static java.lang.System.arraycopy;

public class Storage {
    private static final int PREFIX_BYTES = 16;
    @Getter
    private final Map<DataWord, StorageRowCapsule> rowCache = new HashMap<>();
    @Getter
    private byte[] addrHash;
    @Getter
    private StorageRowStore store;
    @Getter
    private byte[] address;

    public Storage(byte[] address, StorageRowStore store) {
        addrHash = addrHash(address);
        this.address = address;
        this.store = store;
    }

    public Storage(Storage storage) {
        this.addrHash = storage.addrHash.clone();
        this.address = storage.getAddress().clone();
        this.store = storage.store;
        storage.getRowCache().forEach((DataWord rowKey, StorageRowCapsule row) -> {
            StorageRowCapsule newRow = new StorageRowCapsule(row);
            this.rowCache.put(rowKey.clone(), newRow);
        });
    }

    private static byte[] compose(byte[] key, byte[] addrHash) {
        byte[] result = new byte[key.length];
        arraycopy(addrHash, 0, result, 0, PREFIX_BYTES);
        arraycopy(key, PREFIX_BYTES, result, PREFIX_BYTES, PREFIX_BYTES);
        return result;
    }

    // 32 bytes
    private static byte[] addrHash(byte[] address) {
        return Hash.sha3(address);
    }

    private static byte[] addrHash(byte[] address, byte[] txHash) {
        if (ByteUtil.isNullOrZeroArray(txHash)) {
            return Hash.sha3(address);
        }
        return Hash.sha3(ByteUtil.merge(address, txHash));
    }

    public void generateAddrHash(byte[] txId) {
        addrHash = addrHash(address, txId);
    }

    public DataWord getValue(DataWord key) {
        if (rowCache.containsKey(key)) {
            return new DataWord(rowCache.get(key).getValue());
        } else {
            StorageRowCapsule row = store.get(compose(key.getData(), addrHash));
            if (row == null || row.getInstance() == null) {
                return null;
            }
            rowCache.put(key, row);
            return new DataWord(row.getValue());
        }
    }

    public void put(DataWord key, DataWord value) {
        if (rowCache.containsKey(key)) {
            rowCache.get(key).setValue(value.getData());
        } else {
            byte[] rowKey = compose(key.getData(), addrHash);
            StorageRowCapsule row = new StorageRowCapsule(rowKey, value.getData());
            rowCache.put(key, row);
        }
    }

    public void commit() {
        rowCache.forEach((DataWord rowKey, StorageRowCapsule row) -> {
            if (row.isDirty()) {
                if (new DataWord(row.getValue()).isZero()) {
                    this.store.delete(row.getRowKey());
                } else {
                    this.store.put(row.getRowKey(), row);
                }
            }
        });
    }
}
