package com.wynprice.boneophone.network;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.types.ConductorType;
import com.wynprice.boneophone.types.MusicianTypeFactory;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class C4SkeletonChangeType implements IMessage {

    private int entityId;
    private MusicianTypeFactory type;

    @SuppressWarnings("unused")
    public C4SkeletonChangeType() {
    }

    public C4SkeletonChangeType(int entityId, MusicianTypeFactory type) {
        this.entityId = entityId;
        this.type = type;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.type = ByteBufUtils.readRegistryEntry(buf, SkeletalBand.MUSICIAN_REGISTRY);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        ByteBufUtils.writeRegistryEntry(buf, this.type);
    }

    public static class Handler extends WorldModificationsMessageHandler<C4SkeletonChangeType, IMessage> {

        @Override
        protected void handleMessage(C4SkeletonChangeType message, MessageContext ctx, World world, EntityPlayer player) {
            Entity entity = world.getEntityByID(message.entityId);
            if(entity instanceof MusicalSkeleton) {
                MusicalSkeleton skeleton = (MusicalSkeleton) entity;
                skeleton.musicianType = message.type.createType(skeleton);
                for (Entity e : world.loadedEntityList) {
                    if(e instanceof MusicalSkeleton && !(((MusicalSkeleton) e).musicianType instanceof ConductorType)) {
                        ((MusicalSkeleton) e).musicianType.getConductorRef().setReferenceFromEntity(skeleton);
                    }
                }
                SkeletalBand.NETWORK.sendToDimension(new S5SyncSkeletonChangeType(message.entityId, message.type), world.provider.getDimension());
            }
        }
    }
}
