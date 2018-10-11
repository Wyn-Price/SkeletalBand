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
        if(entityIn instanceof MusicalSkeleton && ((MusicalSkeleton) entityIn).isPlaying) {
            limbSwing = limbSwingAmount = netHeadYaw = 0; //Kinda a hack but ok for now
        }

        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);


        if(entityIn instanceof MusicalSkeleton) {

            MusicalSkeleton skeleton = (MusicalSkeleton) entityIn;

            if(skeleton.isPlaying) {
                this.bipedLeftArm.rotateAngleX = skeleton.lx;
                this.bipedLeftArm.rotateAngleY = skeleton.ly;

                this.bipedRightArm.rotateAngleX = skeleton.rx;
                this.bipedRightArm.rotateAngleY = skeleton.ry;


                float angle = (float) (90D * Math.PI/180F);
                this.bipedRightLeg.rotateAngleX = -angle;
                this.bipedLeftLeg.rotateAngleX = -angle;

            }

        }
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entityIn, float limbSwing, float limbSwingAmount, float partialTickTime) {

        if(entityIn instanceof MusicalSkeleton) {

            MusicalSkeleton skeleton = (MusicalSkeleton) entityIn;

            boolean doLeft = skeleton.leftTicksFromHit <= MusicalSkeleton.ticksToHit;
            boolean doRight = skeleton.rightTicksFromHit <= MusicalSkeleton.ticksToHit;

            float rad = (float) Math.toRadians(60F); //Total Y angle that can be covered.

            if(doLeft) {
                float lLerpHit = (skeleton.leftTicksFromHit + partialTickTime) / MusicalSkeleton.ticksToHit;
                float targetL = (skeleton.prevLeftTargetHit + (skeleton.leftTargetHit - skeleton.prevLeftTargetHit) * lLerpHit) - 0.5F;

                skeleton.ry = targetL * rad;
                skeleton.rx = (float) Math.min(Math.toRadians(-90F + 45F * lLerpHit), 0F); //-90: non hit angle, (-90 + 45) = -45: hit angle
            }

            if(doRight) {
                float rLerpHit = (skeleton.rightTicksFromHit + partialTickTime) / MusicalSkeleton.ticksToHit;
                float targetR = (skeleton.prevRightTargetHit + (skeleton.rightTargetHit - skeleton.prevRightTargetHit) * rLerpHit) - 0.5F;

                skeleton.ly = targetR * rad;
                skeleton.lx = (float) Math.min(Math.toRadians(-90F + 45F * rLerpHit), 0F);
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
