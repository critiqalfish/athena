package one.txrsp.athena.features;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.mixin.client.gametest.input.MouseAccessor;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import one.txrsp.athena.Athena;
import one.txrsp.athena.commands.AthenaCommand;
import one.txrsp.athena.config.AthenaConfig;
import one.txrsp.athena.mixin.InGameHudAccessor;
import one.txrsp.athena.pathfinding.AStarPathfinder;
import one.txrsp.athena.pathfinding.PathFollower;
import one.txrsp.athena.render.RenderUtils;
import one.txrsp.athena.utils.Crops;
import one.txrsp.athena.utils.KeyPressHelper;
import one.txrsp.athena.utils.OnceAgain;
import one.txrsp.athena.utils.Utils;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;

import static one.txrsp.athena.Athena.DEBUG;
import static one.txrsp.athena.Athena.LOGGER;
import static one.txrsp.athena.render.RenderUtils.renderBlockMark;
import static one.txrsp.athena.utils.Utils.AthenaPrint;

public class FramignAuto {
    private static KeyBinding keybind;
    private static boolean active = false;
    private static boolean wasActive = false;
    private static BlockPos stoppedBlock = null;
    private static boolean renderStoppedBlock = false;
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
    private static double lastMouseDx = Float.NaN;
    private static double lastMouseDy = Float.NaN;
    private static boolean maybeMacroCheck = false;
    public static boolean holdLeftClick = false;

    public static void init() {
        actionPointsList.add("empty");

        keybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "framignAuto",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_CONTROL,
                "category.athena"
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
                if (isPestRemoving && !client.currentScreen.getTitle().getString().contains("Pets") || isPestRemoving && Objects.equals(client.currentScreen.getTitle().getString(), "Stereo Harmony")) {
                    OnceAgain.executeWithCooldown("screenclose", 20, () -> {
                        MinecraftClient.getInstance().currentScreen.close();
                    });
                } else {
                    startTimestamp = System.currentTimeMillis();
                    return;
                }
            }
            if (actionPointsList.contains("empty")) {
                actionPointsList.clear();
                Collections.addAll(actionPointsList, AthenaConfig.Farming.actionPoints);
            }
            if (!DEBUG) {
                if (active && !Utils.isInGarden()) {
                    active = false;
                    wasActive = true;
                }
                renderStoppedBlock = Utils.isInGarden();
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

            if (Utils.isInGarden() && keybind.wasPressed()) active = !active;

            if (active) {
                if (!wasActive) {
                    AthenaPrint(Text.literal("farmer working :)").formatted(Formatting.WHITE));
                    client.options.pauseOnLostFocus = false;
                    stoppedBlock = null;
                    waitForTP = false;
                    startTimestamp = System.currentTimeMillis();
                    maybeMacroCheck = false;
                    if (AthenaConfig.Farming.autoPetSwitch) PetSwitcher.switchPet("Mooshroom Cow");
                }

                if (lastPos != null && currentPos.distanceTo(lastPos) < 0.001) {
                    stillTicks++;
                } else {
                    stillTicks = 0;
                }

                lastPos = currentPos;

                if (PestESP.totalPests >= AthenaConfig.Farming.autoPestThreshold && !isPestRemoving && AthenaConfig.Farming.autoPest) {
                    if (AthenaConfig.Farming.autoPestWarpWait) {
                        autoPestNextWarp = true;
                    } else {
                        initiateAutoPest(client);
                        AthenaPrint(Text.literal("killing pests").formatted(Formatting.WHITE));
                    }
                }

                if (isPestRemoving && AthenaConfig.Farming.autoPest) {
                    //holdLeftClick = false;
                    rotate = false;

                    if (!PestESP.pestPlots.isEmpty()) {
                        if (AthenaConfig.Farming.autoPetSwitch && !Objects.equals(PetSwitcher.targetPet, "Hedgehog")) {
                            PetSwitcher.switchPet("Hedgehog");
                        } else if (PetSwitcher.isSwitching) {
                            // chill out
                        } else if (!waitForTP && currentPlot != PestESP.pestPlots.getFirst()) {
                            waitForTP = true;
                            PathFollower.stop();
                            client.player.networkHandler.sendChatCommand("plottp " + PestESP.pestPlots.getFirst());
                        } else if (currentPlot == PestESP.pestPlots.getFirst()) {
                            waitForTP = false;

                            OnceAgain.swingHandWithCooldown(30, true);

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
                                    if (!AStarPathfinder.isPathfinding) {
                                        PathFollower.pathfind(pestPos, box.getCenter(), success -> {
                                            if (success) {

                                            }
                                        });
                                    }
                                } else {
                                    PathFollower.stop();
                                }
                            } else if (!PestESP.pests.isEmpty() && PathFollower.goal.getSquaredDistance(PestESP.pests.getFirst().getCenter()) > 100) {
                                Box box = PestESP.pests.getFirst();
                                BlockPos pestPos = BlockPos.ofFloored(
                                        (box.minX + box.maxX) / 2.0,
                                        box.maxY + 8,
                                        (box.minZ + box.maxZ) / 2.0
                                );
                                if (!AStarPathfinder.isPathfinding) {
                                    PathFollower.pathfind(pestPos, box.getCenter(), success -> {
                                        if (success) {

                                        }
                                    });
                                }
                            }
                        } else if (currentPlot != PestESP.pestPlots.getFirst()) {
                            client.options.forwardKey.setPressed(true);
                        }
                    } else {
                        AthenaPrint(Text.literal("killed all pests").formatted(Formatting.WHITE));
                        PestESP.totalPests = 0;
                        isPestRemoving = false;
                        waitForTP = false;
                        PathFollower.stop();
                        //holdLeftClick = false;
                        client.options.attackKey.setPressed(false);
                        client.options.useKey.setPressed(false);
                        client.options.forwardKey.setPressed(false);
                        client.player.getInventory().setSelectedSlot(originSlot);
                        if (AthenaConfig.Farming.autoPetSwitch) PetSwitcher.switchPet("Mooshroom Cow");

                        if (AthenaConfig.Farming.autoPestWarpWait) {
                            wasPestRemoving = false;
                            startTimestamp = System.currentTimeMillis();
                            client.player.networkHandler.sendChatCommand("warp garden");
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
                                if (!AStarPathfinder.isPathfinding) {
                                    PathFollower.pathfind(originBlock, null, success -> {
                                        if (success) {

                                        }
                                    });
                                }
                            }
                        }
                    } else {
                        client.options.forwardKey.setPressed(true);
                    }
                } else if (!isPestRemoving) {
                    // normal farming
                    if (PetSwitcher.wasSwitching) {
                        //client.options.attackKey.setPressed(false);
                        //KeyBinding.setKeyPressed(InputUtil.Type.MOUSE.createFromCode(GLFW.GLFW_MOUSE_BUTTON_LEFT), false);
                        PetSwitcher.wasSwitching = false;
                        client.mouse.lockCursor();
                        //holdLeftClick = false;
                        return;
                    }

                    if (!OnceAgain.swingHandInProgress && !PetSwitcher.isSwitching) {
                        //client.options.attackKey.setPressed(true);
                        //KeyBinding.setKeyPressed(InputUtil.Type.MOUSE.createFromCode(GLFW.GLFW_MOUSE_BUTTON_LEFT), true);
                        //KeyBinding.onKeyPressed(InputUtil.Type.MOUSE.createFromCode(GLFW.GLFW_MOUSE_BUTTON_LEFT));
                        //holdLeftClick = true;

                        HitResult hit = client.crosshairTarget;

                        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                            BlockHitResult bhr = (BlockHitResult) hit;
                            client.player.swingHand(client.player.getActiveHand());
                            client.interactionManager.attackBlock(bhr.getBlockPos(), bhr.getSide());
                        } /*else if (hit == null || hit.getType() == HitResult.Type.MISS) {
                            Vec3d eyes = client.player.getCameraPosVec(1f);
                            Vec3d look = client.player.getRotationVec(1f);
                            Vec3d end = eyes.add(look.multiply(4.5));
                            BlockHitResult forwardHit = client.world.raycast(
                                    new RaycastContext(eyes, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player)
                            );

                            if (forwardHit.getType() == HitResult.Type.BLOCK) {
                                client.interactionManager.attackBlock(forwardHit.getBlockPos(), forwardHit.getSide());
                            }
                        }*/
                    } else if (PetSwitcher.isSwitching) {
                        //client.options.attackKey.setPressed(false);
                        //holdLeftClick = false;
                        //KeyBinding.setKeyPressed(InputUtil.Type.MOUSE.createFromCode(GLFW.GLFW_MOUSE_BUTTON_LEFT), false);
                    }

                    if (client.player.getAbilities().flying) client.options.sneakKey.setPressed(true);
                    else client.options.sneakKey.setPressed(false);

                    if (!Float.isNaN(lastYaw) && !Float.isNaN(lastPitch)) {
                        float yaw = client.player.getYaw();
                        float pitch = client.player.getPitch();

                        float yawDiff = MathHelper.wrapDegrees(yaw - lastYaw);
                        float pitchDiff = pitch - lastPitch;

                        double mouseX = client.mouse.getX();
                        double mouseY = client.mouse.getY();

                        double mouseDx = Math.abs(mouseX - lastMouseX);
                        double mouseDy = Math.abs(mouseY - lastMouseY);

                        if (System.currentTimeMillis() - startTimestamp > 2000 && (Math.abs(yawDiff) > 5.0f || Math.abs(pitchDiff) > 5.0f)) {
                            //LOGGER.info("mDx: " + mouseDx + " mDy: " + mouseDy + " lmDx: " + lastMouseDx + " lmDy: " + lastMouseDy);
                            if (mouseDx == lastMouseDx && mouseDy == lastMouseDy) {
                                if (Math.abs(mouseDx) < 0.1 && Math.abs(mouseDy) < 0.1) {
                                    maybeMacroCheck = true;
                                    rotate = false;
                                    AthenaPrint(Text.literal("Unexpected rotation detected! ΔYaw=" + yaw + " ΔPitch=" + pitch));
                                }
                            }
                        }

                        lastYaw = yaw;
                        lastPitch = pitch;
                        lastMouseX = mouseX;
                        lastMouseY = mouseY;
                        lastMouseDx = mouseDx;
                        lastMouseDy = mouseDy;
                    }
                    else {
                        lastYaw = client.player.getYaw();
                        lastPitch = client.player.getPitch();
                        lastMouseX = client.mouse.getX();
                        lastMouseY = client.mouse.getY();
                        lastMouseDx = 0.0;
                        lastMouseDy = 0.0;
                    }

                    if (!maybeMacroCheck) rotate = true;

                    for (String s : AthenaConfig.Farming.actionPoints) {
                        if (s.startsWith(currentCrop.name() + "|" + client.player.getBlockPos().toString())) {
                            if (!wasActive || stillTicks >= 1) {
                                String keys = s.substring(s.lastIndexOf("}") + 1);
                                heldKeys.clear();
                                startTimestamp = System.currentTimeMillis();
                                for (char k : keys.toCharArray()) {
                                    heldKeys.add(String.valueOf(k));
                                }
                                client.options.forwardKey.setPressed(false);
                                client.options.leftKey.setPressed(false);
                                client.options.rightKey.setPressed(false);
                                client.options.backKey.setPressed(false);
                            }
                        }
                        else if (s.startsWith(client.player.getBlockPos().toString())) {
                            // this is migration
                            String keys = s.substring(s.lastIndexOf("}") + 1);

                            boolean removed = FramignAuto.actionPointsList.removeIf(p -> p.startsWith(MinecraftClient.getInstance().player.getBlockPos().toString()));
                            if (removed) AthenaPrint(Text.literal("removed this point"));
                            AthenaConfig.Farming.actionPoints = FramignAuto.actionPointsList.toArray(new String[0]);
                            Athena.CONFIG.saveConfig(AthenaConfig.class);

                            FramignAuto.actionPointsList.add(currentCrop.name() + "|" + MinecraftClient.getInstance().player.getBlockPos().toString() + keys);
                            AthenaConfig.Farming.actionPoints = FramignAuto.actionPointsList.toArray(new String[0]);
                            Athena.CONFIG.saveConfig(AthenaConfig.class);
                            AthenaPrint(Text.literal("added this point"));
                            Athena.CONFIG.saveConfig(AthenaConfig.class);
                        }
                    }

                    if (brokenCropTimestamps.size() < 1 && System.currentTimeMillis() - startTimestamp > 2000) {
                        maybeMacroCheck = true;
                        rotate = false;
                    }

                    if (Math.abs(MathHelper.wrapDegrees(client.player.getYaw() - AthenaConfig.Farming.yaw)) < 5) {
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
                                keybindsTranslation.get(key).setPressed(true);
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
                //holdLeftClick = false;
                client.options.useKey.setPressed(false);

                PathFollower.stop();

                isPestRemoving = false;
                wasPestRemoving = false;
                waitForTP = false;
                wasActive = false;
                if (Utils.isInGarden()) stoppedBlock = client.player.getBlockPos();
                client.options.pauseOnLostFocus = true;
                maybeMacroCheck = false;

                AthenaPrint(Text.literal("farmer stopped :(").formatted(Formatting.WHITE));
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;
            if (!DEBUG) {
                if (!Utils.isInGarden()) return;
            }

            if (rotate) {
                smoothRotateTo(AthenaConfig.Farming.yaw, AthenaConfig.Farming.pitch);
            }

            if (stoppedBlock != null && renderStoppedBlock) renderBlockMark(context.matrixStack(), stoppedBlock, 1f, 0f, 0.2f, 0.9f, false, "");

            if (AthenaConfig.Farming.showActionPoints && !actionPointsList.contains("empty")) {
                for (String p : actionPointsList) {

                    if (p.contains("|")) {
                        String cropName = p.substring(0, p.lastIndexOf("|"));
                        if (currentCrop == Crops.getCropForString(cropName)) {
                            String[] parts = p.substring(p.indexOf("{") + 1, p.lastIndexOf("}")).split(", ");
                            Vec3i coords = new Vec3i(Integer.parseInt(parts[0].split("=")[1]), Integer.parseInt(parts[1].split("=")[1]), Integer.parseInt(parts[2].split("=")[1]));
                            BlockPos pos = new BlockPos(coords);
                            String text = p.substring(p.lastIndexOf("}") + 1);
                            text = Objects.equals(text, ".") ? "warp" : text;
                            renderBlockMark(context.matrixStack(), pos, 0.2f, 0.3f, 1f, 0.5f, false, text);
                        }
                    }
                }
            }
        });

        ClientPlayerBlockBreakEvents.AFTER.register((world, player, pos, state) -> {
            if (player == MinecraftClient.getInstance().player) {
                onBlockBroken(state.getBlock());
            }
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient) {
                Block block = world.getBlockState(pos).getBlock();

                if (Objects.equals(block.getTranslationKey(), "block.minecraft.cactus")) {
                    onBlockBroken(block);
                }
            }
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register(((drawContext, renderTickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            if (!AthenaConfig.statusIndicator) return;
            if (!Utils.isInGarden()) return;

            if (active) {
                if (maybeMacroCheck) {
                    RenderUtils.drawStatusLabel(drawContext, "Athena", Color.ORANGE);
                } else {
                    RenderUtils.drawStatusLabel(drawContext, "Athena", Color.GREEN);
                }
            } else if (!active) {
                RenderUtils.drawStatusLabel(drawContext, "Athena", Color.RED);
            }
        }));
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
        //holdLeftClick = false;
        mc.options.useKey.setPressed(false);
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
        if (dist < 0.3) {
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
