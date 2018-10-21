package com.wynprice.boneophone.entity;

import com.wynprice.boneophone.SkeletalBand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ItemLayerModel;

import static net.minecraft.client.renderer.GlStateManager.*;

public class MusicalSkeletonRenderer extends RenderBiped<MusicalSkeleton> {
    private static final ResourceLocation SKELETON_TEXTURES = new ResourceLocation("textures/entity/skeleton/skeleton.png");
    private static final ResourceLocation WITHER_SKELETON_TEXTURES = new ResourceLocation("textures/entity/skeleton/wither_skeleton.png");
    private static final ResourceLocation STRAY_SKELETON_TEXTURES = new ResourceLocation("textures/entity/skeleton/stray.png");


    private final StrayClothingLayer strayLayer = new StrayClothingLayer(this);

    public MusicalSkeletonRenderer(RenderManager renderManagerIn) {
        super(renderManagerIn, new MusicalSkeletonModel(0F, true), 0.5F);
        for (int i = 0; i < this.layerRenderers.size(); i++) {
            if(((LayerRenderer<?>)this.layerRenderers.get(i)) instanceof LayerHeldItem) {
                this.layerRenderers.remove(i);
                this.layerRenderers.add(i, new SkeletonHeldItemLayer(this));
            }
        }
    }


    @Override
    public void doRender(MusicalSkeleton entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if(entity.type == MusicalSkeleton.SkeletonType.STRAY) {
            this.layerRenderers.add(this.strayLayer);
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        if(entity.type == MusicalSkeleton.SkeletonType.STRAY) {
            this.layerRenderers.remove(this.strayLayer);
        }
    }

    @Override
    protected void applyRotations(MusicalSkeleton entity, float p_77043_2_, float rotationYaw, float partialTicks) {
        entity.musicianType.setEntityTranslations();
        super.applyRotations(entity, p_77043_2_, rotationYaw, partialTicks);
    }

    @Override
    public void renderName(MusicalSkeleton entity, double x, double y, double z) {
        if(entity.paused) {
            pushMatrix();
            translate(x, y + entity.height + 0.2F, z);
            glNormal3f(0.0F, 1.0F, 0.0F);
            rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
            rotate((float)(this.renderManager.options.thirdPersonView == 2 ? -1 : 1) * this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
            scale(-0.025F, -0.025F, 0.025F);
            disableLighting();
            depthMask(false);
            enableAlpha();

            enableBlend();

            Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(SkeletalBand.MODID, "textures/misc/pause.png"));

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buff = tess.getBuffer();
            buff.begin(7, DefaultVertexFormats.POSITION_TEX);
            buff.pos(-4, -4, 0.0D).tex(0, 0).endVertex();
            buff.pos(-4, +4, 0.0D).tex(0, 1).endVertex();
            buff.pos(+4, +4, 0.0D).tex(1, 1).endVertex();
            buff.pos(+4, -4, 0.0D).tex(1, 0).endVertex();
            tess.draw();

            depthMask(true);



            enableLighting();
            disableBlend();
            color(1.0F, 1.0F, 1.0F, 1.0F);
            popMatrix();
        }
        super.renderName(entity, x, y, z);
    }

    @Override
    protected void preRenderCallback(MusicalSkeleton entitylivingbaseIn, float partialTickTime) {
        if(entitylivingbaseIn.type == MusicalSkeleton.SkeletonType.WITHER) {
            GlStateManager.scale(1.2F, 1.2F, 1.2F);
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(MusicalSkeleton entity) {
        if(entity.type == null) {
            return SKELETON_TEXTURES;
        }
        switch (entity.type) {
            case STRAY:
                return STRAY_SKELETON_TEXTURES;
            case WITHER:
                return WITHER_SKELETON_TEXTURES;
            default:
                return SKELETON_TEXTURES;
        }
    }

    private static class StrayClothingLayer implements LayerRenderer<MusicalSkeleton>
    {
        private static final ResourceLocation STRAY_CLOTHES_TEXTURES = new ResourceLocation("textures/entity/skeleton/stray_overlay.png");
        private final RenderLivingBase<?> renderer;
        private final MusicalSkeletonModel layerModel = new MusicalSkeletonModel(0.25F, false);

        public StrayClothingLayer(RenderLivingBase<?> renderer) {
            this.renderer = renderer;
        }

        public void doRenderLayer(MusicalSkeleton skeleton, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
        {
            this.layerModel.setModelAttributes(this.renderer.getMainModel());
            this.layerModel.setLivingAnimations(skeleton, limbSwing, limbSwingAmount, partialTicks);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.renderer.bindTexture(STRAY_CLOTHES_TEXTURES);
            this.layerModel.render(skeleton, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        public boolean shouldCombineTextures()
        {
            return true;
        }
    }

    private class SkeletonHeldItemLayer implements LayerRenderer<MusicalSkeleton>
    {
        private final RenderLivingBase<?> livingEntityRenderer;

        public SkeletonHeldItemLayer(RenderLivingBase<?> livingEntityRendererIn) {
            this.livingEntityRenderer = livingEntityRendererIn;
        }

        public void doRenderLayer(MusicalSkeleton entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
        {
            boolean rightHand = entity.getPrimaryHand() == EnumHandSide.RIGHT;
            ItemStack mainhand = entity.musicianType.getHeldItem(EnumHand.OFF_HAND);
            ItemStack offhand = entity.musicianType.getHeldItem(EnumHand.OFF_HAND);

            if (!mainhand.isEmpty() || !offhand.isEmpty()) {
                GlStateManager.pushMatrix();

                if (this.livingEntityRenderer.getMainModel().isChild) {
                    GlStateManager.translate(0.0F, 0.75F, 0.0F);
                    GlStateManager.scale(0.5F, 0.5F, 0.5F);
                }

                this.renderHeldItem(entity,  rightHand ? mainhand : offhand, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, EnumHandSide.RIGHT);
                this.renderHeldItem(entity, rightHand ? offhand : mainhand, ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, EnumHandSide.LEFT);
                GlStateManager.popMatrix();
            }
        }

        private void renderHeldItem(EntityLivingBase p_188358_1_, ItemStack p_188358_2_, ItemCameraTransforms.TransformType p_188358_3_, EnumHandSide handSide)
        {
            if (!p_188358_2_.isEmpty())
            {
                GlStateManager.pushMatrix();

                if (p_188358_1_.isSneaking())
                {
                    GlStateManager.translate(0.0F, 0.2F, 0.0F);
                }
                // Forge: moved this call down, fixes incorrect offset while sneaking.
                this.translateToHand(handSide);
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                boolean flag = handSide == EnumHandSide.LEFT;
                GlStateManager.translate((float)(flag ? -1 : 1) / 16.0F, 0.125F, -0.625F);
                Minecraft.getMinecraft().getItemRenderer().renderItemSide(p_188358_1_, p_188358_2_, p_188358_3_, flag);
                GlStateManager.popMatrix();
            }
        }

        protected void translateToHand(EnumHandSide p_191361_1_)
        {
            ((ModelBiped)this.livingEntityRenderer.getMainModel()).postRenderArm(0.0625F, p_191361_1_);
        }

        public boolean shouldCombineTextures()
        {
            return false;
        }
    }

}
