package one.txrsp.hktools.pathfinding;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import one.txrsp.hktools.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static one.txrsp.hktools.commands.HKToolsCommand.HKPrint;

public class PathFollower {
    private static List<BlockPos> path = new ArrayList<>();
    private static int currentIndex = 0;
    public static boolean following = false;
    public static boolean found = false;
    private static int flightToggleTimer = 4;
    private static float yawVelocity = 0;
    private static float pitchVelocity = 0;

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            literal("pathfind")
                .then(argument("x", IntegerArgumentType.integer())
                    .then(argument("y", IntegerArgumentType.integer())
                        .then(argument("z", IntegerArgumentType.integer())
                            .executes(context -> {
                                int x = IntegerArgumentType.getInteger(context, "x");
                                int y = IntegerArgumentType.getInteger(context, "y");
                                int z = IntegerArgumentType.getInteger(context, "z");
                                BlockPos target = new BlockPos(x, y, z);

                                if (pathfind(target)) {
                                    HKPrint(context, Text.literal("following path. nodes: " + path.size()).formatted(Formatting.WHITE));
                                }
                                else {
                                    HKPrint(context, Text.literal("no path found :(").formatted(Formatting.WHITE));
                                }

                                return 1;
                            })
                        )
                    )
                )
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            if (following && !mc.player.getAbilities().flying) {
                switch (flightToggleTimer) {
                    case 4 -> mc.options.jumpKey.setPressed(true);
                    case 3 -> mc.options.jumpKey.setPressed(false);
                    case 2 -> {}
                    case 1 -> mc.options.jumpKey.setPressed(true);
                    case 0 -> {
                        mc.options.jumpKey.setPressed(false);
                        flightToggleTimer = 5;
                    }
                }
                flightToggleTimer--;
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

    public static boolean pathfind(BlockPos to) {
        ClientWorld world = MinecraftClient.getInstance().world;
        BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
        path = AStarPathfinder.findPath(playerPos, to, world);
        if (!path.isEmpty()) {
            following = true;
            found = false;
            currentIndex = 0;
            flightToggleTimer = 4;
            return true;
        }
        else {
            return false;
        }
    }

    private static void followPath() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;

        Vec3d playerPos = mc.player.getPos();
        Vec3d eyePos = mc.player.getEyePos();
        BlockPos targetNode = path.get(currentIndex);
        Vec3d targetCenter = Vec3d.ofCenter(targetNode);

        Vec3d dir = targetCenter.subtract(playerPos);
        double distance = dir.length();

        if (distance < 1) {
            currentIndex++;
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
        if (currentIndex == path.size() - 1) pitch = 75;
        else pitch = MathHelper.clamp(pitch, -20, 20);

        Vec3d vel = mc.player.getVelocity();
        double dot = vel.normalize().dotProduct(dir.normalize()); // 1 = moving toward node

        smoothRotateTo(yaw, pitch, 0.5f, 5f);

        KeyBinding forward = mc.options.forwardKey;
        KeyBinding back = mc.options.backKey;
        KeyBinding jump = mc.options.jumpKey;
        KeyBinding sneak = mc.options.sneakKey;

        if (Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - yaw)) < 15) {
            if (horizontalDist < 0.4 || vel.length() > 0.4) {
                forward.setPressed(false);
                back.setPressed(true);
            } else {
                forward.setPressed(true);
                back.setPressed(false);
            }
        } else {
            forward.setPressed(false);
            back.setPressed(false);
        }

        if (mc.player.getAbilities().flying) {
            double zone = 0.5;

            Vec3d lookVec = mc.player.getRotationVec(1.0f).normalize();
            Vec3d frontPos = mc.player.getPos().add(lookVec.multiply(1.0));
            BlockPos blockInFront = BlockPos.ofFloored(frontPos);
            BlockState frontState = world.getBlockState(blockInFront);

            if (frontState.getBlock() instanceof TrapdoorBlock && frontState.get(TrapdoorBlock.OPEN)) {
                jump.setPressed(true);
                sneak.setPressed(false);
            } else {
                if (dy > zone) {
                    jump.setPressed(true);
                    sneak.setPressed(false);
                } else if (dy < -zone) {
                    sneak.setPressed(true);
                    jump.setPressed(false);
                } else {
                    jump.setPressed(false);
                    sneak.setPressed(false);
                }
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
    }

    private static void smoothRotateTo(float targetYaw, float targetPitch, float maxAccel, float maxSpeed) {
        MinecraftClient mc = MinecraftClient.getInstance();

        float currentYaw   = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDiff   = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        // accelerate toward target
        yawVelocity   += Math.signum(yawDiff)   * maxAccel;
        pitchVelocity += Math.signum(pitchDiff) * maxAccel;

        // clamp speeds
        yawVelocity   = MathHelper.clamp(yawVelocity,   -maxSpeed, maxSpeed);
        pitchVelocity = MathHelper.clamp(pitchVelocity, -maxSpeed, maxSpeed);

        // decelerate near target
        if (Math.abs(yawDiff) < Math.abs(yawVelocity)) yawVelocity *= 0.5f;
        if (Math.abs(pitchDiff) < Math.abs(pitchVelocity)) pitchVelocity *= 0.5f;

        // prevent overshoot: if we'd pass the target this frame, just land on it
        if (Math.abs(yawDiff) < Math.abs(yawVelocity))   yawVelocity   = yawDiff;
        if (Math.abs(pitchDiff) < Math.abs(pitchVelocity)) pitchVelocity = pitchDiff;

        // light friction so we settle nicely
        yawVelocity   *= 0.9f;
        pitchVelocity *= 0.9f;

        float newYaw   = MathHelper.wrapDegrees(currentYaw + yawVelocity);
        float newPitch = MathHelper.clamp(currentPitch + pitchVelocity, -90f, 90f);

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
    }
}
