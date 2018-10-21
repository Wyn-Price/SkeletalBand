package com.wynprice.boneophone.network;

import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.types.BoneophoneType;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class S0MusicalSkeletonStateUpdate implements IMessage {

    private int entityID;
    private int freindId;
    private int mode;

    @SuppressWarnings("unused")
    public S0MusicalSkeletonStateUpdate() {
    }

    public S0MusicalSkeletonStateUpdate(int entityID, int freindId, boolean playing, boolean keyboard) {
        this.entityID = entityID;
        this.freindId = freindId;
        if(playing == keyboard) {
            if(playing) {
                throw new IllegalArgumentException("Unable to turn playing and keyboard on at the same time. Choose between one");
            } else {
                this.mode = 0;
            }
        } else {
            if(playing) {
                this.mode = 1;
            } else {
                this.mode = 2;
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityID = buf.readInt();
        this.freindId = buf.readInt();
        this.mode = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeInt(this.freindId);
        buf.writeInt(this.mode);
    }

    public static class Handler extends WorldModificationsMessageHandler<S0MusicalSkeletonStateUpdate, IMessage> {

        @Override
        protected void handleMessage(S0MusicalSkeletonStateUpdate message, MessageContext ctx, World world, EntityPlayer player) {
            Entity entity = world.getEntityByID(message.entityID);
            Entity freind = world.getEntityByID(message.freindId);
            if(entity instanceof MusicalSkeleton && freind instanceof MusicalSkeleton) {
                MusicalSkeleton skeleton = (MusicalSkeleton) entity;
                if(skeleton.musicianType instanceof BoneophoneType) {
                    BoneophoneType b = (BoneophoneType) skeleton.musicianType;
                    b.freind = (MusicalSkeleton) freind;
                    b.isPlaying = message.mode == 1;
                    b.isKeyboard = message.mode == 2;
                    if(message.mode == 2) {
                        b.keyboardRotationYaw = skeleton.rotationYaw;
                    }
                }
            }
        }
    }
}
