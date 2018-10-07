package com.wynprice.boneophone.entity;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class MusicalSkeletonRenderer extends RenderBiped<MusicalSkeleton> {
    private static final ResourceLocation SKELETON_TEXTURES = new ResourceLocation("textures/entity/skeleton/skeleton.png");


    public MusicalSkeletonRenderer(RenderManager renderManagerIn) {
        super(renderManagerIn, new MusicalSkeletonModel(0F), 0.1F);
    }

    @Override
    public void doRender(MusicalSkeleton entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if(this.mainModel instanceof ModelBiped) {
            ModelBiped model = ((ModelBiped) this.mainModel);
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(MusicalSkeleton entity) {
        return SKELETON_TEXTURES;
    }
}
