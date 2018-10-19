package com.wynprice.boneophone.network;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.gui.MidiSplitNetworkHandler;
import com.wynprice.boneophone.midi.MidiFileHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class C3SplitUploadMidiFile implements IMessage {
    private static int usedIds;

    private int entityID;
    private int collectionID;
    private int index;
    private int total;
    private byte[] data;

    @SuppressWarnings("unused")
    public C3SplitUploadMidiFile() {
    }

    public C3SplitUploadMidiFile(int entityID, int collectionID, int index, int total, byte[] data) {
        this.entityID = entityID;
        this.collectionID = collectionID;
        this.index = index;
        this.total = total;
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityID = buf.readInt();
        this.collectionID = buf.readInt();
        this.index = buf.readInt();
        this.total = buf.readInt();
        this.data = MidiFileHandler.readBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeInt(this.collectionID);
        buf.writeInt(this.index);
        buf.writeInt(this.total);
        MidiFileHandler.writeBytes(this.data, buf);
    }

    public static int getNextAvalibleId() {
        return usedIds++ % 50000;
    }

    public static class Handler extends WorldModificationsMessageHandler<C3SplitUploadMidiFile, IMessage> {

        @Override
        protected void handleMessage(C3SplitUploadMidiFile message, MessageContext ctx, World world, EntityPlayer player) {
            byte[] data = MidiSplitNetworkHandler.getMidiData(message.collectionID, message.index, message.total, message.data);
            if(data != null) {
                SkeletalBand.NETWORK.sendToDimension(new S2SyncAndPlayMidi(message.entityID, data), world.provider.getDimension());
            }
        }
    }
}
