package one.txrsp.hktools.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import one.txrsp.hktools.utils.Utils;
import org.lwjgl.glfw.GLFW;

public class FramignClassic {
    private static KeyBinding keyL;
    private static KeyBinding keyR;
    private static KeyBinding keyWarp;
    private static boolean wasActive = false;

    public static void init() {
        keyL = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hktools.key_l",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT,
                "category.hktools"
        ));

        keyR = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hktools.key_r",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                "category.hktools"
        ));

        keyWarp = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hktools.key_warp",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN,
                "category.hktools"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!Utils.isInGarden()) return;
            if (client.player == null || client.currentScreen != null) return;

            if (keyWarp.wasPressed()) client.player.networkHandler.sendChatCommand("warp garden");

            boolean holdingL = keyL.isPressed();
            boolean holdingR = keyR.isPressed();
            boolean featureActive = holdingL || holdingR;

            if (featureActive) {
                wasActive = true;

                client.options.forwardKey.setPressed(true);
                client.options.attackKey.setPressed(true);

                if (holdingL) {
                    client.options.leftKey.setPressed(true);
                }
                else if (holdingR) {
                    client.options.rightKey.setPressed(true);
                }
            }
            else if (wasActive) {
                client.options.forwardKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.attackKey.setPressed(false);

                wasActive = false;
            }
        });
    }
}
