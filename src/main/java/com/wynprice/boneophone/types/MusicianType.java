package com.wynprice.boneophone.types;

import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.gui.GuiMusician;
import com.wynprice.boneophone.midi.MidiStream;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec2f;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MusicianType {

    protected final MusicalSkeleton entity;

    public final MusicianTypeFactory factoryType;

    public MusicianType(MusicalSkeleton entity, MusicianTypeFactory factoryType) {
        this.entity = entity;
        this.factoryType = factoryType;
    }

    public void onTick() {
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
        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt) {
    }

    public void writeToBuf(ByteBuf buf) {
    }

    public void readFromBuf(ByteBuf buf) {
    }

    public void setChannel(int channel) {
        this.entity.setChannel(channel);
    }

    public void setTrack(int track) {
        this.entity.setTrackID(track);
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
        Minecraft.getMinecraft().displayGuiScreen(new GuiMusician(this.entity.getEntityId(), () -> this.entity.musicianType.factoryType, this.entity::getChannel, this.entity::getTrackID));
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
