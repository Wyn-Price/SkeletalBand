package com.wynprice.boneophone.entity;

import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class MusicalSkeletonRenderer extends RenderBiped<MusicalSkeleton> {
    private static final ResourceLocation SKELETON_TEXTURES = new ResourceLocation("textures/entity/skeleton/skeleton.png");


    public MusicalSkeletonRenderer(RenderManager renderManagerIn) {
        super(renderManagerIn, new MusicalSkeletonModel(0F), 0.1F);
    }

    @Override
    protected ResourceLocation getEntityTexture(MusicalSkeleton entity) {
        return SKELETON_TEXTURES;
    }
}
