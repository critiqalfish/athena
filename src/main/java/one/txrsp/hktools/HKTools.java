package one.txrsp.hktools;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import net.fabricmc.api.ClientModInitializer;

import one.txrsp.hktools.commands.HKToolsCommand;
import one.txrsp.hktools.config.HKConfig;
import one.txrsp.hktools.features.FramignAuto;
import one.txrsp.hktools.features.PestESP;
import one.txrsp.hktools.pathfinding.PathFollower;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HKTools implements ClientModInitializer {
	public static final String MOD_ID = "hktools";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Configurator CONFIG = new Configurator(MOD_ID);
	public static boolean DEBUG = false;

	@Override
	public void onInitializeClient() {
		LOGGER.info("\n---------------------------------\nHKTools says hi!\n---------------------------------");

		HKToolsCommand.init();
		FramignAuto.init();
		PathFollower.init();
		PestESP.init();
		CONFIG.register(HKConfig.class);
	}
}