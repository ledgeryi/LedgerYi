package cn.ledgeryi.sdk.serverapi.data.stcc;

import lombok.Builder;
import lombok.Data;

/**
 * @author Brian
 * @date 2021/8/4 17:53
 */
@Data
@Builder
public class TriggerResult {
    private String txId;
    private String callResult;
}
