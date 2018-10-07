package com.wynprice.boneophone.entity;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHandSide;

public class MusicalSkeletonModel extends ModelBiped { //TODO: move to own model

    public MusicalSkeletonModel(float modelSize) {
        super(modelSize, 0.0F, 64, 32);
        this.bipedRightArm = new ModelRenderer(this, 40, 16);
        this.bipedRightArm.addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2, modelSize);
        this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
        this.bipedLeftArm = new ModelRenderer(this, 40, 16);
        this.bipedLeftArm.mirror = true;
        this.bipedLeftArm.addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2, modelSize);
        this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        this.bipedRightLeg = new ModelRenderer(this, 0, 16);
        this.bipedRightLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2, modelSize);
        this.bipedRightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);
        this.bipedLeftLeg = new ModelRenderer(this, 0, 16);
        this.bipedLeftLeg.mirror = true;
        this.bipedLeftLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2, modelSize);
        this.bipedLeftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
    }

    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn)
    {
//        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entityIn, float limbSwing, float limbSwingAmount, float partialTickTime) {

        if(entityIn instanceof MusicalSkeleton) {

            MusicalSkeleton skeleton = (MusicalSkeleton) entityIn;

            boolean doLeft = skeleton.leftTicksFromHit <= MusicalSkeleton.ticksToHit;
            boolean doRight = skeleton.rightTicksFromHit <= MusicalSkeleton.ticksToHit;

            float rad = (float) Math.toRadians(90F);

            if(doLeft) {
                float lLerpHit = (skeleton.leftTicksFromHit + partialTickTime) / MusicalSkeleton.ticksToHit;
                float targetL = (skeleton.prevLeftTargetHit + (skeleton.leftTargetHit - skeleton.prevLeftTargetHit) * lLerpHit) - 0.5F;

                this.bipedLeftArm.rotateAngleY = targetL * rad;
                this.bipedLeftArm.rotateAngleX = (float) Math.min(Math.toRadians(-45F + 45F * lLerpHit), 0F);
            }

            if(doRight) {
                float rLerpHit = (skeleton.rightTicksFromHit + partialTickTime) / MusicalSkeleton.ticksToHit;
                float targetR = (skeleton.prevRightTargetHit + (skeleton.rightTargetHit - skeleton.prevRightTargetHit) * rLerpHit) - 0.5F;

                this.bipedRightArm.rotateAngleY = targetR * rad;
                this.bipedRightArm.rotateAngleX = (float) Math.min(Math.toRadians(-45F + 45F * rLerpHit), 0F);
            }
        }

    }

    public void postRenderArm(float scale, EnumHandSide side)
    {
        float f = side == EnumHandSide.RIGHT ? 1.0F : -1.0F;
        ModelRenderer modelrenderer = this.getArmForSide(side);
        modelrenderer.rotationPointX += f;
        modelrenderer.postRender(scale);
        modelrenderer.rotationPointX -= f;
    }
}
