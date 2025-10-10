package one.txrsp.athena.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import one.txrsp.athena.config.AthenaConfig;
import one.txrsp.athena.utils.KeyPressHelper;
import one.txrsp.athena.utils.Utils;

import java.util.List;
import java.util.Objects;

import static one.txrsp.athena.Athena.LOGGER;

public class SimpleAutoFisher {
    private static boolean autoFishing = false;
    private static int ticksUntilUse = -1;

    public static void init() {

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!AthenaConfig.autoFisher) return;
            if (mc.player == null) return;
            if (mc.currentScreen != null) return;

            boolean holdingRod = mc.player.getMainHandStack().isOf(Items.FISHING_ROD);

            if (holdingRod && mc.options.useKey.isPressed()) {
                if (!autoFishing && mc.player.fishHook == null) {
                    autoFishing = true;
                    Utils.AthenaPrint(Text.literal("autoFishing now"));
                } else if (autoFishing) {
                    autoFishing = false;
                    Utils.AthenaPrint(Text.literal("autoFishing stopped"));
                }
                mc.options.useKey.setPressed(false);
            }

            if (!holdingRod && autoFishing) {
                autoFishing = false;
                Utils.AthenaPrint(Text.literal("autoFishing stopped"));
            }

            if (autoFishing) {
                if (mc.player.fishHook != null && mc.player.fishHook.isInFluid()) {
                    List<ArmorStandEntity> as = mc.world.getEntitiesByClass(ArmorStandEntity.class, mc.player.getBoundingBox().expand(50), e -> e.hasCustomName() && Objects.equals(e.getCustomName().getString(), "!!!"));

                    if (as.size() > 0) {
                        if (ticksUntilUse == -1) ticksUntilUse = Utils.randIntBetween(3, 7);
                    }
                }

                if (ticksUntilUse >= 0) {
                    ticksUntilUse--;

                    if (ticksUntilUse == 0) {
                        if (mc.player.fishHook != null && mc.player.fishHook.isInFluid()) {
                            ticksUntilUse = Utils.randIntBetween(10, 20);
                        }

                        mc.player.swingHand(mc.player.getActiveHand());
                        mc.interactionManager.interactItem(mc.player, mc.player.getActiveHand());
                    }
                }
            } else {
                ticksUntilUse = -1;
            }
        });
    }
}
