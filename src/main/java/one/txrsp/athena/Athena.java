package one.txrsp.athena;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import one.txrsp.athena.commands.AthenaCommand;
import one.txrsp.athena.config.AthenaConfig;
import one.txrsp.athena.features.*;
import one.txrsp.athena.pathfinding.PathFollower;
import one.txrsp.athena.utils.KeyPressHelper;
import one.txrsp.athena.utils.OnceAgain;
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
		Path newNewPath = configDir.resolve("athena.jsonc");

		// migrate old config name
		if (Files.exists(oldPath) && !Files.exists(newPath)) {
			migrateConfig(oldPath, newPath);
		}
		else if (Files.exists(newPath) && !Files.exists(newNewPath)) {
			migrateConfig(newPath, newNewPath);
		}

		AthenaCommand.init();
		FramignAuto.init();
		PathFollower.init();
		PestESP.init();
		SimpleAutoFisher.init();
		KeyPressHelper.init();
		OnceAgain.init();
		PetSwitcher.init();
		AutoVisitors.init();
		CONFIG.register(AthenaConfig.class);
	}

	private static void migrateConfig(Path oldPath, Path newPath) {
		try {
			Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("Migrated old config: " + oldPath.getFileName() + " → " + newPath.getFileName());

			//Files.deleteIfExists(oldPath);
			LOGGER.info("Deleted old config: " + oldPath.getFileName());
		} catch (IOException e) {
			LOGGER.info("Failed to migrate config: " + e.getMessage());
		}
	}
}