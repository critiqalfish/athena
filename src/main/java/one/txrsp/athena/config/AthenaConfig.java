package one.txrsp.athena.config;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo.Link;
import one.txrsp.athena.Athena;

@Config(value= Athena.MOD_ID,
        categories = {
            AthenaConfig.Farming.class,
            AthenaConfig.Fishing.class
        }
)
@ConfigInfo(
        titleTranslation = "Athena (HKTools)",
        descriptionTranslation = "i love hello kitty",
        links = {
                @Link(text = "help", icon = "", value = "https://athena.txrsp.one")
        }
)
public class AthenaConfig {
    @ConfigEntry(id="statusIndicator", translation="Athena Status Indicator")
    public static boolean statusIndicator = true;

    @Category(value = "farming")
    @ConfigInfo(
            titleTranslation = "Farming",
            descriptionTranslation = "Farming, Pest-killing and Garden stuff"
    )
    public static class Farming {
        @ConfigEntry(id="yaw", translation="yaw")
        @ConfigOption.Range(min=-180, max=180)
        public static float yaw = 0;

        @ConfigEntry(id="pitch", translation="pitch")
        @ConfigOption.Range(min=-90, max=90)
        public static float pitch = 0;

        @ConfigEntry(id="pestESP", translation="pestESP")
        public static boolean pestESP = false;

        @ConfigEntry(id="autoPest", translation="autoPest")
        public static boolean autoPest = false;

        @ConfigEntry(id="autoPestThreshold", translation="autoPest threshold")
        @ConfigOption.Range(min=1, max=8)
        public static int autoPestThreshold = 4;

        @ConfigEntry(id="autoPestWarpWait", translation="autoPest wait for warp")
        public static boolean autoPestWarpWait = true;

        @ConfigEntry(id="autoPetSwitch", translation="auto hedgehog/mcow switcher")
        public static boolean autoPetSwitch = false;

        @ConfigEntry(id="showActionPoints", translation="show action points")
        public static boolean showActionPoints = false;

        @ConfigEntry(id="actionPoints", translation="action points")
        @ConfigOption.Hidden()
        public static String[] actionPoints = new String[]{};
    }

    @Category(value = "fishing")
    @ConfigInfo(
            titleTranslation = "Fishing",
            descriptionTranslation = "fishing stuff"
    )
    public static class Fishing {
        @ConfigEntry(id="autoFisher", translation="autoFisher")
        public static boolean autoFisher = true;
    }
}
