package git.jbredwards.tor_fix;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author jbred
 *
 */
@Config(modid = "tor_fix")
@Mod.EventBusSubscriber(modid = "tor_fix")
public final class TrollOresRebornFixConfigHandler
{
    @Nonnull
    @Config.LangKey("config.tor.blacklist")
    public static String[] blacklist = new String[0];

    @Nonnull
    @Config.Ignore
    public static final List<String> BLACKLIST = new ArrayList<>();

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onReload(@Nonnull ConfigChangedEvent.OnConfigChangedEvent event) {
        if(event.getModID().equals("tor_fix")) {
            BLACKLIST.clear();
            BLACKLIST.addAll(Arrays.asList(blacklist));
        }
    }
}
