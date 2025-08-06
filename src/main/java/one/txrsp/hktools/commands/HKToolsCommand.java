package one.txrsp.hktools.commands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import one.txrsp.hktools.features.FramignAuto;

public class HKToolsCommand {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            literal("hktools")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("[HKTools] ").formatted(Formatting.LIGHT_PURPLE).append(Text.literal("hi ").formatted(Formatting.WHITE)).append(Text.literal("<3").formatted(Formatting.BOLD)));
                    return 1;
                })
                .then(literal("rotation")
                        .then(argument("yaw", FloatArgumentType.floatArg(-180, 180))
                        .then(argument("pitch", FloatArgumentType.floatArg(-90, 90))
                            .executes(context -> {
                                FramignAuto.yaw = FloatArgumentType.getFloat(context, "yaw");
                                FramignAuto.pitch = FloatArgumentType.getFloat(context, "pitch");
                                context.getSource().sendFeedback(Text.literal("[HKTools] ").formatted(Formatting.LIGHT_PURPLE).append(Text.literal("set yaw/pitch successfully").formatted(Formatting.WHITE)));
                                return 1;
                            })
                        ))
                )
        ));
    }
}
