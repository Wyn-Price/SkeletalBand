package com.wynprice.boneophone.entity;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.gui.GuiSelectMidis;
import com.wynprice.boneophone.midi.MidiStream;
import com.wynprice.boneophone.network.S0MusicalSkeletonStateUpdate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class MusicalSkeleton extends EntityCreature {

    public static int ticksToHit = 3;

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

    public MidiStream currentlyPlaying = SkeletalBand.SPOOKY;
    public int playingTicks = 0;

    public MusicalSkeleton(World worldIn) {
        super(worldIn);
    }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();

        this.tasks.addTask(0, new AiFindFreind(0.5D));

        this.tasks.addTask(1, new EntityAISwimming(this));

        this.tasks.addTask(5, new MusicalSkeleton.AiWander(0.2D));
        this.tasks.addTask(6, new MusicalSkeleton.AIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(6, new MusicalSkeleton.AILookIdle(this));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        this.rightTicksFromHit++;
        this.leftTicksFromHit++;

        this.playingTicks++;

        if(this.world.isRemote && this.isPlaying) {
            boolean usedLeft = false;
            boolean usedRight = false;

            float rightNote = this.rightTargetHit;
            float leftNote = this.leftTargetHit;

            boolean usedSecondLeft = false;
            boolean usedSecondRight = false;

            float secondRightNote = -1F;
            float secondLeftNote = -1F;

            for (MidiStream.MidiTone tone : this.currentlyPlaying.getNotesAt(this.playingTicks + ticksToHit)) {
                if(!usedLeft || !usedRight || !usedSecondLeft || !usedSecondRight) {
                    if(tone.getPosition() < 0.5F) { //I would have thought it to be >= 0.5, however in practice this seems not to be the case todo: investigate that
                        if(!usedRight) {
                            rightNote = tone.getPosition();
                            usedRight = true;
                        } else if(!usedSecondRight) {
                            secondRightNote = tone.getPosition();
                            usedSecondRight = true;
                        }
                    } else {
                        if(!usedLeft) {
                            leftNote = tone.getPosition();
                            usedLeft = true;
                        } else if(!usedSecondLeft) {
                            secondLeftNote = tone.getPosition();
                            usedSecondLeft = true;
                        }
                    }
                }
            }


            //If one hand isn't in use, but has more than one note, have the other hand fill in
            if(!usedRight && usedSecondLeft) {
                usedRight = true;
                rightNote = secondLeftNote;
            }

            if(!usedLeft && usedSecondRight) {
                usedLeft = true;
                leftNote = secondRightNote;
            }

            if(usedRight) {
                this.prevRightTargetHit = this.rightTargetHit;
                this.rightTicksFromHit = 0;
                this.rightTargetHit = rightNote;
            }
            if(usedLeft) {
                this.prevLeftTargetHit = this.leftTargetHit;
                this.leftTicksFromHit = 0;
                this.leftTargetHit = leftNote;
            }


            for (MidiStream.MidiTone tone : this.currentlyPlaying.getNotesAt(this.playingTicks)) {
                float f = (float)Math.pow(2.0D, (tone.getKey() / 12.0D));
                Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(tone.getEvent(), SoundCategory.RECORDS, 2F, f, this.getPosition()));
            }


            if(this.freind != null) {
                this.setPosition(this.freind.posX + 0.75, this.freind.posY, this.freind.posZ);

                this.rotationYaw = 90;
                this.rotationYawHead = 90;
                this.prevRotationYawHead = 90;
                this.rotationPitch = 0;

                this.getLookHelper().setLookPositionWithEntity(this.freind, this.getHorizontalFaceSpeed(), this.getVerticalFaceSpeed());
            }

        } else if(this.world.isRemote && this.isKeyboard) {
            this.rotationPitch = 0;
            this.rotationYaw = 0;
            this.rotationYawHead = 0;
        }

        if(this.freind != null && this.freind.freind != this) {
            this.isPlaying = false;
            this.isKeyboard = false;
            this.freind = null;
        }
    }

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        if(this.isPlaying && this.world.isRemote) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiSelectMidis(this.getEntityId()));
            return true;
        } else if(this.isKeyboard) {
            return this.freind.processInteract(player, hand);
        }
        return false;
    }

    @Override
    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn) {
        return this.isKeyboard && !this.isPlaying ? ItemStack.EMPTY : super.getItemStackFromSlot(slotIn);
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
        IEntityLivingData data =  super.onInitialSpawn(difficulty, livingdata);
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.BONE));
        this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.BONE));

        return data;
    }

    private class AiWander extends EntityAIWanderAvoidWater {

        public AiWander(double speedIn) {
            super(MusicalSkeleton.this, speedIn);
        }

        @Override
        public boolean shouldExecute() {
            return !MusicalSkeleton.this.isPlaying && !MusicalSkeleton.this.isKeyboard && super.shouldExecute();
        }
    }

    private class AIWatchClosest extends EntityAIWatchClosest {

        public AIWatchClosest(EntityLiving entityIn, Class<? extends Entity> watchTargetClass, float maxDistance) {
            super(entityIn, watchTargetClass, maxDistance);
        }

        @Override
        public boolean shouldExecute() {
            return !MusicalSkeleton.this.isPlaying && !MusicalSkeleton.this.isKeyboard && super.shouldExecute();
        }
    }

    private class AILookIdle extends EntityAILookIdle {

        public AILookIdle(EntityLiving entitylivingIn) {
            super(entitylivingIn);
        }

        @Override
        public boolean shouldExecute() {
            return !MusicalSkeleton.this.isPlaying && !MusicalSkeleton.this.isKeyboard && super.shouldExecute();
        }

    }
    private class AiFindFreind extends EntityAIBase {

        private final double moveSpeed;

        private final MusicalSkeleton skeleton = MusicalSkeleton.this;

        private boolean playing;

        private AiFindFreind(double moveSpeed) {
            this.moveSpeed = moveSpeed;
        }

        @Override
        public boolean shouldExecute() {
            if(this.skeleton.freind != null && this.skeleton.freind.freind == this.skeleton) {
                return false;
            }
            if(this.skeleton.freind != null && (this.skeleton.freind.isDead || this.skeleton.getPositionVector().distanceTo(this.skeleton.freind.getPositionVector()) >= 40 || this.skeleton.freind.freind == this.skeleton)) {
                this.skeleton.freind = null;
            }
            if(this.skeleton.freind == null) {
                for (MusicalSkeleton skeleton : this.skeleton.world.getEntitiesWithinAABB(MusicalSkeleton.class, new AxisAlignedBB(-40, -20, -40, 40, 20, 40).offset(this.skeleton.getPositionVector()), e -> e != this.skeleton)) {
                    if(skeleton.freind == null && !skeleton.isPlaying && !skeleton.isKeyboard) {
                        this.skeleton.freind = skeleton;
                        skeleton.freind = this.skeleton;
                    }
                }
            }
            return this.skeleton.freind != null;
        }

        @Override
        public boolean isInterruptible() {
            return !this.playing;
        }

        @Override
        public void updateTask() {
            this.skeleton.getLookHelper().setLookPositionWithEntity(this.skeleton.freind, 10.0F, (float)this.skeleton.getVerticalFaceSpeed());
            this.skeleton.getNavigator().tryMoveToEntityLiving(this.skeleton.freind, this.moveSpeed);

            if(this.skeleton.getDistanceSq(this.skeleton.freind) < 4.0D && !this.skeleton.freind.isPlaying && !this.skeleton.freind.isKeyboard) {
                this.playing = true;

                SkeletalBand.NETWORK.sendToAll(new S0MusicalSkeletonStateUpdate(this.skeleton.getEntityId(), this.skeleton.freind.getEntityId(), true, false));
                SkeletalBand.NETWORK.sendToAll(new S0MusicalSkeletonStateUpdate(this.skeleton.freind.getEntityId(), this.skeleton.getEntityId(), false, true));

                this.skeleton.isPlaying = true;
                this.skeleton.freind.isKeyboard = true;

                this.skeleton.getNavigator().clearPath();

            }
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.skeleton.freind != null && this.skeleton.freind.isEntityAlive() && this.skeleton.freind.freind == this.skeleton;
        }

        @Override
        public void resetTask() {
            this.playing = false;
        }
    }
}
