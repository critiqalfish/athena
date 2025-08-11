package one.txrsp.hktools.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import one.txrsp.hktools.config.HKConfig;
import one.txrsp.hktools.utils.Utils;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

import static one.txrsp.hktools.render.RenderUtils.renderBlockMark;

public class FramignAuto {
    private static KeyBinding keybind;
    private static boolean active = false;
    private static boolean wasActive = false;
    private static int direction = 0;
    private static BlockPos stoppedBlock = null;

    public static void init() {
        keybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hktools.key_framign",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_CONTROL,
                "category.hktools"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) return;
            if (!Utils.isInGarden()) {stoppedBlock = null; return;}

            if (keybind.wasPressed()) active = !active;

            if (active) {
                if (!wasActive) {
                    client.inGameHud.getChatHud().addMessage(Text.literal("[HKTools] ").formatted(Formatting.LIGHT_PURPLE).append(Text.literal("farmer working now :)").formatted(Formatting.WHITE)));
                    client.options.pauseOnLostFocus = false;
                    stoppedBlock = null;
                }
                wasActive = true;

                lerpRotationTo(HKConfig.yaw, HKConfig.pitch);

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
                client.options.forwardKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.attackKey.setPressed(false);

                wasActive = false;
                stoppedBlock = client.player.getBlockPos();
                client.options.pauseOnLostFocus = true;

                client.inGameHud.getChatHud().addMessage(Text.literal("[HKTools] ").formatted(Formatting.LIGHT_PURPLE).append(Text.literal("farmer stopped working :(").formatted(Formatting.WHITE)));
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;

            if (stoppedBlock != null) renderBlockMark(context.matrixStack(), stoppedBlock, 1f, 0f, 0.2f, 0.9f, false);
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

    private static void lerpRotationTo(float yaw, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        // 0.1f = 10% of delta per tick
        float speed = 0.1f;

        float newYaw = lerpAngle(currentYaw, yaw, speed);
        float newPitch = lerpAngle(currentPitch, pitch, speed);

        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);

        if (Math.abs(yaw - newYaw) < 0.5f && Math.abs(pitch - newPitch) < 0.5f) {
            client.player.setYaw(yaw);
            client.player.setPitch(pitch);
        }
    }

    private static float lerpAngle(float current, float target, float speed) {
        float delta = (((target - current + 540) % 360 + 360) % 360) - 180;
        return current + delta * speed;
    }
}
