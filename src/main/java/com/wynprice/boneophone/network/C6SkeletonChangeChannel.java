package com.wynprice.boneophone.network;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class C6SkeletonChangeChannel implements IMessage {

    private int entityID;
    private int channel;

    @SuppressWarnings("unused")
    public C6SkeletonChangeChannel() {
    }

    public C6SkeletonChangeChannel(int entityID, int channel) {
        this.entityID = entityID;
        this.channel = channel;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityID = buf.readInt();
        this.channel = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeInt(this.channel);
    }

    public static class Handler extends WorldModificationsMessageHandler<C6SkeletonChangeChannel, IMessage> {
        @Override
        protected void handleMessage(C6SkeletonChangeChannel message, MessageContext ctx, World world, EntityPlayer player) {
            Entity entity = world.getEntityByID(message.entityID);
            if(entity instanceof MusicalSkeleton) {
                ((MusicalSkeleton) entity).musicianType.setChannel(message.channel);
                SkeletalBand.NETWORK.sendToDimension(new S7SyncSkeletonChangeChannel(message.entityID, message.channel), world.provider.getDimension());
            }
        }
    }
}
