package com.sch246.muhc.util;

import com.sch246.muhc.MaidUseHandCrank;
import net.minecraft.locale.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DynamicLangKeys {
    private static volatile String[] CHAT_BUBBLES;
    private static final Object lock = new Object();

    public static String[] getChatBubbles() {
        if (CHAT_BUBBLES == null) {
            synchronized (lock) {
                if (CHAT_BUBBLES == null) {
                    initializeChatBubbles();
                }
            }
        }
        return CHAT_BUBBLES;
    }

    private static void initializeChatBubbles() {
        Map<String, String> langData = Language.getInstance().getLanguageData();

        String[] prefixes = {
                "message." + MaidUseHandCrank.MODID + ".working.",
                "message." + MaidUseHandCrank.MODID + ".master.",
                "message." + MaidUseHandCrank.MODID + ".master2."
        };

        List<String> keys = new ArrayList<>();
        langData.keySet().stream()
                .filter(key ->
                        Arrays.stream(prefixes).anyMatch(key::startsWith))
                .forEach(keys::add);

        CHAT_BUBBLES = keys.toArray(new String[0]);
    }
}