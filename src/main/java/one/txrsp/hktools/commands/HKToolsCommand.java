package one.txrsp.hktools.commands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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
                    HKPrint(context, Text.literal("hi ").formatted(Formatting.WHITE).append(Text.literal("<3").formatted(Formatting.BOLD)));
                    MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreenAndRender(ResourcefulConfigScreen.getFactory(HKTools.MOD_ID).apply(null)));
                    return 1;
                })
                .then(literal("points")
                    .then(literal("delete")
                        .executes(context -> {
                            boolean removed = FramignAuto.actionPointsList.removeIf(s -> s.startsWith(MinecraftClient.getInstance().player.getBlockPos().toString()));
                            if (removed) HKPrint(context, Text.literal("removed this point").formatted(Formatting.WHITE));
                            else HKPrint(context, Text.literal("no point to remove").formatted(Formatting.WHITE));
                            HKConfig.actionPoints = FramignAuto.actionPointsList.toArray(new String[0]);
                            HKTools.CONFIG.saveConfig(HKConfig.class);
                            return 1;
                        })
                    )
                    .then(literal("add")
                        .then(argument("args", StringArgumentType.string())
                            .executes(context -> {
                                String keys = StringArgumentType.getString(context, "args").toUpperCase();
                                for (char c : keys.toCharArray()) {
                                    if ("WASD.".indexOf(c) == -1) {
                                        HKPrint(context, Text.literal("only WASD or . allowed").formatted(Formatting.WHITE));
                                        return 1;
                                    }
                                }
                                if (!FramignAuto.actionPointsList.contains(MinecraftClient.getInstance().player.getBlockPos().toString() + keys)) {
                                    FramignAuto.actionPointsList.add(MinecraftClient.getInstance().player.getBlockPos().toString() + keys);
                                    HKConfig.actionPoints = FramignAuto.actionPointsList.toArray(new String[0]);
                                    HKTools.CONFIG.saveConfig(HKConfig.class);
                                    HKPrint(context, Text.literal("added this point").formatted(Formatting.WHITE));
                                }
                                else {
                                    HKPrint(context, Text.literal("already a point here").formatted(Formatting.WHITE));
                                }
                                return 1;
                            })
                        )
                    )
                )
        ));
    }

    public static void HKPrint(CommandContext<FabricClientCommandSource> ctx, Text text) {
        ctx.getSource().sendFeedback(Text.literal("[HKTools] ").formatted(Formatting.LIGHT_PURPLE).append(text));
    }
}
