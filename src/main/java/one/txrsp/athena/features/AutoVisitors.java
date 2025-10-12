package one.txrsp.athena.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class AutoVisitors {
    public static boolean doingVisitors = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

        });
    }

    public static void doVisitors() {
        doingVisitors = true;
    }
}
