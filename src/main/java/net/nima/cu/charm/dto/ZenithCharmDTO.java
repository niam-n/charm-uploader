package net.nima.cu.charm.dto;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ZenithCharmDTO(
        @NotNull
        String name,

        @NotNull
        Long uuid,

        @NotNull
        Integer rarity,

        @NotNull
        Boolean upgraded,

        @NotNull
        Integer budget,

        @NotNull
        String charmType,

        @NotNull
        Integer charmPower,

        @NotNull
        String baseItem,

        @NotNull
        List<ZenithCharmEffectDTO> effects
) {
}