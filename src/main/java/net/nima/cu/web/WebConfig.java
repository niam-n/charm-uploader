package net.nima.cu.web;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public final class WebConfig {
    @ConfigEntry.Gui.Tooltip
    public String charm_server_address = "http://localhost:8080";
}