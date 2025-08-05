package one.txrsp.hktools.commands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

public class HKToolsCommand {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            literal("hktools")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("hi <3"));
                    return 1;
                })
        ));
    }
}
