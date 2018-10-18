package com.wynprice.boneophone;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.entity.MusicalSkeletonRenderer;
import com.wynprice.boneophone.entity.ThrowableNoteEntity;
import com.wynprice.boneophone.entity.ThrowableNoteRenderer;
import com.wynprice.boneophone.midi.MidiFileHandler;
import com.wynprice.boneophone.midi.MidiStream;
import com.wynprice.boneophone.network.C1UploadMidiFile;
import com.wynprice.boneophone.network.S0MusicalSkeletonStateUpdate;
import com.wynprice.boneophone.network.S2SyncAndPlayMidi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.crash.CrashReport;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.NoteBlockEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;

@Mod(modid = SkeletalBand.MODID, name = SkeletalBand.NAME, version = SkeletalBand.VERSION)
@Mod.EventBusSubscriber
public class SkeletalBand {
    public static final String MODID = "skeletalband";
    public static final String NAME = "SkeletalBand";
    public static final String VERSION = "1.0.4";

    public static Logger LOGGER;

    public static MidiStream SPOOKY = null;

    public static SimpleNetworkWrapper NETWORK = new SimpleNetworkWrapper(MODID);

    @GameRegistry.ObjectHolder(MODID + ":throwable_note")
    public static Item THROWABLE_NOTE;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        this.registerPackets();
    }

    private void registerPackets() {
        NETWORK.registerMessage(new S0MusicalSkeletonStateUpdate.Handler(), S0MusicalSkeletonStateUpdate.class, 0, Side.CLIENT);
        NETWORK.registerMessage(new C1UploadMidiFile.Handler(), C1UploadMidiFile.class, 1, Side.SERVER);
        NETWORK.registerMessage(new S2SyncAndPlayMidi.Handler(), S2SyncAndPlayMidi.class, 2, Side.CLIENT);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            SPOOKY = MidiStream.getMidi(new ResourceLocation(MODID, "spook"));
            new Thread(() -> {
                String base = "https://raw.githubusercontent.com/Wyn-Price/SkeletalBand/master/midis/";
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(base + "midis.json").openStream()))) {
                    JsonObject json = new JsonParser().parse(bufferedReader).getAsJsonObject();
                    LOGGER.info("Downloaded midi json file");
                    for (JsonElement elements : JsonUtils.getJsonArray(json, "midis")) {
                        String name = JsonUtils.getString(elements, "midis");
                        File out = new File(MidiFileHandler.folder, name);
                        if(!out.exists()) {
                            Files.copy(new URL(base + name).openStream(), out.toPath());
                            LOGGER.info("Successfully downloaded and copied " + name);
                        } else {
                            LOGGER.info("Skipping download of {} as it already exists locally", name);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error(CrashReport.makeCrashReport(e, "Failed to receive data from URL").getCompleteReport());
                }

            }).start();
            registerItemColors();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerItemColors() {
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler(
                (stack, tintIndex) -> {
                    float param = stack.getOrCreateSubCompound(MODID).getInteger("Note") / 24F;
                    int red = (int) ((MathHelper.sin((param + 0.0F) * ((float)Math.PI * 2F)) * 0.65F + 0.35F) * 255F);
                    int green = (int) ((MathHelper.sin((param + 1F/3F) * ((float)Math.PI * 2F)) * 0.65F + 0.35F) * 255F);
                    int blue = (int) ((MathHelper.sin((param + 2/3F) * ((float)Math.PI * 2F)) * 0.65F + 0.35F) * 255F);

                    return ((red & 0xFF) << 16) |
                            ((green & 0xFF) << 8)  |
                            (blue & 0xFF);
                },
                THROWABLE_NOTE
        );
    }

    @SubscribeEvent
    public static void onItemRegister(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(
                new ThrowableNote().setRegistryName("throwable_note").setUnlocalizedName("throwable_note").setCreativeTab(new CreativeTabs(MODID) {

                    @Override
                    @SideOnly(Side.CLIENT)
                    public ItemStack getIconItemStack() {
                        return ThrowableNote.fromNote((int) ((Minecraft.getMinecraft().world.getTotalWorldTime() / 20) % 25), 0);
                    }

                    @Override
                    public ItemStack getTabIconItem() {
                        return ItemStack.EMPTY;
                    }
                })
        );
    }

    @SubscribeEvent
    public static void onEntityRegister(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().registerAll(
                EntityEntryBuilder.create()
                        .id(new ResourceLocation(MODID, "musical_skeleton"), 0)
                        .name("musical_skeleton")
                        .tracker(64, 1, true)
                        .entity(MusicalSkeleton.class)
                        .factory(MusicalSkeleton::new)
                        .egg(0xC1C1C1, 0x494949) //TODO: better color
                        .build(),

                EntityEntryBuilder.create()
                        .id(new ResourceLocation(MODID, "throwable_note"), 1)
                        .name("throwable_note")
                        .tracker(64, 10, true)
                        .entity(ThrowableNoteEntity.class)
                        .factory(ThrowableNoteEntity::new)
                        .build()
        );
    }

    @SubscribeEvent
    public static void onNotePlay(NoteBlockEvent.Play event) {
        World world  = event.getWorld();
        BlockPos pos = event.getPos();
        if(!world.isRemote) {
            for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos.up(), pos.add(1, 2, 1)))) {
                if(player.isSneaking()) {
                    ItemStack stack = ThrowableNote.fromNote(event.getVanillaNoteId(), event.getInstrument().ordinal());
                    if(!player.inventory.addItemStackToInventory(stack)) {
                        player.dropItem(stack, false);
                    }
                    event.setCanceled(true);
                    break;
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(MusicalSkeleton.class, MusicalSkeletonRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ThrowableNoteEntity.class, ThrowableNoteRenderer::new);

        ModelLoader.setCustomModelResourceLocation(THROWABLE_NOTE, 0, new ModelResourceLocation(Objects.requireNonNull(THROWABLE_NOTE.getRegistryName()), "inventory"));
    }
}
