package net.nima.cu.charm.dto;

import java.util.List;

public record RequestDTO(
        List<ZenithCharmDTO> data,
        String reporter,
        String shard,
        List<Integer> at,
        String type
) {
}