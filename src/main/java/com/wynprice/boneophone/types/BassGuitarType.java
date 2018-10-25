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

public class BassGuitarType extends MusicianType {
    public BassGuitarType(MusicalSkeleton entity, MusicianTypeFactory factoryType) {
        super(entity, factoryType);
    }

    @Override
    public void playTones(MidiStream.MidiTone[] tones) {
        if(this.entity.world.isRemote) {
            for (MidiStream.MidiTone tone : tones) {
                this.playRawSound(SoundHandler.BASS_OCTAVES[tone.getOctave()],  this.entity.getVolume() * (2F * (tone.getRawKey() / 128F) + 0.5F), (float) Math.pow(2.0D, (tone.getKey() / 12.0D)));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void playRawSound(SoundEvent event, float volume, float pitch) {

        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(event, SoundCategory.RECORDS, volume, pitch, this.entity.getPosition()));
    }

    @Override
    public boolean shouldStopAiTasks() {
        return true;
    }

}
