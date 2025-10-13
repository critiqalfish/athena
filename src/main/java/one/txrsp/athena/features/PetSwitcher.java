package one.txrsp.athena.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import one.txrsp.athena.utils.OnceAgain;
import one.txrsp.athena.utils.Utils;

import java.util.Objects;

import static one.txrsp.athena.Athena.LOGGER;

public class PetSwitcher {
    public static boolean isSwitching = false;
    public static boolean wasSwitching = false;
    public static String targetPet;
    public static String activePet;
    private static int ticksToNotSwitching = -1;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            Utils.getTablistLines().forEach(entry -> {
                if (entry.startsWith("[Lvl")) {
                    activePet = entry.substring(entry.indexOf("]") + 1).replace("✦", "").strip();
                }
            });

            if (isSwitching) {
                if (ticksToNotSwitching == 0) {
                    isSwitching = false;
                    wasSwitching = true;
                }
                if (ticksToNotSwitching != -1) ticksToNotSwitching--;
                //LOGGER.info("" + ticksToNotSwitching);

                if (ticksToNotSwitching == -1) {
                    wasSwitching = false;
                    if (mc.currentScreen != null && !mc.currentScreen.getTitle().getString().startsWith("Pets")) {
                        OnceAgain.executeWithCooldown("screenclose", 20, () -> {MinecraftClient.getInstance().currentScreen.close();});
                    } else if (mc.currentScreen == null) {
                        OnceAgain.executeWithCooldown("petscommand", 60, () -> {
                            MinecraftClient.getInstance().player.networkHandler.sendChatCommand("pets");
                        });
                    }

                    if (mc.currentScreen != null && mc.currentScreen.getTitle().getString().startsWith("Pets")) {
                        if (mc.currentScreen instanceof HandledScreen<?> handledScreen) {
                            ScreenHandler handler = handledScreen.getScreenHandler();
                            int notEmpty = 0;
                            for (int i = 0; i < handler.slots.size(); i++) {
                                var slot = handler.slots.get(i);
                                var stack = slot.getStack();

                                if (i < 54 && !stack.isEmpty()) notEmpty++;
                            }

                            if (notEmpty >= 54) {
                                for (int i = 0; i < handler.slots.size(); i++) {
                                    var slot = handler.slots.get(i);
                                    var stack = slot.getStack();

                                    if (!stack.isEmpty() && stack.getCustomName().getString().contains(targetPet)) {
                                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                                        ticksToNotSwitching = 10;
                                    }
                                }
                            }
                        }
                    }
                }

            }
        });
    }

    public static boolean switchPet(String pet) {
        if (!isSwitching && !Objects.equals(pet, activePet)) {
            isSwitching = true;
            targetPet = pet;
            Utils.AthenaPrint(Text.literal("switching pet to " + pet));
        }
        return true;
    }
}
