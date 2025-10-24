package net.nima.cu.charm.utils;

public enum CharmTypeParser {
    ;
    public static String getCharmType(String val) {
        if (!val.contains(",")) {
            return val;
        }

        String[] parts = val.split(",");
        return parts[parts.length - 1].substring(1);
    }
}