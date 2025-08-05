package one.txrsp.hktools;

import net.fabricmc.api.ClientModInitializer;

import one.txrsp.hktools.commands.HKToolsCommand;
import one.txrsp.hktools.features.FramignAuto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HKTools implements ClientModInitializer {
	public static final String MOD_ID = "hktools";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("\n---------------------------------\nHKTools says hi!\n---------------------------------");

		HKToolsCommand.init();
		FramignAuto.init();
	}
}