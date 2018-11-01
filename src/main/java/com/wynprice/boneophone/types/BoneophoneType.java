package com.wynprice.boneophone.types;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.SoundHandler;
import com.wynprice.boneophone.entity.EntityFieldReference;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.midi.MidiStream;
import com.wynprice.boneophone.network.S0MusicalSkeletonStateUpdate;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.vecmath.Vector2f;

import static net.minecraft.client.renderer.GlStateManager.rotate;
import static net.minecraft.client.renderer.GlStateManager.translate;

public class BoneophoneType extends MusicianType {

    public boolean isKeyboard;
    public boolean isPlaying;

//    public MusicalSkeleton freind;

    public final EntityFieldReference<MusicalSkeleton, BoneophoneType> fieldReference;

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
        this.fieldReference = new EntityFieldReference<MusicalSkeleton, BoneophoneType>(MusicalSkeleton.class, "Friend", e -> {
            if(e == this.entity) {
                return false;
            }
            if(e.musicianType instanceof BoneophoneType) {
                BoneophoneType type = (BoneophoneType) e.musicianType;
//                if(type.fieldReference.getRawReference() != null)
//                System.out.println(type.fieldReference.getRawReference().entity.getPositionVector());
                if(type.fieldReference.getRawReference() == this || type.fieldReference.getRawReference() == null) {
                    return Math.abs(e.posX - entity.posX) <= 80 && Math.abs(e.posZ - entity.posZ) <= 80 && Math.abs(e.posY - entity.posY) <= 30;
                }
            }
            return false;
        },
                e -> (BoneophoneType) e.musicianType) {
            @Override
            public void reset() {
                BoneophoneType.this.isPlaying = false;
                BoneophoneType.this.isKeyboard = false;

                if(this.reference != null) { //Should always be true
                    this.reference.isPlaying = false;
                    this.reference.isKeyboard = false;
                }
                super.reset();
            }
        };
    }

    @Override
    public void setChannel(int channel) {
        BoneophoneType type = this.fieldReference.get(this.entity.world);
        if(type != null) {
            type.setChannel(channel);
        }
        super.setChannel(channel);
    }

    @Override
    public void onTick() {
        BoneophoneType type = this.fieldReference.get(this.entity.world);

        if(type != null && type.fieldReference.getRawReference() != this) {
            type.fieldReference.reset();
        }
        if(type == null) {
            this.isPlaying = false;
            this.isKeyboard = false;
        } else if(!this.entity.world.isRemote && this.entity.ticksSinceCreation % 20 == 0) {
            SkeletalBand.NETWORK.sendToAll(new S0MusicalSkeletonStateUpdate(this.entity.getEntityId(), this.entity.getEntityId(), this.isPlaying, this.isKeyboard));
        }

        this.rightTicksFromHit++;
        this.leftTicksFromHit++;

        if(this.isPlaying) {
            if(type != null) {
                this.entity.setPosition(type.entity.posX + 0.75 * Math.sin(Math.toRadians(type.keyboardRotationYaw + 90F)), type.entity.posY, type.entity.posZ + 0.75 * Math.cos(Math.toRadians(type.keyboardRotationYaw + 90F)));
                this.entity.rotationYaw = this.entity.rotationYawHead = this.entity.prevRotationYawHead = -type.keyboardRotationYaw + 90F;
                this.entity.rotationPitch = 30;
            }
        }

        if(this.entity.world.isRemote && this.isKeyboard) {
            this.entity.rotationPitch = 0;
            this.entity.rotationYaw = 0;
            this.entity.rotationYawHead = 0;
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
            this.playRawSound(SoundHandler.BONEOPHONE_OCTAVES[tone.getOctave()], this.entity.getVolume() * 5f, (float) Math.pow(2.0D, (tone.getKey() / 12.0D)));
        }
    }

    @Override
    public void lookAtConductor() {
        ConductorType conductor = this.getConductor();
        if(this.isKeyboard && conductor != null) {
            double xDist = conductor.entity.posX - this.entity.posX;
            double zDist =  conductor.entity.posZ - this.entity.posZ;
            this.keyboardRotationYaw = (float)(-Math.toDegrees(MathHelper.atan2(zDist, xDist))) + 180F;
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
    public Vector2f getSize() {
        if(this.isKeyboard) {
            return new Vector2f(1.0F, 0.3F);
        } else if(this.isPlaying) {
            return new Vector2f(0.6F, 1.35F);
        } else {
            return new Vector2f(0.6F, 1.99F);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean("Keyboard", this.isKeyboard);
        nbt.setBoolean("Playing", this.isPlaying);
        nbt.setFloat("KeyboardRotation", this.keyboardRotationYaw);
        return this.fieldReference.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.isKeyboard = nbt.getBoolean("Keyboard");
        this.isPlaying = nbt.getBoolean("Playing");
        this.keyboardRotationYaw = nbt.getFloat("KeyboardRotation");
        this.fieldReference.readFromNBT(nbt);
    }

    @Override
    public void writeToBuf(ByteBuf buf) {
        buf.writeBoolean(this.isKeyboard);
        buf.writeBoolean(this.isPlaying);
        buf.writeFloat(this.keyboardRotationYaw);
        this.fieldReference.writeToByteBuf(buf);
    }

    @Override
    public void readFromBuf(ByteBuf buf) {
        this.isKeyboard = buf.readBoolean();
        this.isPlaying = buf.readBoolean();
        this.keyboardRotationYaw = buf.readFloat();
        this.fieldReference.readFromByteBuf(buf);
    }


    @Override
    public ItemStack getHeldItem(EnumHand hand) {
        return this.isPlaying ? new ItemStack(Items.BONE): super.getHeldItem(hand);
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        BoneophoneType type = this.fieldReference.get(this.entity.world);
        if(this.isKeyboard && type != null) {
            return type.processInteract(player, hand);
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
        //During the startup of the world, weird things can happen and the client can get out of sync with the server
        if(this.entity.ticksSinceCreation <= 10) {
            return false;
        }
        BoneophoneType type = this.fieldReference.get(this.entity.world);
        if(type != null && this.entity.getPositionVector().distanceTo(type.entity.getPositionVector()) >= 40) {
            this.fieldReference.reset();
            return false;
        }
        return type != null;
    }

    @Override
    public void updateAITask() {
        BoneophoneType type = this.fieldReference.get(this.entity.world);
        if(type == null) {
            return;
        }
        if(this.entity.getDistanceSq(type.entity) < 4.0D) {
            if(type.isPlaying == this.isPlaying || type.isKeyboard == this.isKeyboard) {
                SkeletalBand.NETWORK.sendToAll(new S0MusicalSkeletonStateUpdate(this.entity.getEntityId(), type.entity.getEntityId(), true, false));
                SkeletalBand.NETWORK.sendToAll(new S0MusicalSkeletonStateUpdate(type.entity.getEntityId(), this.entity.getEntityId(), false, true));

                this.isPlaying = true;
                this.isKeyboard = false;
                type.isPlaying = false;
                type.isKeyboard = true;

                this.entity.getNavigator().clearPath();
                type.entity.getNavigator().clearPath();
            }
        } else {
            this.entity.getLookHelper().setLookPositionWithEntity(type.entity, 10.0F, (float)this.entity.getVerticalFaceSpeed());
            this.entity.getNavigator().tryMoveToEntityLiving(type.entity, 0.5F);
        }

    }

    @Override
    public boolean shouldAIContinueExecuting() {
        BoneophoneType type = this.fieldReference.get(this.entity.world);
        return type != null && this.entity.getDistanceSq(type.entity) >= 4.0D;
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
