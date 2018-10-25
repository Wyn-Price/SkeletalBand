package com.wynprice.boneophone.network;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class C11SkeletonChangeVolume implements IMessage {

    private int entityID;
    private float volume;

    @SuppressWarnings("unused")
    public C11SkeletonChangeVolume() {
    }

    public C11SkeletonChangeVolume(int entityID, float track) {
        this.entityID = entityID;
        this.volume = track;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityID = buf.readInt();
        this.volume = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeFloat(this.volume);
    }

    public static class Handler extends WorldModificationsMessageHandler<C11SkeletonChangeVolume, IMessage> {
        @Override
        protected void handleMessage(C11SkeletonChangeVolume message, MessageContext ctx, World world, EntityPlayer player) {
            Entity entity = world.getEntityByID(message.entityID);
            if(entity instanceof MusicalSkeleton) {
                ((MusicalSkeleton) entity).musicianType.setVolume(message.volume);
                SkeletalBand.NETWORK.sendToDimension(new S12SyncSkeletonChangeVolume(message.entityID, message.volume), world.provider.getDimension());
            }
        }
    }
}
