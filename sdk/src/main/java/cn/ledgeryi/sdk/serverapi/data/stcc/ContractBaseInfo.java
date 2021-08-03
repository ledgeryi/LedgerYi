package cn.ledgeryi.sdk.serverapi.data.stcc;

import lombok.Builder;
import lombok.ToString;

/**
 * @author Brian
 * @date 2021/8/2 14:41
 */
@Builder
@ToString
public class ContractBaseInfo {
    String nameEn;
    String nameZn;
    String address;
    String creator;
    String owner;
    String createTime;
    String uid;
}
