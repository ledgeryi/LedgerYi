package cn.ledgeryi.chainbase.core.store;

import cn.ledgeryi.chainbase.core.capsule.BytesCapsule;
import cn.ledgeryi.chainbase.core.db.LedgerYiStoreWithRevoking;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StorageConsumeStore extends LedgerYiStoreWithRevoking<BytesCapsule> {

    @Autowired
    public StorageConsumeStore(@Value("storage-consume") String dbName) {
        super(dbName);
    }

    public void put(BytesCapsule key,BytesCapsule value) {
        put(key.getData(), new BytesCapsule(value.getData()));
    }

    @Override
    public BytesCapsule get(byte[] key) {
        byte[] value = revokingDB.getUnchecked(key);
        if (ArrayUtils.isEmpty(value)) {
            return null;
        }
        return new BytesCapsule(value);
    }

    @Override
    public boolean has(byte[] key) {
        byte[] value = revokingDB.getUnchecked(key);
        return !ArrayUtils.isEmpty(value);
    }
}
