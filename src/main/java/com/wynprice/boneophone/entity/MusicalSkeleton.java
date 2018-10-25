package com.wynprice.boneophone.entity;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.midi.MidiStream;
import com.wynprice.boneophone.types.BoneophoneType;
import com.wynprice.boneophone.types.ConductorType;
import com.wynprice.boneophone.types.MusicianType;
import com.wynprice.boneophone.types.MusicianTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import javax.annotation.Nullable;
import java.util.Objects;

public class MusicalSkeleton extends EntityCreature implements IEntityAdditionalSpawnData {

    public boolean paused;

    private int channel;
    private int trackID;

    private float volume = 0.5F;

    public SkeletonType type = SkeletonType.NORMAL;

    public MusicianType musicianType = MusicianTypes.CONDUCTOR.createType(this);

    public MusicalSkeleton(World worldIn) {
        super(worldIn);
        this.setSize(0.6F, 1.99F);
    }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();

        this.tasks.addTask(0, new AiFindFreind());

        this.tasks.addTask(1, new EntityAISwimming(this));

        this.tasks.addTask(5, new MusicalSkeleton.AiWander(0.2D));
        this.tasks.addTask(6, new MusicalSkeleton.AIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(6, new MusicalSkeleton.AILookIdle(this));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        this.musicianType.onTick();

        Vec2f vec = this.musicianType.getSize();
        this.setSize(vec.x, vec.y);
    }

    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("SkeletonType", this.type.ordinal());
        nbt.setInteger("Channel", this.channel);
        nbt.setInteger("Track", this.trackID);
        nbt.setBoolean("Paused", this.paused);
        nbt.setFloat("Volume", this.volume);

        nbt.setString("MusicianFactoryType", Objects.requireNonNull(this.musicianType.factoryType.getRegistryName()).toString());
        nbt.setTag("MusicianType", this.musicianType.writeToNBT(new NBTTagCompound()));

        return super.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.type = SkeletonType.values()[nbt.getInteger("SkeletonType") % SkeletonType.values().length];
        this.channel = nbt.getInteger("Channel");
        this.trackID = nbt.getInteger("Track");
        this.paused = nbt.getBoolean("Paused");
        this.volume = nbt.getFloat("Volume");

        this.musicianType = Objects.requireNonNull(SkeletalBand.MUSICIAN_REGISTRY.getValue(new ResourceLocation(nbt.getString("MusicianFactoryType")))).createType(this);
        this.musicianType.readFromNBT(nbt.getCompoundTag("MusicianType"));
        super.readFromNBT(nbt);
    }

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        return this.musicianType.processInteract(player, hand);
    }

    @Override
    protected void collideWithEntity(Entity entityIn) {
        if(this.musicianType instanceof BoneophoneType) {
            BoneophoneType type = ((BoneophoneType) this.musicianType).fieldReference.get(entityIn.world);
            if(type != null && type.getEntity() == entityIn) {
                return;
            }
        }
        super.collideWithEntity(entityIn);
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
        IEntityLivingData data =  super.onInitialSpawn(difficulty, livingdata);
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.BONE));
        this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.BONE));
        return data;
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(this.type.ordinal());
        buffer.writeInt(this.channel);
        buffer.writeInt(this.trackID);
        buffer.writeBoolean(this.paused);
        buffer.writeFloat(this.volume);
        ByteBufUtils.writeRegistryEntry(buffer, this.musicianType.factoryType);
        this.musicianType.writeToBuf(buffer);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        this.type = SkeletonType.values()[additionalData.readInt()];
        this.channel = additionalData.readInt();
        this.trackID = additionalData.readInt();
        this.paused = additionalData.readBoolean();
        this.volume = additionalData.readFloat();
        this.musicianType = ByteBufUtils.readRegistryEntry(additionalData, SkeletalBand.MUSICIAN_REGISTRY).createType(this);
        this.musicianType.readFromBuf(additionalData);
    }

    public int getChannel() {
        return this.channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getTrackID() {
        return this.trackID;
    }

    public void setTrackID(int trackID) {
        this.trackID = trackID;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getVolume() {
        return this.volume;
    }

    private class AiWander extends EntityAIWanderAvoidWater {

        public AiWander(double speedIn) {
            super(MusicalSkeleton.this, speedIn);
        }

        @Override
        public boolean shouldExecute() {
            return !MusicalSkeleton.this.musicianType.shouldStopAiTasks() && super.shouldExecute();
        }
    }

    private class AIWatchClosest extends EntityAIWatchClosest {

        public AIWatchClosest(EntityLiving entityIn, Class<? extends Entity> watchTargetClass, float maxDistance) {
            super(entityIn, watchTargetClass, maxDistance);
        }

        @Override
        public boolean shouldExecute() {
            return !MusicalSkeleton.this.musicianType.shouldStopAiTasks() && super.shouldExecute();
        }
    }

    private class AILookIdle extends EntityAILookIdle {

        public AILookIdle(EntityLiving entitylivingIn) {
            super(entitylivingIn);
        }

        @Override
        public boolean shouldExecute() {
            return !MusicalSkeleton.this.musicianType.shouldStopAiTasks() && super.shouldExecute();
        }

    }
    private class AiFindFreind extends EntityAIBase {

        @Override
        public boolean shouldExecute() {
            return MusicalSkeleton.this.musicianType.shouldAIExecute();
        }

        @Override
        public void updateTask() {
            MusicalSkeleton.this.musicianType.updateAITask();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return MusicalSkeleton.this.musicianType.shouldAIContinueExecuting();
        }
    }

    public enum SkeletonType {
        NORMAL, WITHER, STRAY
    }
}
