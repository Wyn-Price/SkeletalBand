package com.wynprice.boneophone.entity;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class MusicalSkeletonRenderer extends RenderBiped<MusicalSkeleton> {
    private static final ResourceLocation SKELETON_TEXTURES = new ResourceLocation("textures/entity/skeleton/skeleton.png");


    public MusicalSkeletonRenderer(RenderManager renderManagerIn) {
        super(renderManagerIn, new MusicalSkeletonModel(0F), 0.5F);
    }


    @Override
    public void doRender(MusicalSkeleton entity, double x, double y, double z, float entityYaw, float partialTicks) {

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected void applyRotations(MusicalSkeleton entity, float p_77043_2_, float rotationYaw, float partialTicks) {
        if(entity.isPlaying) {
            GlStateManager.translate(0, -0.7F, 0);
        } else if(entity.isKeyboard) {
            GlStateManager.rotate(90, 1, 0, 0);
            GlStateManager.translate(0, -entity.height / 2 - 0.3, -0.3);
        }
        super.applyRotations(entity, p_77043_2_, rotationYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(MusicalSkeleton entity) {
        return SKELETON_TEXTURES;
    }
}
