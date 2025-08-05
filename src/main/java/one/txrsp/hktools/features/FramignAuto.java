package one.txrsp.hktools.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import one.txrsp.hktools.utils.Utils;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

import static one.txrsp.hktools.HKTools.LOGGER;

public class FramignAuto {
    private static KeyBinding keybind;
    private static boolean wasActive = false;
    private static int direction = 0;
    private static Double preLockSens = 0.00001;

    public static void init() {
        keybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hktools.key_framign",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_CONTROL,
                "category.hktools"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) return;
            if (!Utils.isInGarden()) return;

            if (keybind.isPressed()) {
                wasActive = true;

                if (preLockSens != 0.0 && client.options.getMouseSensitivity().getValue() != 0.0) preLockSens = client.options.getMouseSensitivity().getValue();
                client.options.getMouseSensitivity().setValue(0.0);

                if (Objects.equals(getBlockLeft().getTranslationKey(), "block.minecraft.sea_lantern")) {
                    direction = 1;
                }
                else if (Objects.equals(getBlockRight().getTranslationKey(), "block.minecraft.sea_lantern")) {
                    direction = -1;
                }
                else if (Objects.equals(getBlockLeft().getTranslationKey(), "block.minecraft.glowstone") || Objects.equals(getBlockRight().getTranslationKey(), "block.minecraft.glowstone")) {
                    client.options.forwardKey.setPressed(false);
                    client.options.leftKey.setPressed(false);
                    client.options.rightKey.setPressed(false);
                    client.options.attackKey.setPressed(false);

                    if (direction != 0) client.player.networkHandler.sendChatCommand("warp garden");

                    direction = 0;
                }

                if (direction == -1) {
                    client.options.forwardKey.setPressed(true);
                    client.options.attackKey.setPressed(true);
                    client.options.leftKey.setPressed(true);
                    client.options.rightKey.setPressed(false);
                }
                else if (direction == 1) {
                    client.options.forwardKey.setPressed(true);
                    client.options.attackKey.setPressed(true);
                    client.options.rightKey.setPressed(true);
                    client.options.leftKey.setPressed(false);
                }
            }
            else if (wasActive) {
                client.options.getMouseSensitivity().setValue(preLockSens);

                client.options.forwardKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.attackKey.setPressed(false);

                wasActive = false;
            }
        });
    }

    private static BlockPos getBlockOffsetPerpendicular(int direction) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) return BlockPos.ORIGIN;

        float yaw = client.player.getYaw() + (direction * 90);
        double yawRad = Math.toRadians(yaw);

        double offsetX = Math.round(-Math.sin(yawRad)) / 1.5f;
        double offsetZ = Math.round(Math.cos(yawRad)) / 1.5f;

        Vec3d playerPos = client.player.getPos();
        double x = playerPos.x + offsetX;
        double y = playerPos.y + 0.2;
        double z = playerPos.z + offsetZ;

        return BlockPos.ofFloored(x, y, z);
    }

    private static Block getBlockLeft() {
        BlockPos pos = getBlockOffsetPerpendicular(-1);
        return MinecraftClient.getInstance().world.getBlockState(pos).getBlock();
    }

    private static Block getBlockRight() {
        BlockPos pos = getBlockOffsetPerpendicular(1);
        return MinecraftClient.getInstance().world.getBlockState(pos).getBlock();
    }
}
