package one.txrsp.athena.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static one.txrsp.athena.Athena.LOGGER;

public class KeyPressHelper {
    private static final Map<KeyBinding, Integer> keysToRelease = new HashMap<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            keysToRelease.replaceAll((k, v) -> v - 1);

            keysToRelease.entrySet().removeIf(e -> {
               if (e.getValue() == 3) {
                   e.getKey().setPressed(false);
               } else if (e.getValue() <= 0) {
                   return true;
               } else {
                   e.getKey().setPressed(true);
               }
                return false;
            });
        });
    }

    public static void pressOnce(KeyBinding key, int duration) {
        if (keysToRelease.containsKey(key)) return;

        key.setPressed(true);
        keysToRelease.put(key, duration + 3);
    }
}
