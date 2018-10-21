package com.wynprice.boneophone.types;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.gui.GuiSelectMidis;
import com.wynprice.boneophone.midi.MidiStream;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class ConductorType extends MusicianType {

    public static int ticksToHit = 3;

    public int playingTicks = 0;
    public MidiStream currentlyPlaying = SkeletalBand.SPOOKY;

    public ConductorType(MusicalSkeleton entity, MusicianTypeFactory factoryType) {
        super(entity, factoryType);
    }

    @Override
    public void onTick() {
        for (Entity entity : this.entity.world.loadedEntityList) {
            if(entity instanceof MusicalSkeleton) {
                MusicalSkeleton skeleton = (MusicalSkeleton) entity;
                if(skeleton.getChannel() == this.entity.getChannel()) {
                    skeleton.musicianType.setAnimationsFromTones(this.currentlyPlaying.getNotesAt(this.playingTicks + ticksToHit));
                    skeleton.musicianType.playTones(this.currentlyPlaying.getNotesAt(this.playingTicks));

                }
            }
        }
        this.playingTicks++;
    }

    @Override
    protected void displayMidiGui() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiSelectMidis(this.entity.getEntityId(), () -> this.entity.musicianType.factoryType, this.entity::getChannel));
    }

    @Override
    public boolean shouldStopAiTasks() {
        return true;
    }
}
