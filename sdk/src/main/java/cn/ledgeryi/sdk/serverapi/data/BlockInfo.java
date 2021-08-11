package cn.ledgeryi.sdk.serverapi.data;

import lombok.Builder;
import lombok.Data;

/**
 * @author Brian
 * @date 2021/8/11 15:48
 */
@Data
@Builder
public class BlockInfo {
    private String parentHash;
    private String hash;
    private long number;
    private int txSize;
    private long size;
    private long timestamp;
}
