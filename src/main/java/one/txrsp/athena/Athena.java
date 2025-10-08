package one.txrsp.athena;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import one.txrsp.athena.commands.AthenaCommand;
import one.txrsp.athena.config.AthenaConfig;
import one.txrsp.athena.features.FramignAuto;
import one.txrsp.athena.features.PestESP;
import one.txrsp.athena.pathfinding.PathFollower;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Athena implements ClientModInitializer {
	public static final String MOD_ID = "athena";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Configurator CONFIG = new Configurator(MOD_ID);
	public static boolean DEBUG = false;

	@Override
	public void onInitializeClient() {
		LOGGER.info("\n---------------------------------\nAthena says hi!\n---------------------------------");

		Path configDir = FabricLoader.getInstance().getConfigDir();
		Path oldPath = configDir.resolve("HKTools.jsonc");
		Path newPath = configDir.resolve("Athena (HKTools).jsonc");

		// migrate old config name
		if (Files.exists(oldPath) && !Files.exists(newPath)) {
			try {
				Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("[Athena] Migrated old config: " + oldPath.getFileName() + " → " + newPath.getFileName());

				Files.deleteIfExists(oldPath);
				System.out.println("[Athena] Deleted old config: " + oldPath.getFileName());
			} catch (IOException e) {
				System.err.println("[Athena] Failed to migrate config: " + e.getMessage());
			}
		}

		AthenaCommand.init();
		FramignAuto.init();
		PathFollower.init();
		PestESP.init();
		CONFIG.register(AthenaConfig.class);
	}
}