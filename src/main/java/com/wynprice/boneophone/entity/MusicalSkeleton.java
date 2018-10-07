package com.wynprice.boneophone.entity;

import com.wynprice.boneophone.Boneophone;
import com.wynprice.boneophone.midi.MidiStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class MusicalSkeleton extends EntityMob {

    public static int ticksToHit = 3;

    public float rightTargetHit = -5;
    public float leftTargetHit = -5;


    public float prevRightTargetHit = -5;
    public float prevLeftTargetHit = -5;

    public int rightTicksFromHit;
    public int leftTicksFromHit;

    public MusicalSkeleton(World worldIn) {
        super(worldIn);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        this.rightTicksFromHit++;
        this.leftTicksFromHit++;

        if(this.world.isRemote) {
            boolean usedLeft = false;
            boolean usedRight = false;

            float rightNote = this.rightTargetHit;
            float leftNote = this.leftTargetHit;

            boolean usedSecondLeft = false;
            boolean usedSecondRight = false;

            float secondRightNote = -1F;
            float secondLeftNote = -1F;

            for (MidiStream.MidiTone tone : Boneophone.SPOOKY.getNotesAt(this.ticksExisted + ticksToHit)) {
                if(!usedLeft || !usedRight || !usedSecondLeft || !usedSecondRight) {
                    if(tone.getPosition() >= 0.5F) {
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


            for (MidiStream.MidiTone tone : Boneophone.SPOOKY.getNotesAt(this.ticksExisted)) {
                float f = (float)Math.pow(2.0D, (tone.getKey() / 12.0D));
                Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(tone.getEvent(), SoundCategory.RECORDS, 1F, f, this.getPosition()));
            }
        }
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
        IEntityLivingData data =  super.onInitialSpawn(difficulty, livingdata);
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.BONE));
        this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.BONE));

        return data;
    }
}
