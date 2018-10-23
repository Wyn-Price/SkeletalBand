package com.wynprice.boneophone.types;

import com.google.common.collect.Maps;
import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.gui.GuiSelectMidis;
import com.wynprice.boneophone.midi.MidiStream;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Map;

public class ConductorType extends MusicianType {

    public int playingTicks = 0;
    private MidiStream currentlyPlaying = SkeletalBand.SPOOKY;

    private Map<MusicianType, MidiStream.MidiTrack> assignedMap = Maps.newHashMap();

    public ConductorType(MusicalSkeleton entity, MusicianTypeFactory factoryType) {
        super(entity, factoryType);
    }

    @Override
    public void onTick() {
        super.onTick();
        if(this.entity.world.isRemote && !this.entity.paused) {
            for (MusicianType type : this.assignedMap.keySet()) {
                MidiStream.MidiTrack track = this.currentlyPlaying.getTrackAt(type.entity.getTrackID());
                type.setAnimationsFromTones(track.getNotesAt(this.playingTicks + ticksToHit));
                type.playTones(track.getNotesAt(this.playingTicks));
            }
        }
        this.playingTicks++;
    }

    @Override
    protected void displayMidiGui() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiSelectMidis(this.entity.getEntityId(), () -> this.entity.musicianType.factoryType, this.entity::getChannel));
    }

    @Override
    public void setChannel(int channel) {
        super.setChannel(channel);
        this.entity.musicianType = this.factoryType.createType(this.entity); //When the channel is changed, just reset it
    }

    @Override
    public boolean shouldStopAiTasks() {
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("PlayingTicks", this.playingTicks);
        return super.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.playingTicks = nbt.getInteger("PlayingTicks");
        super.readFromNBT(nbt);
    }

    @Override
    public void writeToBuf(ByteBuf buf) {
        buf.writeInt(this.playingTicks);
        super.writeToBuf(buf);
    }

    @Override
    public void readFromBuf(ByteBuf buf) {
        this.playingTicks = buf.readInt();
        super.readFromBuf(buf);
    }

    public void assign(MusicianType type, MidiStream.MidiTrack track) {
        this.assignedMap.put(type, track);
        this.checkMap();
    }

    public int getAmountPlaying(MidiStream.MidiTrack track) {
        int out = 0;
        this.checkMap();

        for (MidiStream.MidiTrack assignedTrack : this.assignedMap.values()) {
            if(track == assignedTrack) {
                out++;
            }
        }

        return out;
    }

    public boolean isAssigned(MusicianType type) {
        this.checkMap();
        return this.assignedMap.containsKey(type);
    }

    private void checkMap() {
        for (MusicianType type : this.assignedMap.keySet()) {
            if(type.entity.musicianType != type || type.entity.getChannel() != this.entity.getChannel()) {
                this.assignedMap.remove(type);
            }
        }
    }

    public MidiStream getCurrentlyPlaying() {
        return this.currentlyPlaying;
    }

    public void setCurrentlyPlaying(MidiStream currentlyPlaying) {
        this.currentlyPlaying = currentlyPlaying;
        this.assignedMap.clear();
        this.playingTicks = 0;
    }
}
