package com.wynprice.boneophone.types;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.gui.GuiSelectMidis;
import com.wynprice.boneophone.midi.MidiFileHandler;
import com.wynprice.boneophone.midi.MidiStream;
import com.wynprice.boneophone.network.C10SkeletonPlayMidi;
import com.wynprice.boneophone.network.S2SyncAndPlayMidi;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Map;

public class ConductorType extends MusicianType {

    public int playingTicks = 0;
    public String currentlyPlayingHash = "";
    private MidiStream currentlyPlaying = SkeletalBand.SPOOKY;

    private Map<MusicianType, MidiStream.MidiTrack> assignedMap = Maps.newHashMap();

    public ConductorType(MusicalSkeleton entity, MusicianTypeFactory factoryType) {
        super(entity, factoryType);
    }

    @Override
    public void onTick() {
        //Don't call super, as it is just unnecessary
        if(this.entity.world.isRemote && !this.currentlyPlaying.hash.equals(this.currentlyPlayingHash) && !this.currentlyPlayingHash.isEmpty()) {
            SkeletalBand.NETWORK.sendToServer(new C10SkeletonPlayMidi(this.entity.getEntityId(), this.currentlyPlayingHash));
            this.currentlyPlayingHash = this.currentlyPlaying.hash;
        }
        if(this.entity.world.isRemote && !this.entity.paused) {
            for (MusicianType type : this.assignedMap.keySet()) {
                if(type.entity.paused) {
                    continue;
                }
                MidiStream.MidiTrack track = this.currentlyPlaying.getTrackAt(type.entity.getTrackID());
                type.setAnimationsFromTones(track.getNotesAt(this.playingTicks + ticksToHit));
                type.playTones(track.getNotesAt(this.playingTicks));
            }
        }
        if(!this.entity.paused) {
            this.playingTicks++;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
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
        nbt.setString("PlayingHash", this.currentlyPlayingHash);
        return super.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.playingTicks = nbt.getInteger("PlayingTicks");
        this.currentlyPlayingHash = nbt.getString("PlayingHash");
        super.readFromNBT(nbt);
    }

    @Override
    public void writeToBuf(ByteBuf buf) {
        buf.writeInt(this.playingTicks);
        ByteBufUtils.writeUTF8String(buf, this.currentlyPlayingHash);
        super.writeToBuf(buf);
    }

    @Override
    public void readFromBuf(ByteBuf buf) {
        this.playingTicks = buf.readInt();
        this.currentlyPlayingHash = ByteBufUtils.readUTF8String(buf);
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
        List<MusicianType> removed = Lists.newArrayList();
        for (MusicianType type : this.assignedMap.keySet()) {
            if(type.entity.musicianType != type || type.entity.getChannel() != this.entity.getChannel() || !this.currentlyPlaying.getTracks().contains(this.assignedMap.get(type))) {
                removed.add(type);
            }
        }
        removed.forEach(this.assignedMap::remove);
    }

    public MidiStream getCurrentlyPlaying() {
        return this.currentlyPlaying;
    }

    public void setCurrentlyPlayingRaw(MidiStream stream) {
        this.currentlyPlaying = stream;
        this.currentlyPlayingHash = stream.hash;
    }

    public void setCurrentlyPlaying(MidiStream currentlyPlaying) {
        this.currentlyPlaying = currentlyPlaying;
        this.currentlyPlayingHash = currentlyPlaying.hash;
        this.assignedMap.clear();
        this.playingTicks = 0;
    }
}
