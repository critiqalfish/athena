package one.txrsp.athena.features;

import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import one.txrsp.athena.config.AthenaConfig;
import one.txrsp.athena.render.RenderUtils;
import one.txrsp.athena.utils.KeyPressHelper;
import one.txrsp.athena.utils.Utils;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import static one.txrsp.athena.Athena.LOGGER;

public class SimpleAutoFisher {
    private static boolean autoFishing = false;
    private static int ticksUntilUse = -1;
    private static long lastRod = 0;
    private static boolean holdingRod = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!AthenaConfig.Fishing.autoFisher) return;
            if (mc.player == null) return;
            if (mc.currentScreen != null) return;

            holdingRod = mc.player.getMainHandStack().isOf(Items.FISHING_ROD);

            if (holdingRod && mc.options.useKey.isPressed()) {
                if (!autoFishing && mc.player.fishHook == null) {
                    autoFishing = true;
                    mc.options.pauseOnLostFocus = false;
                    lastRod = System.currentTimeMillis();
                    Utils.AthenaPrint(Text.literal("autoFishing now"));
                } else if (autoFishing) {
                    autoFishing = false;
                    mc.options.pauseOnLostFocus = true;
                    Utils.AthenaPrint(Text.literal("autoFishing stopped"));
                }
                mc.options.useKey.setPressed(false);
            }

            if (!holdingRod && autoFishing) {
                autoFishing = false;
                Utils.AthenaPrint(Text.literal("autoFishing stopped"));
            }

            if (autoFishing) {
                if (mc.player.fishHook == null && ticksUntilUse == -1 && System.currentTimeMillis() - lastRod > 3000) {
                    ticksUntilUse = Utils.randIntBetween(3, 7);
                }
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

                        lastRod = System.currentTimeMillis();
                        mc.player.swingHand(mc.player.getActiveHand());
                        mc.interactionManager.interactItem(mc.player, mc.player.getActiveHand());
                    }
                }
            } else {
                ticksUntilUse = -1;
            }
        });

        ClientReceiveMessageEvents.GAME.register((msg, bool) -> {
            String text = msg.getString();

            if (text.contains("Golden Fish escapes") || text.contains("Golden Fish is weak")) {
                if (ticksUntilUse == -1) ticksUntilUse = Utils.randIntBetween(8, 16);
            }
        });

        HudRenderCallback.EVENT.register(((drawContext, renderTickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            if (!AthenaConfig.statusIndicator) return;
            if (!holdingRod) return;

            if (autoFishing) {
                RenderUtils.drawStatusLabel(drawContext, "Athena", Color.GREEN);
            } else {
                RenderUtils.drawStatusLabel(drawContext, "Athena", Color.RED);
            }
        }));
    }
}
