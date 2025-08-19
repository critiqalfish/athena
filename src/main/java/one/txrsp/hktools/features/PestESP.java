package one.txrsp.hktools.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import one.txrsp.hktools.config.HKConfig;
import one.txrsp.hktools.render.RenderUtils;
import one.txrsp.hktools.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static one.txrsp.hktools.HKTools.DEBUG;

public class PestESP {
    private static final Box gardenBarnAABB = new Box(new Vec3d(-46, 65, -46), new Vec3d(46, 90, 46));
    public static List<Integer> pestPlots = new ArrayList<>();
    public static List<Box> pests = new ArrayList<>();
    public static int totalPests = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) return;
            if (!DEBUG) {
                if (!Utils.isInGarden()) return;
            }

            pests.clear();
            pestPlots.clear();

            List<String> tabLines = Utils.getTablistLines();
            for (String line : tabLines) {
                if (line.contains("Plots:")) {
                    for (String s : line.replace("Plots: ", "").split(", ")) {
                        pestPlots.add(Integer.parseInt(s));
                    }
                }
            }

            List<String> sbLines = Utils.getScoreboardLines();
            for (String line : sbLines) {
                if (line.contains("ൠ")) {
                    totalPests = Integer.parseInt(line.substring(line.lastIndexOf("ൠ") + 3));
                }
            }

            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ArmorStandEntity as) {
                    if (as.isInvisible() && !as.getEquippedStack(EquipmentSlot.HEAD).isEmpty() && as.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.PLAYER_HEAD)) {
                        Box shrinkBox = as.getBoundingBox().withMinY(as.getBoundingBox().maxY - as.getBoundingBox().getLengthX());
                        pests.add(shrinkBox.expand(0.1));
                    }
                }
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!HKConfig.pestESP) return;
            if (!DEBUG) {
                if (!Utils.isInGarden()) return;
            }
            ItemStack held = MinecraftClient.getInstance().player.getMainHandStack();
            if (held.isEmpty() || !held.getName().getString().contains("Vacuum")) return;

            for (Box box : pests) {
                RenderUtils.drawBox(context.matrixStack(), box, 0.2f, 1.0f, 0.1f, 0.6f);
            }
        });
    }
}
