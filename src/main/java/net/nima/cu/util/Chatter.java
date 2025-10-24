package net.nima.cu.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class Chatter {

    private final String cls_name;
    private final MinecraftClient mc;
    private final StringBuilder buf = new StringBuilder();

    public Chatter(Class<?> cls, MinecraftClient mc) {
        cls_name = cls.getSimpleName();
        this.mc = mc;
    }

    public void info(String s) {
        appendLocation();
        buf.append("INFO] ");
        appendMessage(s);
    }

    public void debug(String s) {
        appendLocation();
        buf.append("DEBUG] ");
        appendMessage(s);
    }

    public void warn(String s) {
        appendLocation();
        buf.append("§eWARN§7] ");
        appendMessage(s);
    }

    public void error(String s) {
        appendLocation();
        buf.append("§4ERROR§7] ");
        appendMessage(s);
    }

    private void appendLocation() {
        buf.append("§7[nima/");
    }

    private void appendMessage(String s) {
        buf.append(s);
        printToChat();
    }

    private void printToChat() {
        if (mc != null && mc.player != null) {
            Text text = Text.literal(buf.toString());
            mc.inGameHud.getChatHud().addMessage(text);
        }
        buf.setLength(0);
    }
}