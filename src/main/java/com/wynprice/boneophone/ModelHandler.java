package com.wynprice.boneophone;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = SkeletalBand.MODID)
public class ModelHandler {

    //Do i really need to store the guitars in the main texture map?
    public static IBakedModel GUITAR_MODEL;

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent event) {
        GUITAR_MODEL = getModel(new ResourceLocation(SkeletalBand.MODID, "instruments/guitar"), event.getMap());
    }

    private static IBakedModel getModel(ResourceLocation location, TextureMap map) {
        IModel model;
        try {
            model = ModelLoaderRegistry.getModel(location);
        } catch (Exception e) {
            SkeletalBand.LOGGER.error("Unable to load model " + location, e);
            model = ModelLoaderRegistry.getMissingModel();
        }
        return model.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, map::registerSprite);
    }
}
