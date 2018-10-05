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
import net.minecraft.util.SoundCategory;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class MusicalSkeleton extends EntityMob {
    public MusicalSkeleton(World worldIn) {
        super(worldIn);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();


        if(this.world.isRemote) {
            for (MidiStream.MidiTone tone : Boneophone.SPOOKY.getNotesAt(this.ticksExisted, 66)) {
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
