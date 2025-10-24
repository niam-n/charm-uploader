package net.nima.cu.charm;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public final class CharmConfig {
    @ConfigEntry.Gui.Tooltip
    public boolean charm_report_enable = true;
}