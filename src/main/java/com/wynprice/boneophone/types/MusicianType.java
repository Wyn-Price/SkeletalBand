package com.wynprice.boneophone.types;

import com.wynprice.boneophone.entity.EntityFieldReference;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MusicianType {

    protected static final int ticksToHit = 3;

    protected final MusicalSkeleton entity;

    public final MusicianTypeFactory factoryType;

    private final EntityFieldReference<MusicalSkeleton, ConductorType> conductorRef;

    public MusicianType(MusicalSkeleton entity, MusicianTypeFactory factoryType) {
        this.entity = entity;
        this.factoryType = factoryType;

        this.conductorRef = new EntityFieldReference<MusicalSkeleton, ConductorType>(MusicalSkeleton.class, "Conductor", s -> s != this.entity && s.musicianType instanceof ConductorType && s.getChannel() == MusicianType.this.entity.getChannel(), s -> (ConductorType) s.musicianType) {
            @Override
            public void setReferenceFromEntity(@Nonnull MusicalSkeleton entity) {
                if(this.entityPredicate.test(entity)) {
                    ConductorType conductor = ((ConductorType)entity.musicianType);
                    conductor.assign(MusicianType.this, conductor.getCurrentlyPlaying().getTrackAt(MusicianType.this.entity.getTrackID()));
                }
                super.setReferenceFromEntity(entity);
            }
        };
    }


    public void onTick() {
        this.checkAssignment();
        this.lookAtConductor();
    }

    public MusicalSkeleton getEntity() {
        return entity;
    }

    protected void checkAssignment() {
        ConductorType conductor = this.getConductor();
        if(conductor != null && !conductor.isAssigned(this)) {
            conductor.assign(this, conductor.getCurrentlyPlaying().getTrackAt(this.entity.getTrackID()));
        }

    }

    public void setAnimationsFromTones(MidiStream.MidiTone[] tones) {
    }

    public void lookAtConductor() {
        ConductorType conductor = this.getConductor();
        if(conductor != null) {
            this.entity.getLookHelper().setLookPositionWithEntity(conductor.entity, 15F, 15F);
        }
    }

    public void playTones(MidiStream.MidiTone[] tones) {
    }

    public void setEntityAnimations(float partialTicks, ModelBase model) {
    }

    public void setEntityTranslations() {
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        return this.conductorRef.writeToNBT(nbt);
    }

    public void readFromNBT(NBTTagCompound nbt) {
        this.conductorRef.readFromNBT(nbt);
    }

    public void writeToBuf(ByteBuf buf) {
        this.conductorRef.writeToByteBuf(buf);
    }

    public void readFromBuf(ByteBuf buf) {
        this.conductorRef.readFromByteBuf(buf);
    }

    @Nullable
    public ConductorType getConductor() {
        if(this instanceof ConductorType) {
            this.conductorRef.reset();
            return null;
        }

        ConductorType type = this.conductorRef.get(this.entity.world);
        if(type != null && type.entity.musicianType != type) {
            this.conductorRef.reset();
            return null;
        }
        return type;
    }


    public EntityFieldReference<MusicalSkeleton, ConductorType> getConductorRef() {
        return this.conductorRef;
    }

    public void setChannel(int channel) {
        this.entity.setChannel(channel);
    }

    public void setTrack(int track) {
        this.entity.setTrackID(track);
        ConductorType type = this.getConductor();
        if(type != null) {
            type.assign(this, type.getCurrentlyPlaying().getTrackAt(track));
        }
    }

    public void setVolume(float volume) {
        this.entity.setVolume(volume);
    }

    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if(player.isSneaking()) {
            this.entity.paused = !this.entity.paused;
        } else if(this.entity.world.isRemote){
            this.displayMidiGui();
        }
        player.swingArm(hand);
        return true;
    }

    @SideOnly(Side.CLIENT)
    protected void displayMidiGui() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiMusician(this.entity.getEntityId(), () -> this.entity.musicianType.factoryType, this.getConductor(), this.entity::getChannel, this.entity::getTrackID, this.entity::getVolume));
    }

    public Vec2f getSize() {
        return new Vec2f(0.6F, 1.99F);
    }

    public ItemStack getHeldItem(EnumHand hand) {
        return this.entity.getHeldItem(hand);
    }

    public void renderExtras(float partialTicks) {
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
