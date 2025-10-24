package net.nima.cu.charm.utils;

public enum CharmRarityMapper {
    ;
    public static Integer getRarity(String val) {
        switch (val) {
            case "Negative Legendary" -> {
                return -5;
            }
            case "Negative Epic" -> {
                return -4;
            }
            case "Negative Rare" -> {
                return -3;
            }
            case "Negative Uncommon" -> {
                return -2;
            }
            case "Negative Common" -> {
                return -1;
            }
            case "Common" -> {
                return 1;
            }
            case "Uncommon" -> {
                return 2;
            }
            case "Rare" -> {
                return 3;
            }
            case "Epic" -> {
                return 4;
            }
            case "Legendary" -> {
                return 5;
            }
            default -> {
                return 0;
            }
        }
    }
}