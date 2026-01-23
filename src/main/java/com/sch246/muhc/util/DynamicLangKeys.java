package com.sch246.muhc.util;

import com.sch246.muhc.MaidUseHandCrank;
import net.minecraft.locale.Language;

import java.util.*;

public class DynamicLangKeys {
    private static volatile String[] CHAT_BUBBLES;
    private static final Object LOCK = new Object();

    // 与你当前语言文件保持一致：working 1..50, master 1..13, master2 1..1
    private static final int WORKING_COUNT = 50;
    private static final int MASTER_COUNT = 13;
    private static final int MASTER2_COUNT = 1;

    public static String[] getChatBubbles() {
        String[] cached = CHAT_BUBBLES;
        if (cached != null && cached.length > 0) {
            return cached;
        }
        synchronized (LOCK) {
            if (CHAT_BUBBLES == null || CHAT_BUBBLES.length == 0) {
                CHAT_BUBBLES = buildChatBubbles();
            }
            return CHAT_BUBBLES;
        }
    }

    /**
     * 预留：资源重载/语言切换后可调用，让下次 getChatBubbles() 重新构建。
     * 不过客户端重载后服务端好像也不会被重载
     */
    public static void invalidateCache() {
        synchronized (LOCK) {
            CHAT_BUBBLES = null;
        }
    }

    private static String[] buildChatBubbles() {
        String base = "message." + MaidUseHandCrank.MODID + ".";

        List<String> keys = new ArrayList<>(WORKING_COUNT + MASTER_COUNT + MASTER2_COUNT);
        for (int i = 1; i <= WORKING_COUNT; i++) keys.add(base + "working." + i);
        for (int i = 1; i <= MASTER_COUNT; i++) keys.add(base + "master." + i);
        for (int i = 1; i <= MASTER2_COUNT; i++) keys.add(base + "master2." + i);

        return keys.toArray(new String[0]);
    }
}
