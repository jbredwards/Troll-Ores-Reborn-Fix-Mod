package git.jbredwards.tor_fix;

import com.kashdeya.trolloresreborn.entity.EntityOreTroll;
import com.kashdeya.trolloresreborn.entity.EntitySmallWither;
import com.kashdeya.trolloresreborn.handlers.ConfigHandler;
import com.kashdeya.trolloresreborn.handlers.TOREventHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumDifficulty;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author jbred
 *
 */
@Mod.EventBusSubscriber
@Mod(modid = "tor_fix", name = "Troll Ores Reborn Fix", version = "1.0.0", dependencies = "required-after:tor@[4.2.16,)", serverSideOnly = true)
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onHarvest(@Nonnull BlockEvent.HarvestDropsEvent event) {
        final World world = event.getWorld();
        if(!world.isRemote && world.getDifficulty() != EnumDifficulty.PEACEFUL && (!event.isSilkTouching() || !ConfigHandler.SILK_IMMUNITY) && isAllowedHarvester(event.getHarvester())) {
            final float chance = ConfigHandler.FORTUNE_MULTIPLIER ? ConfigHandler.CHANCE * (event.getFortuneLevel() + 1) : ConfigHandler.CHANCE;
            if(world.rand.nextFloat() < chance && world.getGameRules().getBoolean("doTileDrops") && isAllowedBlock(event.getState(), world, event.getPos())) {
                //spawn trolls
                if(world.rand.nextInt(100) < ConfigHandler.TROLL_PRECENT) {
                    for(int i = 0; i < ConfigHandler.TROLL_SPAWN; i++) {
                        final EntityOreTroll entity = new EntityOreTroll(world);
                        entity.setLocationAndAngles(event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, 0, 0);

                        final ItemStack blockStack = event.getState().getBlock().getItem(world, event.getPos(), event.getState());
                        if(!blockStack.isEmpty()) {
                            entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, blockStack);
                            entity.setDropChance(EntityEquipmentSlot.MAINHAND, 0);

                            final IItemHandler wrapper = new ItemStackHandler(entity.inventory);
                            event.getDrops().forEach(stack -> ItemHandlerHelper.insertItemStacked(wrapper, stack, false));
                            event.setDropChance(0);
                        }

                        world.spawnEntity(entity);
                        final BlockPos entityPos = new BlockPos(entity);
                        entity.onInitialSpawn(world.getDifficultyForLocation(entityPos), null);

                        //break surrounding blocks
                        if(i == 0) breakSurroundingBlocks(entity.getEntityBoundingBox(), entityPos, world);
                    }
                }
                //spawn wither
                else if(ConfigHandler.ENABLE_WITHER) {
                    final EntitySmallWither entity = new EntitySmallWither(world);
                    entity.setLocationAndAngles(event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, 0, 0);
                    world.spawnEntity(entity);

                    entity.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entity)), null);
                    if(!(event.getHarvester() instanceof FakePlayer)) entity.setAttackTarget(event.getHarvester());
                }
            }
        }
    }

    static boolean isAllowedHarvester(@Nullable EntityPlayer player) {
        return player != null && (ConfigHandler.FAKE_PLAYERS || !(player instanceof FakePlayer)) && !player.isCreative();
    }

    static boolean isAllowedBlock(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos) {
        //check whitelist
        final String blockId = state.getBlock().delegate.name().toString();
        if(ConfigHandler.EXTRA_ORES.contains(blockId)) return true;
        final String stateId = blockId + ':' + state.getBlock().getMetaFromState(state);
        if(ConfigHandler.EXTRA_ORES.contains(stateId)) return true;

        //check blacklist
        else if(TrollOresRebornFixConfigHandler.BLACKLIST.contains(blockId) || TrollOresRebornFixConfigHandler.BLACKLIST.contains(stateId))
            return false;

        //check oredict
        for(int oreId : OreDictionary.getOreIDs(state.getBlock().getItem(world, pos, state))) {
            final String ore = OreDictionary.getOreName(oreId);
            if(ore.startsWith("ore")) return true;
        }

        return false;
    }

    static void breakSurroundingBlocks(@Nonnull AxisAlignedBB bb, @Nonnull BlockPos origin, @Nonnull World world) {
        final int mX = MathHelper.floor(bb.minX);
        final int mY = MathHelper.floor(bb.minY);
        final int mZ = MathHelper.floor(bb.minZ);
        for(int y = mY; y < bb.maxY; y++) {
            for(int x = mX; x < bb.maxX; x++) {
                for(int z = mZ; z < bb.maxZ; z++) {
                    final BlockPos pos = new BlockPos(x, y, z);
                    if(!origin.equals(pos)) {
                        final float hardness = world.getBlockState(pos).getBlockHardness(world, pos);
                        if(hardness != -1 && hardness < ConfigHandler.BLOCK_HARDNESS) world.destroyBlock(pos, true);
                    }
                }
            }
        }
    }
}
