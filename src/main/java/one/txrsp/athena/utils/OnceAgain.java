package one.txrsp.athena.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import one.txrsp.athena.Athena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static one.txrsp.athena.Athena.LOGGER;

public class OnceAgain {
    private static boolean swingHandInProgress = false;
    private static int swingHandTicks;
    private static boolean noSneak = false;

    private static final Map<String, Integer> cooldowns = new HashMap<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (swingHandTicks == 0) {
                if (noSneak && !client.player.isSneaking()) client.player.swingHand(client.player.getActiveHand());
                swingHandInProgress = false;
                noSneak = false;
            }
            swingHandTicks--;

            Iterator<Map.Entry<String, Integer>> iterator = cooldowns.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Integer> entry = iterator.next();
                int time = entry.getValue() - 1;
                if (time <= 0) iterator.remove();
                else entry.setValue(time);
            }
        });
    }

    public static void swingHandWithCooldown(int ticks, boolean noSneak) {
        if (!swingHandInProgress) {
            LOGGER.info(String.valueOf(swingHandInProgress));
            MinecraftClient.getInstance().player.swingHand(MinecraftClient.getInstance().player.getActiveHand());
            swingHandTicks = ticks;
            swingHandInProgress = true;
            OnceAgain.noSneak = noSneak;
        }
    }

    public static void executeWithCooldown(String key, int cooldownTicks, Runnable action) {
        if (cooldowns.containsKey(key)) return;

        try {
            action.run();
        } catch (Exception e) {
            LOGGER.error("Error executing cooldown action for key " + key, e);
        }

        cooldowns.put(key, cooldownTicks);
    }
}
