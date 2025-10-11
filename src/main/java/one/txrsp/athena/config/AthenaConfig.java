package one.txrsp.athena.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Config(value="Athena (HKTools)")
public class AthenaConfig {
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
    public static boolean autoPetSwitch = true;

    @ConfigEntry(id="showActionPoints", translation="show action points")
    public static boolean showActionPoints = false;

    @ConfigEntry(id="actionPoints", translation="action points")
    @ConfigOption.Hidden()
    public static String[] actionPoints = new String[]{};

    @ConfigEntry(id="autoFisher", translation="autoFisher")
    public static boolean autoFisher = true;
}
