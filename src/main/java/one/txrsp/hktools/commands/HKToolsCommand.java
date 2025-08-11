package one.txrsp.hktools.commands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen;
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import com.teamresourceful.resourcefulconfig.client.ConfigsScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import one.txrsp.hktools.HKTools;
import one.txrsp.hktools.config.HKConfig;
import one.txrsp.hktools.features.FramignAuto;

public class HKToolsCommand {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            literal("hktools")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("[HKTools] ").formatted(Formatting.LIGHT_PURPLE).append(Text.literal("hi ").formatted(Formatting.WHITE)).append(Text.literal("<3").formatted(Formatting.BOLD)));
                    MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreenAndRender(ResourcefulConfigScreen.getFactory(HKTools.MOD_ID).apply(null)));
                    return 1;
                })
        ));
    }
}
