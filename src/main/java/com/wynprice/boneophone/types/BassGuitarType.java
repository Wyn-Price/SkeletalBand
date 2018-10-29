package com.wynprice.boneophone.types;

import com.wynprice.boneophone.ModelHandler;
import com.wynprice.boneophone.SoundHandler;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.midi.MidiStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BassGuitarType extends MusicianType {

    private float prevNotePosition;
    private float notePosition;
    private float handTicksToHit;

    private float rotation;

    private int rightHandHitTicks;
    private int hitDirection = 1;
    private float hitRotation;

    public BassGuitarType(MusicalSkeleton entity, MusicianTypeFactory factoryType) {
        super(entity, factoryType);
    }

    @Override
    public void playTones(MidiStream.MidiTone[] tones) {
        if(this.entity.world.isRemote) {
            for (MidiStream.MidiTone tone : tones) {
                this.playRawSound(SoundHandler.BASS_OCTAVES[tone.getOctave()],  this.entity.getVolume() * 2F, (float) Math.pow(2.0D, (tone.getKey() / 12.0D)));
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

    @Override
    public void renderExtras(float partialTicks) {
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.5F, 1.5F, 1.5F);
        GlStateManager.rotate(180, 1, 0, 0);
        GlStateManager.translate(-0.05F, -0.32F, 0.15F);
        GlStateManager.rotate(-20F, 0, 1F, 0);
        GlStateManager.rotate(-65F, 0, 0, 1F);
        Minecraft.getMinecraft().getRenderItem().renderItem(new ItemStack(Blocks.STONE), ModelHandler.GUITAR_MODEL);
        GlStateManager.popMatrix();
    }

    @Override
    public void onTick() {
        this.handTicksToHit++;
        this.rightHandHitTicks++;
        super.onTick();
    }

    @Override
    public void setAnimationsFromTones(MidiStream.MidiTone[] tones) {
        if(tones.length == 0) {
            return;
        }
        this.prevNotePosition = notePosition;
        this.notePosition = tones[0].getPosition();
        this.handTicksToHit = 0;
        this.hitDirection *= -1;
        this.rightHandHitTicks = 0;
    }

    @Override
    public void setEntityAnimations(float partialTicks, ModelBase model) {
        if(model instanceof ModelBiped) {
            ModelBiped biped = (ModelBiped) model;
            biped.bipedRightArm.rotateAngleX = (float) Math.toRadians(-40F);
            biped.bipedLeftArm.rotateAngleX = (float) Math.toRadians(-60F);

            int ticksToHit = 2;//Faster ticks to hit

            if(this.handTicksToHit <= ticksToHit) {
                this.rotation = (float) (Math.toRadians(55F) * (this.prevNotePosition + (this.notePosition - this.prevNotePosition) * (this.handTicksToHit + partialTicks) / ticksToHit));
            }

            if(this.rightHandHitTicks <= ticksToHit) {
                this.hitRotation = (float) (Math.toRadians(5F) *  (-2F * this.hitDirection * ((this.rightHandHitTicks + partialTicks) / ticksToHit)));
            }

            biped.bipedLeftArm.rotateAngleZ = this.rotation;
            biped.bipedRightArm.rotateAngleZ = this.hitRotation;

        }
    }
}
