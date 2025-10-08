package one.txrsp.athena.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import one.txrsp.athena.config.AthenaConfig;
import one.txrsp.athena.mixin.InGameHudAccessor;
import one.txrsp.athena.pathfinding.PathFollower;
import one.txrsp.athena.utils.Crops;
import one.txrsp.athena.utils.Utils;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static one.txrsp.athena.Athena.DEBUG;
import static one.txrsp.athena.Athena.LOGGER;
import static one.txrsp.athena.render.RenderUtils.renderBlockMark;

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
    private static boolean waitForWarp = false;
    private static boolean autoPestNextWarp = false;
    private static float yawVelocity = 0;
    private static float pitchVelocity = 0;
    private static boolean rotate = false;
    private static final List<Long> brokenCropTimestamps = new ArrayList<>();
    public static Crops.CROP currentCrop = null;
    private static long startTimestamp;
    private static float lastYaw = Float.NaN;
    private static float lastPitch = Float.NaN;
    private static double lastMouseX = Float.NaN;
    private static double lastMouseY = Float.NaN;
    private static boolean maybeMacroCheck = false;

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
            if (client.player == null) return;
            if (client.currentScreen != null) {
                startTimestamp = System.currentTimeMillis();
                return;
            }
            if (actionPointsList.contains("empty")) {
                actionPointsList.clear();
                Collections.addAll(actionPointsList, AthenaConfig.actionPoints);
            }
            if (!DEBUG) {
                if (!Utils.isInGarden()) {stoppedBlock = null; return;}
            }

            currentCrop = Crops.getCropForTool(client.player.getInventory().getSelectedStack().getName().getString());
            brokenCropTimestamps.removeIf(t -> System.currentTimeMillis() - t > 1000);

            List<String> lines = Utils.getScoreboardLines();
            for (String line : lines) {
                try {
                    if (line.contains("Plot - ")) {
                        line = line.replace("Plot - ", "");
                        if (line.contains("ൠ")) {
                            line = line.replace(" ൠ ", "");
                            if (line.lastIndexOf("x") != -1) {
                                line = line.substring(0, line.lastIndexOf("x"));
                            }
                            else {
                                line = line.replace(" ൠ", "");
                            }
                        }
                        if (line.contains("⏣")) line = line.replace("⏣", "");
                        try {
                            if (!line.strip().equals("n")) currentPlot = Integer.parseInt(line.strip());
                        } catch (NumberFormatException e) {
                            // fuck this shit
                        }
                    }
                } catch (Exception e) {
                    LOGGER.info("Failed to parse line: " + line, e);
                }
            }

            Vec3d currentPos = client.player.getPos();

            if (maybeMacroCheck) {
                client.inGameHud.setTitle(Text.literal("!!!  WARNING  !!!").formatted(Formatting.BOLD).formatted(Formatting.RED));
                client.inGameHud.setSubtitle(Text.literal("LOW BPS OR YAW/PITCH CHANGE DETECTED. POTENTIAL MACRO CHECK").formatted(Formatting.BOLD).formatted(Formatting.RED));
                client.inGameHud.setTitleTicks(0, 400, 0);
                client.player.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                        SoundCategory.MASTER,
                        1.0f,
                        1.0f);
                client.player.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                        SoundCategory.MASTER,
                        1.0f,
                        0.5f);
                client.player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_HURT,
                        SoundCategory.MASTER,
                        1.0f,
                        1.0f);
                client.player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        SoundCategory.MASTER,
                        1.0f,
                        1.5f);
            }
            else if (((InGameHudAccessor) MinecraftClient.getInstance().inGameHud).getCurrentTitle() != null && Objects.equals(((InGameHudAccessor) MinecraftClient.getInstance().inGameHud).getCurrentTitle().getString(), "!!!  WARNING  !!!")) {
                client.inGameHud.clearTitle();
            }

            if (keybind.wasPressed()) active = !active;

            if (active) {
                if (!wasActive) {
                    Utils.HKPrint(Text.literal("farmer working :)").formatted(Formatting.WHITE));
                    client.options.pauseOnLostFocus = false;
                    stoppedBlock = null;
                    waitForTP = false;
                    startTimestamp = System.currentTimeMillis();
                    maybeMacroCheck = false;
                }

                if (lastPos != null && currentPos.distanceTo(lastPos) < 0.001) {
                    stillTicks++;
                } else {
                    stillTicks = 0;
                }

                lastPos = currentPos;

                if (PestESP.totalPests >= AthenaConfig.autoPestThreshold && !isPestRemoving && AthenaConfig.autoPest) {
                    if (AthenaConfig.autoPestWarpWait) {
                        autoPestNextWarp = true;
                    } else {
                        initiateAutoPest(client);
                        Utils.HKPrint(Text.literal("killing pests").formatted(Formatting.WHITE));
                    }
                }

                if (isPestRemoving && AthenaConfig.autoPest) {
                    rotate = false;

                    if (!PestESP.pestPlots.isEmpty()) {
                        if (!waitForTP && currentPlot != PestESP.pestPlots.getFirst()) {
                            waitForTP = true;
                            PathFollower.stop();
                            client.player.networkHandler.sendChatCommand("plottp " + PestESP.pestPlots.getFirst());
                        } else if (currentPlot == PestESP.pestPlots.getFirst()) {
                            waitForTP = false;

                            if (!PestESP.pests.isEmpty() && currentPos.squaredDistanceTo(PestESP.pests.getFirst().getCenter()) < 144) {
                                client.options.useKey.setPressed(true);
                            }
                            else {
                                client.options.useKey.setPressed(false);
                            }

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
                            } else if (!PestESP.pests.isEmpty() && PathFollower.goal.getSquaredDistance(PestESP.pests.getFirst().getCenter()) > 144) {
                                Box box = PestESP.pests.getFirst();
                                BlockPos pestPos = BlockPos.ofFloored(
                                        (box.minX + box.maxX) / 2.0,
                                        box.maxY + 8,
                                        (box.minZ + box.maxZ) / 2.0
                                );
                                PathFollower.pathfind(pestPos);
                            }
                        } else if (currentPlot != PestESP.pestPlots.getFirst()) {
                            client.options.forwardKey.setPressed(true);
                        }
                    } else {
                        Utils.HKPrint(Text.literal("killed all pests").formatted(Formatting.WHITE));
                        PestESP.totalPests = 0;
                        isPestRemoving = false;
                        waitForTP = false;
                        PathFollower.stop();
                        client.options.useKey.setPressed(false);
                        client.options.forwardKey.setPressed(false);
                        client.player.getInventory().setSelectedSlot(originSlot);
                        if (AthenaConfig.autoPestWarpWait) {
                            wasPestRemoving = false;
                            client.player.networkHandler.sendChatCommand("warp garden");
                            startTimestamp = System.currentTimeMillis();
                        } else {
                            wasPestRemoving = true;
                            client.player.networkHandler.sendChatCommand("plottp " + originPlot);
                        }
                    }
                }

                if (wasPestRemoving) {
                    rotate = false;

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
                    // normal farming

                    if (brokenCropTimestamps.size() > 10 && !maybeMacroCheck && AthenaConfig.mcowRemind) {
                        Utils.getTablistLines().forEach(entry -> {
                            if (entry.startsWith("[Lvl") && !entry.contains("Mooshroom Cow")) {
                                client.inGameHud.setTitle(Text.literal("!!!  WARNING  !!!").formatted(Formatting.BOLD).formatted(Formatting.YELLOW));
                                client.inGameHud.setSubtitle(Text.literal("No Mooshroom Cow Pet selected.").formatted(Formatting.BOLD).formatted(Formatting.YELLOW));
                                client.inGameHud.setTitleTicks(0, 10, 0);
                            }
                        });
                    }

                    if (client.player.getAbilities().flying) client.options.sneakKey.setPressed(true);
                    else client.options.sneakKey.setPressed(false);

                    if (brokenCropTimestamps.size() < 1 && System.currentTimeMillis() - startTimestamp > 2000) {
                        maybeMacroCheck = true;
                        rotate = false;
                    }

//                    if (maybeMacroCheck && brokenCropTimestamps.size() > 10) {
//                        maybeMacroCheck = false;
//                    }

                    if (!Float.isNaN(lastYaw) && !Float.isNaN(lastPitch)) {
                        float yaw = client.player.getYaw();
                        float pitch = client.player.getPitch();

                        float yawDiff = MathHelper.wrapDegrees(yaw - lastYaw);
                        float pitchDiff = pitch - lastPitch;

                        double mouseX = client.mouse.getX();
                        double mouseY = client.mouse.getY();

                        double mouseDx = mouseX - lastMouseX;
                        double mouseDy = mouseY - lastMouseY;

                        if (System.currentTimeMillis() - startTimestamp > 2000 && (Math.abs(yawDiff) > 5.0f || Math.abs(pitchDiff) > 5.0f)) {
                            if (Math.abs(mouseDx) < 0.1 && Math.abs(mouseDy) < 0.1) {
                                maybeMacroCheck = true;
                                rotate = false;
                                LOGGER.info("Unexpected rotation detected! ΔYaw=" + yaw + " ΔPitch=" + pitch);
                                Utils.HKPrint(Text.literal("Unexpected rotation detected! ΔYaw=" + yaw + " ΔPitch=" + pitch));
                            }
                        }

                        lastYaw = yaw;
                        lastPitch = pitch;
                        lastMouseX = mouseX;
                        lastMouseY = mouseY;
                    }
                    else {
                        lastYaw = client.player.getYaw();
                        lastPitch = client.player.getPitch();
                        lastMouseX = client.mouse.getX();
                        lastMouseY = client.mouse.getY();
                    }

                    if (!maybeMacroCheck) rotate = true;

                    for (String s : AthenaConfig.actionPoints) {
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

                    if (Math.abs(MathHelper.wrapDegrees(client.player.getYaw() - AthenaConfig.yaw)) < 5) {
                        for (String key : heldKeys) {
                            if (Objects.equals(key, ".")) {
                                if (autoPestNextWarp) {
                                    autoPestNextWarp = false;
                                    initiateAutoPest(client);
                                } else {
                                    if (!waitForWarp) {
                                        waitForWarp = true;
                                        client.player.networkHandler.sendChatCommand("warp garden");
                                    }
                                }
                                break;
                            } else {
                                waitForWarp = false;
                                startTimestamp = System.currentTimeMillis();
                                keybindsTranslation.get(key).setPressed(true);
                                client.options.attackKey.setPressed(true);
                            }
                        }
                    }
                }

                wasActive = true;
            }
            else if (wasActive) {
                rotate = false;

                client.options.forwardKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.backKey.setPressed(false);
                client.options.attackKey.setPressed(false);

                PathFollower.stop();

                isPestRemoving = false;
                wasPestRemoving = false;
                waitForTP = false;
                wasActive = false;
                stoppedBlock = client.player.getBlockPos();
                client.options.pauseOnLostFocus = true;
                maybeMacroCheck = false;

                Utils.HKPrint(Text.literal("farmer stopped :(").formatted(Formatting.WHITE));
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;
            if (!DEBUG) {
                if (!Utils.isInGarden()) return;
            }

            if (rotate) {
                smoothRotateTo(AthenaConfig.yaw, AthenaConfig.pitch);
            }

            if (stoppedBlock != null) renderBlockMark(context.matrixStack(), stoppedBlock, 1f, 0f, 0.2f, 0.9f, false, "");

            if (AthenaConfig.showActionPoints && !actionPointsList.contains("empty")) {
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

        ClientPlayerBlockBreakEvents.AFTER.register((world, player, pos, state) -> {
            if (player == MinecraftClient.getInstance().player) {
                onBlockBroken(state.getBlock());
            }
        });
    }

    private static void initiateAutoPest (MinecraftClient mc) {
        isPestRemoving = true;
        originPlot = currentPlot;
        originSlot = mc.player.getInventory().getSelectedSlot();
        originBlock = mc.player.getBlockPos();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getName().getString().contains("Vacuum")) {
                mc.player.getInventory().setSelectedSlot(i);
                break;
            }
        }

        mc.options.forwardKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
    }

    private static void onBlockBroken(Block block) {
        long now = System.currentTimeMillis();
        if (Crops.getCropForBlock(block) == currentCrop) {
            brokenCropTimestamps.add(now);
        }
    }

    private static void smoothRotateTo(float targetYaw, float targetPitch) {
        MinecraftClient mc = MinecraftClient.getInstance();

        float currentYaw   = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDiff   = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        double dist = Math.hypot(yawDiff, pitchDiff);
        if (dist < 0.1) {
            mc.player.setYaw(targetYaw);
            mc.player.setPitch(targetPitch);
            return;
        }

        float g = 0.40f;

        float wMag = (float) Math.min(1.5, dist);
        float wYaw   = (float) ((Math.random() * 2 - 1) * wMag * 0.2);
        float wPitch = (float) ((Math.random() * 2 - 1) * wMag * 0.2);

        float windScale = (float)Math.min(1.0, dist / 5.0);
        yawVelocity   += (float) (wYaw   * windScale + g * (yawDiff / dist));
        pitchVelocity += (float) (wPitch * windScale + g * (pitchDiff / dist));

        float adaptiveSpeed = (float) Math.max(0.5, 2.0 * (dist / 40.0));

        float vMag = (float) Math.hypot(yawVelocity, pitchVelocity);
        if (vMag > adaptiveSpeed) {
            float vClip = adaptiveSpeed / 2 + (float) Math.random() * adaptiveSpeed / 2;
            yawVelocity   = yawVelocity / vMag * vClip;
            pitchVelocity = pitchVelocity / vMag * vClip;
        }

        float newYaw   = MathHelper.wrapDegrees(currentYaw + yawVelocity);
        float newPitch = MathHelper.clamp(currentPitch + pitchVelocity, -90f, 90f);

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);

        float damping = dist < 5 ? 0.6f : 0.9f;
        yawVelocity   *= damping;
        pitchVelocity *= damping;
    }
}
