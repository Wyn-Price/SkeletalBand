package com.wynprice.boneophone.network;

import com.wynprice.boneophone.entity.MusicalSkeleton;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class S9SyncSkeletonChangeTrack implements IMessage {

    private int entityID;
    private int track;

    @SuppressWarnings("unused")
    public S9SyncSkeletonChangeTrack() {
    }

    public S9SyncSkeletonChangeTrack(int entityID, int track) {
        this.entityID = entityID;
        this.track = track;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityID = buf.readInt();
        this.track = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeInt(this.track);
    }

    public static class Handler extends WorldModificationsMessageHandler<S9SyncSkeletonChangeTrack, IMessage> {
        @Override
        protected void handleMessage(S9SyncSkeletonChangeTrack message, MessageContext ctx, World world, EntityPlayer player) {
            Entity entity = world.getEntityByID(message.entityID);
            if(entity instanceof MusicalSkeleton) {
                ((MusicalSkeleton) entity).musicianType.setTrack(message.track);
            }
        }
    }
}

