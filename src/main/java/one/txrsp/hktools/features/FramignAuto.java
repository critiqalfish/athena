package one.txrsp.hktools.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import one.txrsp.hktools.config.HKConfig;
import one.txrsp.hktools.pathfinding.PathFollower;
import one.txrsp.hktools.utils.Utils;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static one.txrsp.hktools.HKTools.DEBUG;
import static one.txrsp.hktools.HKTools.LOGGER;
import static one.txrsp.hktools.render.RenderUtils.renderBlockMark;

public class FramignAuto {
    private static KeyBinding keybind;
    private static boolean active = false;
    private static boolean wasActive = false;
    private static BlockPos stoppedBlock = null;
    private static List<String> heldKeys = new ArrayList<>();
    public static List<String> actionPointsList = new ArrayList<>();
    public static Map<String, KeyBinding> keybindsTranslation;
    private static Vec3d lastPos = null;
    private static int stillTicks = 0;
    private static boolean isPestRemoving = false;
    private static boolean wasPestRemoving = false;
    private static int originPlot;
    private static int originSlot;
    private static BlockPos originBlock;
    private static int currentPlot;
    private static boolean waitForTP = false;

    public static void init() {
        actionPointsList.add("empty");

        keybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hktools.key_framign",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_CONTROL,
                "category.hktools"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keybindsTranslation == null && client.options != null) {
                keybindsTranslation = Map.of(
                        "W", client.options.forwardKey,
                        "A", client.options.leftKey,
                        "S", client.options.backKey,
                        "D", client.options.rightKey
                );
            }
            if (client.player == null || client.currentScreen != null) return;
            if (actionPointsList.contains("empty")) {
                actionPointsList.clear();
                Collections.addAll(actionPointsList, HKConfig.actionPoints);
            }
            if (!DEBUG) {
                if (!Utils.isInGarden()) {stoppedBlock = null; return;}
            }

            List<String> lines = Utils.getScoreboardLines();
            for (String line : lines) {
                if (line.contains("Plot - ")) {
                    line = line.replace("Plot - ", "");
                    if (line.contains("ൠ")) {
                        line = line.replace(" ൠ ", "");
                        line = line.substring(0, line.lastIndexOf("x"));
                    }
                    if (line.contains("⏣")) line = line.replace("⏣", "");
                    currentPlot = Integer.parseInt(line.strip());
                }
            }

            Vec3d currentPos = client.player.getPos();

            if (keybind.wasPressed()) active = !active;

            if (active) {
                if (!wasActive) {
                    client.inGameHud.getChatHud().addMessage(Text.literal("[HKTools] ").formatted(Formatting.LIGHT_PURPLE).append(Text.literal("farmer working now :)").formatted(Formatting.WHITE)));
                    client.options.pauseOnLostFocus = false;
                    stoppedBlock = null;
                    waitForTP = false;
                }

                if (lastPos != null && currentPos.distanceTo(lastPos) < 0.001) {
                    stillTicks++;
                }
                else {
                    stillTicks = 0;
                }

                lastPos = currentPos;

                if (PestESP.totalPests >= HKConfig.autoPestThreshold && !isPestRemoving) {
                    isPestRemoving = true;
                    originPlot = currentPlot;
                    originSlot = client.player.getInventory().getSelectedSlot();
                    originBlock = client.player.getBlockPos();
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = client.player.getInventory().getStack(i);
                        if (!stack.isEmpty() && stack.getName().getString().contains("Vacuum")) {
                            client.player.getInventory().setSelectedSlot(i);
                            break;
                        }
                    }

                    client.options.forwardKey.setPressed(false);
                    client.options.leftKey.setPressed(false);
                    client.options.rightKey.setPressed(false);
                    client.options.backKey.setPressed(false);
                    client.options.attackKey.setPressed(false);
                }

                if (isPestRemoving) {
                    if (!PestESP.pestPlots.isEmpty()) {
                        if (!waitForTP && currentPlot != PestESP.pestPlots.getFirst()) {
                            waitForTP = true;
                            PathFollower.stop();
                            client.player.networkHandler.sendChatCommand("plottp " + PestESP.pestPlots.getFirst());
                        } else if (currentPlot == PestESP.pestPlots.getFirst()) {
                            waitForTP = false;

                            client.options.useKey.setPressed(true);

                            if (!PathFollower.following) {
                                if (!PestESP.pests.isEmpty()) {
                                    Box box = PestESP.pests.getFirst();
                                    BlockPos pestPos = BlockPos.ofFloored(
                                            (box.minX + box.maxX) / 2.0,
                                            box.maxY + 8,
                                            (box.minZ + box.maxZ) / 2.0
                                    );
                                    PathFollower.pathfind(pestPos);
                                } else {
                                    PathFollower.stop();
                                }
                            }
                        } else if (currentPlot != PestESP.pestPlots.getFirst()) {
                            client.options.forwardKey.setPressed(true);
                        }
                    } else {
                        PestESP.totalPests = 0;
                        isPestRemoving = false;
                        waitForTP = false;
                        PathFollower.stop();
                        client.options.useKey.setPressed(false);
                        client.options.forwardKey.setPressed(false);
                        client.player.networkHandler.sendChatCommand("plottp " + originPlot);
                        client.player.getInventory().setSelectedSlot(originSlot);
                        wasPestRemoving = true;
                    }
                }

                if (wasPestRemoving) {
                    if (PathFollower.found) {
                        PathFollower.stop();
                        client.options.sneakKey.setPressed(true);
                        wasPestRemoving = false;
                    }
                    if (currentPlot == originPlot) {
                         if (!PathFollower.following) {
                            if (Utils.allVisibleChunksLoaded(client)) {
                                client.options.forwardKey.setPressed(false);
                                PathFollower.pathfind(originBlock);
                            }
                        }
                    } else {
                        client.options.forwardKey.setPressed(true);
                    }
                } else if (!isPestRemoving) {
                    if (client.player.getAbilities().flying) client.options.sneakKey.setPressed(true);
                    else client.options.sneakKey.setPressed(false);

                    lerpRotationTo(HKConfig.yaw, HKConfig.pitch);

                    for (String s : HKConfig.actionPoints) {
                        if (s.startsWith(client.player.getBlockPos().toString())) {
                            if (!wasActive || stillTicks >= 1) {
                                String keys = s.substring(s.lastIndexOf("}") + 1);
                                heldKeys.clear();
                                for (char k : keys.toCharArray()) {
                                    heldKeys.add(String.valueOf(k));
                                }
                                client.options.forwardKey.setPressed(false);
                                client.options.leftKey.setPressed(false);
                                client.options.rightKey.setPressed(false);
                                client.options.backKey.setPressed(false);
                            }
                        }
                    }

                    if (Math.abs(MathHelper.wrapDegrees(client.player.getYaw() - HKConfig.yaw)) < 15) {
                        for (String key : heldKeys) {
                            if (Objects.equals(key, ".")) {
                                client.player.networkHandler.sendChatCommand("warp garden");
                                break;
                            }
                            keybindsTranslation.get(key).setPressed(true);
                            client.options.attackKey.setPressed(true);
                        }
                    }
                }

                wasActive = true;
            }
            else if (wasActive) {
                client.options.forwardKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.backKey.setPressed(false);
                client.options.attackKey.setPressed(false);

                PathFollower.stop();

                wasActive = false;
                stoppedBlock = client.player.getBlockPos();
                client.options.pauseOnLostFocus = true;

                client.inGameHud.getChatHud().addMessage(Text.literal("[HKTools] ").formatted(Formatting.LIGHT_PURPLE).append(Text.literal("farmer stopped working :(").formatted(Formatting.WHITE)));
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;
            if (!DEBUG) {
                if (!Utils.isInGarden()) return;
            }

            if (stoppedBlock != null) renderBlockMark(context.matrixStack(), stoppedBlock, 1f, 0f, 0.2f, 0.9f, false, "");

            if (HKConfig.showActionPoints && !actionPointsList.contains("empty")) {
                for (String p : actionPointsList) {
                    String[] parts = p.substring(p.indexOf("{") + 1, p.lastIndexOf("}")).split(", ");
                    Vec3i coords = new Vec3i(Integer.parseInt(parts[0].split("=")[1]), Integer.parseInt(parts[1].split("=")[1]), Integer.parseInt(parts[2].split("=")[1]));
                    BlockPos pos = new BlockPos(coords);
                    String text = p.substring(p.lastIndexOf("}") + 1);
                    text = Objects.equals(text, ".") ? "warp" : text;
                    renderBlockMark(context.matrixStack(), pos, 0.2f, 0.3f, 1f, 0.5f, false, text);
                }
            }
        });
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
