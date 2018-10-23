package com.wynprice.boneophone.types;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.SoundHandler;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.midi.MidiStream;
import com.wynprice.boneophone.network.S0MusicalSkeletonStateUpdate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec2f;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.minecraft.client.renderer.GlStateManager.rotate;
import static net.minecraft.client.renderer.GlStateManager.translate;

public class BoneophoneType extends MusicianType {

    public boolean isKeyboard;
    public boolean isPlaying;

    public MusicalSkeleton freind;

    public float rightTargetHit = -5;
    public float leftTargetHit = -5;

    public float prevRightTargetHit = -5;
    public float prevLeftTargetHit = -5;

    public int rightTicksFromHit;
    public int leftTicksFromHit;

    public float rx, ry, lx, ly;

    public float keyboardRotationYaw;

    public BoneophoneType(MusicalSkeleton entity, MusicianTypeFactory factoryType) {
        super(entity, factoryType);
    }

    @Override
    public void setChannel(int channel) {
        if(this.freind != null) {
            this.freind.setChannel(channel);
        }
        super.setChannel(channel);
    }

    @Override
    public void onTick() {
        BoneophoneType freindType = null;
        if(this.freind != null) {
            MusicianType type = this.freind.musicianType;
            if(!(type instanceof BoneophoneType)) {
                this.entity.paused = false;
                this.isKeyboard = false;
                this.isPlaying = false;
                this.freind = null;
                return;
            }
            freindType = (BoneophoneType) type;
        }


        this.rightTicksFromHit++;
        this.leftTicksFromHit++;

        if(this.isPlaying) {
            if(this.freind != null && freindType != null) {
                this.entity.setPosition(this.freind.posX + 0.75 * Math.sin(Math.toRadians(freindType.keyboardRotationYaw + 90F)), this.freind.posY, this.freind.posZ + 0.75 * Math.cos(Math.toRadians(freindType.keyboardRotationYaw + 90F)));
                this.entity.rotationYaw = this.entity.rotationYawHead = this.entity.prevRotationYawHead = -freindType.keyboardRotationYaw + 90F;
                this.entity.rotationPitch = 30;


            }
        }

        if(this.entity.world.isRemote && this.isKeyboard) {
            this.entity.rotationPitch = 0;
            this.entity.rotationYaw = 0;
            this.entity.rotationYawHead = 0;
        }

        if(this.freind != null && freindType != null && (freindType.freind != this.entity || this.freind.isDead)) {
            this.isPlaying = false;
            this.isKeyboard = false;
            this.freind = null;
        }

        if((this.freind == null && (!this.isKeyboard && !this.isPlaying)) || this.freind == this.entity || (this.freind != null && !(this.freind.musicianType instanceof BoneophoneType))) {
            if(this.freind != null && freindType != null) {
                freindType.freind = null;
                freindType.isKeyboard = false;
                freindType.isPlaying = false;
                this.freind.paused = false;
            }
            this.entity.paused = false;
            this.isKeyboard = false;
            this.isPlaying = false;
            this.freind = null;
        }
        super.onTick();
    }

    @Override
    protected void checkAssignment() {
        if(this.isPlaying) {
            super.checkAssignment();
        }
    }

    @Override
    public void playTones(MidiStream.MidiTone[] tones) {
        if(!this.entity.world.isRemote || !this.isPlaying) {
            return;
        }
        for (MidiStream.MidiTone tone : tones) {
            this.playRawSound(SoundHandler.BONEOPHONE_OCTAVES[tone.getOctave()], 2F * (tone.getRawKey() / 128F) + 0.5F, (float) Math.pow(2.0D, (tone.getKey() / 12.0D)));
        }
    }

    @Override
    public void setAnimationsFromTones(MidiStream.MidiTone[] tones) {
        if(!this.entity.world.isRemote || !this.isPlaying) {
            return;
        }
        boolean usedLeft = false;
        boolean usedRight = false;

        float rightNote = this.rightTargetHit;
        float leftNote = this.leftTargetHit;

        boolean usedSecondLeft = false;
        boolean usedSecondRight = false;

        float secondRightNote = -1F;
        float secondLeftNote = -1F;

        for (MidiStream.MidiTone tone : tones) {
            if (!usedLeft || !usedRight || !usedSecondLeft || !usedSecondRight) {
                if (tone.getPosition() < 0.5F) { //I would have thought it to be >= 0.5, however in practice this seems not to be the case todo: investigate that
                    if (!usedRight) {
                        rightNote = tone.getPosition();
                        usedRight = true;
                    } else if (!usedSecondRight) {
                        secondRightNote = tone.getPosition();
                        usedSecondRight = true;
                    }
                } else {
                    if (!usedLeft) {
                        leftNote = tone.getPosition();
                        usedLeft = true;
                    } else if (!usedSecondLeft) {
                        secondLeftNote = tone.getPosition();
                        usedSecondLeft = true;
                    }
                }
            }
        }


        //If one hand isn't in use, but has more than one note, have the other hand fill in
        if (!usedRight && usedSecondLeft) {
            if(secondLeftNote > leftNote) {
                float ref = secondLeftNote;
                secondLeftNote = leftNote;
                leftNote = ref;
            }
            usedRight = true;
            rightNote = secondLeftNote;
        }

        if (!usedLeft && usedSecondRight) {
            if(secondRightNote > leftNote) {
                float ref = secondRightNote;
                secondRightNote = rightNote;
                rightNote = ref;
            }
            usedLeft = true;
            leftNote = secondRightNote;
        }

        if (usedRight) {
            this.prevRightTargetHit = this.rightTargetHit;
            this.rightTicksFromHit = 0;
            this.rightTargetHit = rightNote;
        }
        if (usedLeft) {
            this.prevLeftTargetHit = this.leftTargetHit;
            this.leftTicksFromHit = 0;
            this.leftTargetHit = leftNote;
        }

    }

    @Override
    public Vec2f getSize() {
        if(this.isKeyboard) {
            return new Vec2f(1.0F, 0.3F);
        } else if(this.isPlaying) {
            return new Vec2f(0.6F, 1.35F);
        } else {
            return new Vec2f(0.6F, 1.99F);
        }
    }

    @Override
    public ItemStack getHeldItem(EnumHand hand) {
        return this.isKeyboard ? ItemStack.EMPTY : super.getHeldItem(hand);
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if(this.isKeyboard && this.freind != null && this.freind.musicianType != null) {
            return this.freind.musicianType.processInteract(player, hand);
        }
        return super.processInteract(player, hand);
    }

    @SideOnly(Side.CLIENT)
    private void playRawSound(SoundEvent event, float volume, float pitch) {
        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(event, SoundCategory.RECORDS, volume, pitch, this.entity.getPosition()));
    }

    @Override
    public boolean shouldStopAiTasks() {
        return this.isKeyboard || this.isPlaying;
    }

    @Override
    public boolean shouldAIExecute() {
        BoneophoneType freindType = null;
        if(this.freind != null) {
            MusicianType type = this.freind.musicianType;
            if(type instanceof BoneophoneType) {
                freindType = (BoneophoneType) type;
            }
        }

        if(this.freind != null && freindType != null && freindType.freind == this.entity) {
            return false;
        }
        if(freindType != null && freindType.freind != null && (this.freind.isDead || this.entity.getPositionVector().distanceTo(this.freind.getPositionVector()) >= 40 || freindType.freind == this.entity)) {
            this.freind = null;
        }
        if(this.freind == null) {
            for (MusicalSkeleton skeleton : this.entity.world.getEntitiesWithinAABB(MusicalSkeleton.class, new AxisAlignedBB(-40, -20, -40, 40, 20, 40).offset(this.entity.getPositionVector()), e -> e != this.entity)) {
                MusicianType type = skeleton.musicianType;
                if(type instanceof BoneophoneType) {
                    BoneophoneType boneophoneType = (BoneophoneType) type;
                    if(boneophoneType.freind == null && !boneophoneType.isPlaying && !boneophoneType.isKeyboard) {
                        this.freind = skeleton;
                        boneophoneType.freind = this.entity;
                    }
                }


            }
        }
        return this.freind != null;
    }

    @Override
    public void updateAITask() {

        BoneophoneType boneophoneType = null;
        if(this.freind == null) {
            for (MusicalSkeleton skeleton : this.entity.world.getEntitiesWithinAABB(MusicalSkeleton.class, new AxisAlignedBB(-40, -20, -40, 40, 20, 40).offset(this.entity.getPositionVector()), e -> e != this.entity)) {
                if(skeleton == this.entity) {
                    continue;
                }
                MusicianType type = skeleton.musicianType;
                if(type instanceof BoneophoneType) {
                    boneophoneType = (BoneophoneType) type;
                    if(boneophoneType.freind == null && !boneophoneType.isPlaying && !boneophoneType.isKeyboard) {
                        this.freind = skeleton;
                        boneophoneType.freind = this.entity;
                    }
                }


            }
        }
        if(boneophoneType == null && this.freind != null) {
            if(this.freind != null) {
                MusicianType type = this.freind.musicianType;
                if(type instanceof BoneophoneType) {
                    boneophoneType = (BoneophoneType) type;
                }
            }

        }
        if(this.freind == null || boneophoneType == null) {
            return;
        }
        if(this.entity.getDistanceSq(this.freind) < 4.0D) {
            if(!boneophoneType.isPlaying && !boneophoneType.isKeyboard) {
                SkeletalBand.NETWORK.sendToAll(new S0MusicalSkeletonStateUpdate(this.entity.getEntityId(), this.freind.getEntityId(), true, false));
                SkeletalBand.NETWORK.sendToAll(new S0MusicalSkeletonStateUpdate(this.freind.getEntityId(), this.entity.getEntityId(), false, true));

                this.isPlaying = true;
                boneophoneType.isKeyboard = true;

                this.entity.getNavigator().clearPath();
                this.freind.getNavigator().clearPath();

            }

        } else {
            this.entity.getLookHelper().setLookPositionWithEntity(this.freind, 10.0F, (float)this.entity.getVerticalFaceSpeed());
            this.entity.getNavigator().tryMoveToEntityLiving(this.freind, 0.5F);
        }

    }

    @Override
    public boolean shouldAIContinueExecuting() {
        BoneophoneType freindType = null;
        if(this.freind != null) {
            MusicianType type = this.freind.musicianType;
            if(type instanceof BoneophoneType) {
                freindType = (BoneophoneType) type;
            }
        }

        return this.freind == null || freindType == null || this.freind.isDead || freindType.entity != this.entity;
    }

    @Override
    public void setEntityTranslations() {
        if(this.isPlaying) {
            translate(0, -0.7F, 0);
        } else if(this.isKeyboard) {
            rotate(this.keyboardRotationYaw, 0, 1, 0);
            rotate(90, 1, 0, 0);
            translate(0, -entity.height / 2 - entity.width , -0.3);
        }
        super.setEntityTranslations();
    }

    @Override
    public void setEntityAnimations(float partialTicks, ModelBase model) {
        ModelBiped m = (ModelBiped) model;
        if(this.isPlaying) {

            boolean doLeft = this.leftTicksFromHit <= ticksToHit;
            boolean doRight = this.rightTicksFromHit <= ticksToHit;

            float rad = (float) Math.toRadians(60F); //Total Y angle that can be covered.

            if(doLeft) {
                float lLerpHit = (this.leftTicksFromHit + partialTicks) / ticksToHit;
                float targetL = (this.prevLeftTargetHit + (this.leftTargetHit - this.prevLeftTargetHit) * lLerpHit) - 0.65F;

                this.ry = targetL * rad;
                this.rx = (float) Math.min(Math.toRadians(-90F + 45F * lLerpHit), 0F); //-90: non hit angle, (-90 + 45) = -45: hit angle
            }

            if(doRight) {
                float rLerpHit = (this.rightTicksFromHit + partialTicks) / ticksToHit;
                float targetR = (this.prevRightTargetHit + (this.rightTargetHit - this.prevRightTargetHit) * rLerpHit) - 0.35F;

                this.ly = targetR * rad;
                this.lx = (float) Math.min(Math.toRadians(-90F + 45F * rLerpHit), 0F);
            }


            m.bipedLeftArm.rotateAngleX = this.lx;
            m.bipedLeftArm.rotateAngleY = this.ly;

            m.bipedRightArm.rotateAngleX = this.rx;
            m.bipedRightArm.rotateAngleY = this.ry;


            float angle = (float) (90D * Math.PI/180F);
            m.bipedRightLeg.rotateAngleX = -angle;
            m.bipedLeftLeg.rotateAngleX = -angle;


        } else if(this.isKeyboard) {
            m.bipedRightLeg.rotateAngleX = m.bipedRightLeg.rotateAngleY = m.bipedRightLeg.rotateAngleZ =
                    m.bipedRightArm.rotateAngleX = m.bipedRightArm.rotateAngleY = m.bipedRightArm.rotateAngleZ =
                            m.bipedLeftLeg.rotateAngleX = m.bipedLeftLeg.rotateAngleY = m.bipedLeftLeg.rotateAngleZ =
                                    m.bipedLeftArm.rotateAngleX = m.bipedLeftArm.rotateAngleY = m.bipedLeftArm.rotateAngleZ =
                                            0;
        }
    }
}
