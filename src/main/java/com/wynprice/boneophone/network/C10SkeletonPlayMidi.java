package com.wynprice.boneophone.network;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.midi.MidiFileHandler;
import com.wynprice.boneophone.midi.MidiStream;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class C10SkeletonPlayMidi implements IMessage {

    private int entityId;
    private String hash;

    @SuppressWarnings("unused")
    public C10SkeletonPlayMidi() {
    }

    public C10SkeletonPlayMidi(int entityId, String hash) {
        this.entityId = entityId;
        this.hash = hash;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        ByteBufUtils.writeUTF8String(buf, this.hash);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.hash = ByteBufUtils.readUTF8String(buf);
    }

    public static class Handler extends WorldModificationsMessageHandler<C10SkeletonPlayMidi, IMessage> {

        @Override
        protected void handleMessage(C10SkeletonPlayMidi message, MessageContext ctx, World world, EntityPlayer player) {
            SkeletalBand.NETWORK.sendToDimension(new S2SyncAndPlayMidi(message.entityId, MidiFileHandler.getWorldMidi(message.hash), false), world.provider.getDimension());
        }
    }
}
