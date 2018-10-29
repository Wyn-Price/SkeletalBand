package com.wynprice.boneophone.types;

import com.wynprice.boneophone.SoundHandler;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.midi.MidiStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class DrumType extends MusicianType {
    public DrumType(MusicalSkeleton entity, MusicianTypeFactory factoryType) {
        super(entity, factoryType);
    }

    @Override
    public void playTones(MidiStream.MidiTone[] tones) {
        if(this.entity.world.isRemote) {
            for (MidiStream.MidiTone tone : tones) {
                int key = tone.getRawKey() - SoundHandler.DRUM_OFFSET;
                if(key >= 0 && key < SoundHandler.DRUM_NOTES.length) {
                    this.playRawSound(SoundHandler.DRUM_NOTES[key],  this.entity.getVolume());
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void playRawSound(SoundEvent event, float volume) {
        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(event, SoundCategory.RECORDS, volume, 1F, this.entity.getPosition()));
    }

    @Override
    public boolean shouldStopAiTasks() {
        return true;
    }

}
