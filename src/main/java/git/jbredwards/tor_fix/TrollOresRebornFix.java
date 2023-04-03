package git.jbredwards.tor_fix;

import com.kashdeya.trolloresreborn.handlers.ConfigHandler;
import com.kashdeya.trolloresreborn.handlers.TOREventHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author jbred
 *
 */
@Mod.EventBusSubscriber
@Mod(modid = "tor_fix", name = "Troll Ores Reborn Fix", version = "1.0.0", dependencies = "required-after:tor@[4.2.16,)")
public final class TrollOresRebornFix
{
    @Mod.EventHandler
    static void removeOldHandler(@Nonnull FMLPostInitializationEvent event) {
        final Map<Object, ArrayList<IEventListener>> listeners = ObfuscationReflectionHelper.getPrivateValue(EventBus.class, MinecraftForge.EVENT_BUS, "listeners");
        for(Object key : listeners.keySet()) {
            if(key instanceof TOREventHandler) {
                MinecraftForge.EVENT_BUS.unregister(key);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onHarvest(@Nonnull BlockEvent.HarvestDropsEvent event) {
        final World world = event.getWorld();
        if(!world.isRemote && (!event.isSilkTouching() || !ConfigHandler.SILK_IMMUNITY)) {
            final EntityPlayer player = event.getHarvester();
            if(!(player instanceof FakePlayer) && isAllowedBlock(event.getState(), world, event.getPos(), event.getHarvester())) {
                
            }
        }
    }

    static boolean isAllowedBlock(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
        //check whitelist
        final String blockId = state.getBlock().delegate.name().toString();
        if(ConfigHandler.EXTRA_ORES.contains(blockId)) return true;
        final String stateId = blockId + ':' + state.getBlock().getMetaFromState(state);
        if(ConfigHandler.EXTRA_ORES.contains(stateId)) return true;

        //check blacklist
        else if(TrollOresRebornFixConfigHandler.BLACKLIST.contains(blockId) || TrollOresRebornFixConfigHandler.BLACKLIST.contains(stateId))
            return false;

        //check oredict
        final Vec3d eyePos = player.getPositionEyes(1);
        final RayTraceResult trace = world.rayTraceBlocks(eyePos, eyePos.add(player.getLookVec().scale(player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue())));
        for(int oreId : OreDictionary.getOreIDs(state.getBlock().getPickBlock(state, trace, world, pos, player))) if(OreDictionary.getOreName(oreId).startsWith("ore")) return true;
        return false;
    }
}
