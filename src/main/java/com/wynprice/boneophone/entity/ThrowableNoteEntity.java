package com.wynprice.boneophone.entity;

import com.google.common.collect.Lists;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.util.List;

public class ThrowableNoteEntity extends EntityThrowable {

    private static final List<SoundEvent> INSTRUMENTS = Lists.newArrayList(SoundEvents.BLOCK_NOTE_HARP, SoundEvents.BLOCK_NOTE_BASEDRUM, SoundEvents.BLOCK_NOTE_SNARE, SoundEvents.BLOCK_NOTE_HAT, SoundEvents.BLOCK_NOTE_BASS, SoundEvents.BLOCK_NOTE_FLUTE, SoundEvents.BLOCK_NOTE_BELL, SoundEvents.BLOCK_NOTE_GUITAR, SoundEvents.BLOCK_NOTE_CHIME, SoundEvents.BLOCK_NOTE_XYLOPHONE);


    public int instrument;
    public int note;

    public ThrowableNoteEntity(World worldIn) {
        super(worldIn);
    }

    public ThrowableNoteEntity(World worldIn, EntityLivingBase throwerIn, int note, int instrument) {
        super(worldIn, throwerIn);
        this.note = note;
        this.instrument = instrument;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        compound.setInteger("Note", this.note);
        compound.setInteger("Instrument", this.instrument);
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        this.note = compound.getInteger("Note");
        this.instrument = compound.getInteger("Instrument");
        return super.writeToNBT(compound);
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (result.hitVec != null) {
            float f = (float)Math.pow(2.0D, (double)(this.note - 12) / 12.0D);
            this.world.playSound(null, this.getPosition(), INSTRUMENTS.get(this.instrument < 0 || this.instrument >= INSTRUMENTS.size() ? 0 : this.instrument), SoundCategory.RECORDS, 3.0F, f);
            this.world.spawnParticle(EnumParticleTypes.NOTE, this.posX, this.posY, this.posZ, (double)this.note / 24.0D, 0.0D, 0.0D);
            this.setDead();


            if(!world.isRemote) {
                for (AbstractSkeleton skeleton : world.getEntitiesWithinAABB(AbstractSkeleton.class, new AxisAlignedBB(result.hitVec.x, result.hitVec.y, result.hitVec.z, result.hitVec.x, result.hitVec.y, result.hitVec.z).grow(3D))) {
                    skeleton.setDead();
                    MusicalSkeleton mus = new MusicalSkeleton(world);
                    mus.setPositionAndRotation(skeleton.posX, skeleton.posY, skeleton.posZ, skeleton.rotationYaw, skeleton.rotationPitch);
                    mus.onInitialSpawn(world.getDifficultyForLocation(skeleton.getPosition()), null);
                    world.spawnEntity(mus);
                }
            }

        }
    }
}
