package net.nima.cu;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.nima.cu.charm.CharmConfig;
import net.nima.cu.web.WebConfig;

@Config(name = "charm-uploader")
public class CharmUploaderConfig implements ConfigData {
    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public CharmConfig charm = new CharmConfig();
    public WebConfig web = new WebConfig();
}