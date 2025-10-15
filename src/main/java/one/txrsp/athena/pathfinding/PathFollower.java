package one.txrsp.athena.pathfinding;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import one.txrsp.athena.render.RenderUtils;
import one.txrsp.athena.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static one.txrsp.athena.commands.AthenaCommand.AthenaPrint;
import static one.txrsp.athena.Athena.LOGGER;

public class PathFollower {
    public static List<BlockPos> path = new ArrayList<>();
    private static int currentIndex = 0;
    public static boolean following = false;
    public static boolean found = false;
    private static int flightToggleTimer = 4;
    private static int lastJump = 0;
    private static float yawVelocity = 0;
    private static float pitchVelocity = 0;
    public static BlockPos goal;
    private static final Random random = new Random();
    private static Vec3d lastCheckPos = null;
    private static long lastCheckTime = 0;
    private static boolean isStuck = false;
    private static Vec3d targetLock = null;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            if (following) {
                Vec3d currentPos = mc.player.getPos();
                long now = System.currentTimeMillis();

                if (lastCheckPos == null) {
                    lastCheckPos = currentPos;
                    lastCheckTime = now;
                    return;
                }

                if (now - lastCheckTime > 5000) {
                    double distSq = currentPos.squaredDistanceTo(lastCheckPos);

                    if (distSq < 8) {
                        isStuck = true;
                        Utils.AthenaPrint(Text.literal("pathfollower possibly stuck, restarting"));
                        BlockPos newGoal = goal;
                        Vec3d newTargetLock = targetLock;
                        stop();

                        if (!AStarPathfinder.isPathfinding) {
                            pathfind(newGoal, newTargetLock, success -> {
                                if (success) {
                                    Utils.AthenaPrint(Text.literal("following path. nodes: " + path.size()));
                                } else {
                                    Utils.AthenaPrint(Text.literal("no path found :("));
                                }
                            });
                        }
                    } else {
                        isStuck = false;
                    }

                    lastCheckPos = currentPos;
                    lastCheckTime = now;
                }

                lastJump++;
                if (!mc.player.getAbilities().flying) {
                    switch (flightToggleTimer) {
                        case 4 -> mc.options.jumpKey.setPressed(true);
                        case 3 -> mc.options.jumpKey.setPressed(false);
                        case 2 -> {
                        }
                        case 1 -> mc.options.jumpKey.setPressed(true);
                        case 0 -> {
                            mc.options.jumpKey.setPressed(false);
                            flightToggleTimer = 5;
                        }
                    }
                    flightToggleTimer--;
                } else {
                    flightToggleTimer = 5;
                }
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (path.isEmpty()) return;
            if (!following) return;

            BlockPos prev = null;
            for (BlockPos pos : path) {
                if (prev != null) {
                    RenderUtils.drawLine(context.matrixStack(), prev, pos, 1f, 0f, 0.2f, 1f);
                }
                prev = pos;
            }

            followPath();
        });
    }

    public static void pathfind(BlockPos goal, Vec3d targetLock, Consumer<Boolean> callback) {
        ClientWorld world = MinecraftClient.getInstance().world;
        BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
        AStarPathfinder.findPathAsync(playerPos, goal, world).thenAccept(path -> {
            boolean success = !path.isEmpty();
            if (success) {
                PathFollower.path = path;
                following = true;
                PathFollower.goal = goal;
                found = false;
                currentIndex = 0;
                flightToggleTimer = 4;
                lastJump = 0;
                isStuck = false;
                PathFollower.targetLock = targetLock;
            }
            callback.accept(success);
        });
    }

    private static void followPath() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;

        Vec3d playerPos = mc.player.getPos();
        Vec3d eyePos = mc.player.getEyePos();
        BlockPos targetNode = path.get(currentIndex);
        Vec3d targetCenter = Vec3d.ofCenter(targetNode);
        if (currentIndex == path.size() - 1 && AStarPathfinder.isPassable(world, targetNode)) {
            targetCenter.subtract(0, 0.3, 0);
        }

        Vec3d dir = targetCenter.subtract(playerPos);
        double distance = dir.length();

        if (distance < 1) {
            if (currentIndex < path.size() - 1) {
                currentIndex++;
            } else if (currentIndex == path.size() - 1 && mc.player.getVelocity().length() < 0.2) {
                currentIndex++;
            }

            if (currentIndex >= path.size()) {
                following = false;
                found = true;
                stopAllMovement();
                return;
            }
            targetNode = path.get(currentIndex);
            targetCenter = Vec3d.ofCenter(targetNode);
        }

        double dx = targetCenter.x - playerPos.x;
        double dy = targetCenter.y - playerPos.y;
        double dz = targetCenter.z - playerPos.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        double ex = targetCenter.x - eyePos.x;
        double ey = targetCenter.y + 1 - eyePos.y;
        double ez = targetCenter.z - eyePos.z;

        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(ez, ex)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(ey, Math.sqrt(ex * ex + ez * ez)));
        if (currentIndex == path.size() - 1 && playerPos.squaredDistanceTo(targetCenter) < 160 && targetLock != null) {
            double tex = targetLock.x - eyePos.x;
            double tey = targetLock.y + 1 - eyePos.y;
            double tez = targetLock.z - eyePos.z;
            yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(tez, tex)) - 90.0);
            pitch = (float) -Math.toDegrees(Math.atan2(tey, Math.sqrt(tex * tex + tez * tez)));
        }
        else pitch = MathHelper.clamp(pitch, -20, 20);

        Vec3d vel = mc.player.getVelocity();
        double dot = vel.normalize().dotProduct(dir.normalize()); // 1 = moving toward node, -1 moving away

        smoothRotateTo(yaw, pitch);

        KeyBinding forward = mc.options.forwardKey;
        KeyBinding back = mc.options.backKey;
        KeyBinding jump = mc.options.jumpKey;
        KeyBinding right = mc.options.rightKey;
        KeyBinding left = mc.options.leftKey;
        KeyBinding sneak = mc.options.sneakKey;
        KeyBinding sprint = mc.options.sprintKey;

        if (mc.player.getAbilities().flying) {
            Vec3d lookVec = mc.player.getRotationVec(1.0f).normalize();
            Vec3d flatLook = new Vec3d(lookVec.x, 0, lookVec.z).normalize();
            Vec3d frontPos = mc.player.getPos().subtract(0, 0.250, 0).add(flatLook);
            BlockPos blockInFrontOfHead = BlockPos.ofFloored(frontPos).up(2);
            BlockPos blockInFrontOfFeet = BlockPos.ofFloored(frontPos);
            BlockState frontHeadState = world.getBlockState(blockInFrontOfHead);
            BlockState frontFeetState = mc.world.getBlockState(blockInFrontOfFeet);
            BlockState blockBelow = world.getBlockState(mc.player.getBlockPos().down());
            BlockState blockAbove = world.getBlockState(mc.player.getBlockPos().up(2));

            BlockPos prevNode = currentIndex > 0 ? path.get(currentIndex - 1) : targetNode;
            Vec3d segmentStart = Vec3d.ofCenter(prevNode);
            Vec3d segmentEnd   = Vec3d.ofCenter(targetNode);
            Vec3d segmentDir   = segmentEnd.subtract(segmentStart);

            Vec3d playerPosVec = mc.player.getPos();
            Vec3d projectedPoint;

            Vec3d segmentHoriz = new Vec3d(segmentDir.x, 0.0, segmentDir.z);
            //LOGGER.info("horiz: " + segmentHoriz.length());
            boolean isVerticalSegment = segmentHoriz.length() < 15.0;

            if (isVerticalSegment) {
                projectedPoint = new Vec3d(segmentStart.x, playerPosVec.y, segmentStart.z);
            } else {
                double segLenSq = segmentDir.lengthSquared();
                if (segLenSq < 1e-6) {
                    projectedPoint = segmentStart;
                } else {
                    double t = playerPosVec.subtract(segmentStart).dotProduct(segmentDir) / segLenSq;
                    t = MathHelper.clamp((float) t, 0f, 1f);
                    projectedPoint = segmentStart.add(segmentDir.multiply(t));
                }
            }

            Vec3d offsetVec = playerPosVec.subtract(projectedPoint);

            Vec3d rightVec;
            if (segmentHoriz.lengthSquared() < 1e-6) {
                // segment basically vertical — derive right from player yaw so we still have a stable horizontal axis
                double yawRad = Math.toRadians(mc.player.getYaw());
                // right vector based on yaw (90° clockwise)
                rightVec = new Vec3d(Math.cos(yawRad + Math.PI/2.0), 0, Math.sin(yawRad + Math.PI/2.0));
            } else {
                // up.cross(segmentHoriz) gives a right-hand vector pointing to player's right
                rightVec = new Vec3d(0, 1, 0).crossProduct(segmentHoriz).normalize();
            }

            if (Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - yaw)) < 15) {
                //LOGGER.info("horizontal dist: " + horizontalDist);
                if (segmentHoriz.length() > 30 && horizontalDist > 10) {
                    sprint.setPressed(true);
                    forward.setPressed(true);
                    back.setPressed(false);
                } else if (horizontalDist < 8 && vel.length() > 0.45) {
                    sprint.setPressed(false);
                    forward.setPressed(false);
                    back.setPressed(true);
                } else if (horizontalDist < 1) {
                    //LOGGER.info("1 vel: " + vel.length());
                    if (vel.length() > 0.25) {
                        forward.setPressed(false);
                        back.setPressed(true);
                    } else {
                        forward.setPressed(true);
                        back.setPressed(false);
                    }
                } else if (horizontalDist < 2.5) {
                    //LOGGER.info("2.5 vel: " + vel.length());
                    if (vel.length() > 0.35) {
                        forward.setPressed(false);
                        back.setPressed(true);
                    } else if (vel.length() > 0.23) {
                        forward.setPressed(false);
                        back.setPressed(false);
                    } else {
                        forward.setPressed(true);
                        back.setPressed(false);
                    }
                } else {
                    forward.setPressed(true);
                    back.setPressed(false);
                    sprint.setPressed(false);
                }
            } else {
                forward.setPressed(false);
                back.setPressed(false);
                sprint.setPressed(false);
            }

            double sideOffset = offsetVec.dotProduct(rightVec);
            double sideThreshold = 0.5;

            if (blockBelow.getBlock() instanceof TrapdoorBlock && blockBelow.get(TrapdoorBlock.OPEN) ||
                    blockAbove.getBlock() instanceof TrapdoorBlock && blockAbove.get(TrapdoorBlock.OPEN)) {
                if (random.nextBoolean()) {
                    right.setPressed(true);
                    left.setPressed(false);
                } else {
                    right.setPressed(false);
                    left.setPressed(true);
                }
            } else if (sideOffset < sideThreshold && sideOffset > -sideThreshold) {
                right.setPressed(false);
                left.setPressed(false);
            }

            if (currentIndex != 0) {
                if (Math.abs(sideOffset) < 1) {
                    if (sideOffset > sideThreshold) {
                        left.setPressed(false);
                        right.setPressed(true);
                    } else if (sideOffset < -sideThreshold) {
                        left.setPressed(true);
                        right.setPressed(false);
                    } else {
                        left.setPressed(false);
                        right.setPressed(false);
                    }
                } else {
                    left.setPressed(false);
                    right.setPressed(false);
                }
            }

            double verticalOffset = offsetVec.y;
            if (isVerticalSegment) {
                verticalOffset = -dy;
            }
            double verticalTolerance = 0.3;

            if (verticalOffset > verticalTolerance) {
                jump.setPressed(false);
                if (AStarPathfinder.isPassable(world, mc.player.getBlockPos().down())) {
                    sneak.setPressed(true);
                } else {
                    sneak.setPressed(false);
                }
            } else if (verticalOffset < -verticalTolerance) {
                if (lastJump > 6) {
                    jump.setPressed(true);
                    lastJump = 0;
                }
                sneak.setPressed(false);
            } else {
                jump.setPressed(false);
                sneak.setPressed(false);
            }

            //LOGGER.info(String.valueOf(playerPos.distanceTo(blockInFrontOfHead.toCenterPos())));
            //playerPos.distanceTo(blockInFrontOfHead.toCenterPos()) < 1 &&
            if (!AStarPathfinder.isPassable(world, blockInFrontOfHead) &&
                    AStarPathfinder.isPassable(world, blockInFrontOfHead.down()) &&
                    AStarPathfinder.isPassable(world, blockInFrontOfHead.down(2)) &&
                    mc.player.getEyePos().distanceTo(blockInFrontOfHead.toCenterPos()) < 0.5) {
                sneak.setPressed(true);
                jump.setPressed(false);
                LOGGER.info(world.getBlockState(blockInFrontOfHead).getBlock().getTranslationKey());
            }

            if (blockBelow.exceedsCube() || !AStarPathfinder.isPassable(world, blockInFrontOfFeet)) {
                jump.setPressed(true);
                sneak.setPressed(false);
            }

            if (world.getBlockState(blockInFrontOfHead.down(2)).getBlock() instanceof TrapdoorBlock &&
                    world.getBlockState(blockInFrontOfHead.down(2)).get(TrapdoorBlock.OPEN)) {
                jump.setPressed(true);
                sneak.setPressed(false);
            }
        }
    }

    public static void stop() {
        following = false;
        found = false;
        stopAllMovement();
    }

    private static void stopAllMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();

        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
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
