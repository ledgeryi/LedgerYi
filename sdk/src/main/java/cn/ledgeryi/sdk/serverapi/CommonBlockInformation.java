package cn.ledgeryi.sdk.serverapi;

import cn.ledgeryi.api.GrpcAPI;
import cn.ledgeryi.sdk.common.utils.ByteUtil;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@ToString
public class CommonBlockInformation {
    private final long blockNumber;
    private final String blockId;

    public static final CommonBlockInformation UN_FOUND_BLOCK = new CommonBlockInformation(-1, null);

    private CommonBlockInformation(long blockNumber, String blockId) {
        this.blockNumber = blockNumber;
        this.blockId = blockId;
    }

    public static CommonBlockInformation of(long blockNumber, ByteString blockIdByteString) {
        if (blockNumber < 0) {
            throw new IllegalArgumentException("blockNumber must be great than zero");
        }
        if (Objects.isNull(blockIdByteString)) {
            throw new IllegalArgumentException("blockId cannot be null or empty");
        }
        return new CommonBlockInformation(blockNumber, ByteUtil.toHexString(blockIdByteString.toByteArray()));
    }

    public static CommonBlockInformation of(GrpcAPI.BlockExtention blockExtension) {
        return of(blockExtension.getBlockHeader().getRawData().getNumber(), blockExtension.getBlockid());
    }

    public static List<CommonBlockInformation> of(GrpcAPI.BlockListExtention blockByLimitNext) {
        if (Objects.isNull(blockByLimitNext)) {
            return Lists.newArrayList();
        }

        return blockByLimitNext.getBlockList().stream()
                .map(CommonBlockInformation::of)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "{" +
                "blockNumber=" + blockNumber +
                ", blockId=\"" + blockId + '\"' +
                '}';
    }
}
