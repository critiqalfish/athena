package one.txrsp.athena.commands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static one.txrsp.athena.utils.Crops.getCropForTool;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import one.txrsp.athena.Athena;
import one.txrsp.athena.config.AthenaConfig;
import one.txrsp.athena.features.FramignAuto;
import one.txrsp.athena.utils.Crops;

import static one.txrsp.athena.Athena.LOGGER;

public class AthenaCommand {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            literal("athena")
                .executes(context -> {
                    AthenaPrint(context, Text.literal("hi ").formatted(Formatting.WHITE).append(Text.literal("<3").formatted(Formatting.BOLD)));
                    MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreenAndRender(ResourcefulConfigScreen.getFactory(Athena.MOD_ID).apply(null)));
                    return 1;
                })
                .then(literal("points")
                    .then(literal("delete")
                        .executes(context -> {
                            boolean removed = FramignAuto.actionPointsList.removeIf(s -> s.startsWith(MinecraftClient.getInstance().player.getBlockPos().toString()));
                            if (removed) AthenaPrint(context, Text.literal("removed this point").formatted(Formatting.WHITE));
                            else AthenaPrint(context, Text.literal("no point to remove").formatted(Formatting.WHITE));
                            AthenaConfig.actionPoints = FramignAuto.actionPointsList.toArray(new String[0]);
                            Athena.CONFIG.saveConfig(AthenaConfig.class);
                            return 1;
                        })
                    )
                    .then(literal("add")
                        .then(argument("args", StringArgumentType.string())
                            .executes(context -> {
                                String keys = StringArgumentType.getString(context, "args").toUpperCase();
                                for (char c : keys.toCharArray()) {
                                    if ("WASD.".indexOf(c) == -1) {
                                        AthenaPrint(context, Text.literal("only WASD or . allowed").formatted(Formatting.WHITE));
                                        return 1;
                                    }
                                }
                                if (!FramignAuto.actionPointsList.contains(MinecraftClient.getInstance().player.getBlockPos().toString() + keys)) {
                                    String cropName = Crops.getCropForTool(MinecraftClient.getInstance().player.getInventory().getSelectedStack().getName().getString()).name();
                                    LOGGER.info(cropName);
                                    if (cropName.equals("NONE")) {
                                        AthenaPrint(context, Text.literal("hold a farming tool in your hand").formatted(Formatting.WHITE));
                                        return 1;
                                    }
                                    FramignAuto.actionPointsList.add(cropName + "|" + MinecraftClient.getInstance().player.getBlockPos().toString() + keys);
                                    AthenaConfig.actionPoints = FramignAuto.actionPointsList.toArray(new String[0]);
                                    Athena.CONFIG.saveConfig(AthenaConfig.class);
                                    AthenaPrint(context, Text.literal("added this point").formatted(Formatting.WHITE));
                                }
                                else {
                                    AthenaPrint(context, Text.literal("already a point here").formatted(Formatting.WHITE));
                                }
                                return 1;
                            })
                        )
                    )
                )
        ));
    }

    public static void AthenaPrint(CommandContext<FabricClientCommandSource> ctx, Text text) {
        ctx.getSource().sendFeedback(Text.literal("[Athena] ").formatted(Formatting.LIGHT_PURPLE).append(text));
    }
}
