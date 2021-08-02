package cn.ledgeryi.sdk.serverapi.data.stcc;

import lombok.Builder;

/**
 * @author Brian
 * @date 2021/8/2 14:41
 */
@Builder
public class ContractBaseInfo {
    String nameEn;
    String nameZn;
    String address;
    String creator;
    String owner;
    String createTime;
    String uid;
}
