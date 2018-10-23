package com.wynprice.boneophone.types;

import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.gui.GuiMusician;
import com.wynprice.boneophone.midi.MidiStream;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec2f;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.UUID;

public class MusicianType {

    protected static final int ticksToHit = 3;

    protected final MusicalSkeleton entity;

    public final MusicianTypeFactory factoryType;

    @Nullable
    private ConductorType conductor;
    @Nullable
    private UUID conductorUUID;

    public MusicianType(MusicalSkeleton entity, MusicianTypeFactory factoryType) {
        this.entity = entity;
        this.factoryType = factoryType;
    }


    public void onTick() {
        this.checkAssignment();
    }

    protected void checkAssignment() {
        ConductorType conductor = this.getConductor();
        if(conductor != null && !conductor.isAssigned(this)) {
            conductor.assign(this, conductor.getCurrentlyPlaying().getTrackAt(this.entity.getTrackID()));
        }

    }

    public void setAnimationsFromTones(MidiStream.MidiTone[] tones) {
    }

    public void playTones(MidiStream.MidiTone[] tones) {
    }

    public void setEntityAnimations(float partialTicks, ModelBase model) {
    }

    public void setEntityTranslations() {
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        if(this.conductorUUID != null) {
            nbt.setBoolean("HasConductorUUID", true);
            nbt.setUniqueId("ConductorUUID", this.conductorUUID);
        }
        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        if(nbt.getBoolean("HasConductorUUID")) {
            this.conductorUUID = nbt.getUniqueId("ConductorUUID");
        }
    }

    public void writeToBuf(ByteBuf buf) {
        buf.writeBoolean(this.conductorUUID != null);
        if(this.conductorUUID != null) {
            ByteBufUtils.writeUTF8String(buf, this.conductorUUID.toString());
        }
    }

    public void readFromBuf(ByteBuf buf) {
        if(buf.readBoolean()) {
            this.conductorUUID = UUID.fromString(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Nullable
    public ConductorType getConductor() {
        if(this instanceof ConductorType) {
            this.conductorUUID = null;
            return this.conductor = null;
        }
        if(this.conductor == null && this.conductorUUID != null) {
            for (Entity entity : this.entity.world.loadedEntityList) {
                if(entity instanceof MusicalSkeleton && ((MusicalSkeleton) entity).musicianType instanceof ConductorType && this.conductorUUID.equals(entity.getUniqueID()) && ((MusicalSkeleton) entity).getChannel() == this.entity.getChannel()) {
                    this.conductor = (ConductorType) ((MusicalSkeleton) entity).musicianType;
                    this.conductor.assign(this, this.conductor.getCurrentlyPlaying().getTrackAt(this.entity.getTrackID()));
                    return this.conductor;
                }
            }
            this.conductorUUID = null;
        }
        if(this.conductor == null) { //At this point, conductorUUID will always be null
            for (Entity e : this.entity.world.loadedEntityList) {
                if(e instanceof MusicalSkeleton && ((MusicalSkeleton) e).musicianType instanceof ConductorType && ((MusicalSkeleton) e).getChannel() == this.entity.getChannel()) {
                    this.setConductor((MusicalSkeleton) e);
                }
            }
        }
        if(this.conductor != null && this.conductor.entity.musicianType != this.conductor) {
            this.conductor = null;
            this.conductorUUID = null;
        }
        return this.conductor;
    }

    public void setConductor(MusicalSkeleton conductor) {
        if(conductor.musicianType instanceof ConductorType) {
            this.conductor = (ConductorType) conductor.musicianType;
            this.conductorUUID = conductor.getUniqueID();
        }
    }

    public void setChannel(int channel) {
        this.entity.setChannel(channel);
    }

    public void setTrack(int track) {
        this.entity.setTrackID(track);
        if(this.conductor != null) {
            this.conductor.assign(this, this.conductor.getCurrentlyPlaying().getTrackAt(track));
        }
    }

    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if(this.entity.world.isRemote) {
            if(player.isSneaking()) {
                this.entity.paused = !this.entity.paused;
            } else {
                this.displayMidiGui();
            }
        }
        player.swingArm(hand);
        return true;
    }

    @SideOnly(Side.CLIENT)
    protected void displayMidiGui() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiMusician(this.entity.getEntityId(), () -> this.entity.musicianType.factoryType, this.getConductor(), this.entity::getChannel, this.entity::getTrackID));
    }

    public Vec2f getSize() {
        return new Vec2f(0.6F, 1.99F);
    }

    public ItemStack getHeldItem(EnumHand hand) {
        return this.entity.getHeldItem(hand);
    }

    public boolean shouldStopAiTasks() {
        return false;
    }

    public boolean shouldAIExecute() {
        return false;
    }

    public void updateAITask() {
    }

    public boolean shouldAIContinueExecuting() {
        return this.shouldAIExecute();
    }

}
