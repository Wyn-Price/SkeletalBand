package com.wynprice.boneophone;

import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.entity.MusicalSkeletonRenderer;
import com.wynprice.boneophone.midi.MidiStream;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

@Mod(modid = Boneophone.MODID, name = Boneophone.NAME, version = Boneophone.VERSION)
@Mod.EventBusSubscriber
public class Boneophone
{
    public static final String MODID = "boneophone";
    public static final String NAME = "Boneophone";
    public static final String VERSION = "1.0";

    private static Logger logger;

    public static MidiStream SPOOKY = null;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        SPOOKY = MidiStream.getMidi(new ResourceLocation(MODID, "spook"));
    }

    @SubscribeEvent
    public static void onEntityRegister(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(
                EntityEntryBuilder.create()
                        .id(new ResourceLocation(MODID, "musical_skeleton"), 0)
                        .name("musical_skeleton")
                        .tracker(64, 1, true)
                        .entity(MusicalSkeleton.class)
                        .factory(MusicalSkeleton::new)
                        .egg(0xC1C1C1, 0x494949) //TODO: better color
                        .build()
        );
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(MusicalSkeleton.class, MusicalSkeletonRenderer::new);
    }
}
