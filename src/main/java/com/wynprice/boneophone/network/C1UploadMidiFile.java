package com.wynprice.boneophone.network;

import com.wynprice.boneophone.Boneophone;
import com.wynprice.boneophone.midi.MidiFileHandler;
import com.wynprice.boneophone.midi.MidiStream;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.File;

public class C1UploadMidiFile implements IMessage {

    @SuppressWarnings("unused")
    public C1UploadMidiFile() {
    }

    private int entityID;

    private File midiFileIn;
    private MidiStream midiStreamOut;

    public C1UploadMidiFile(int entityID, File midiFile) {
        this.entityID = entityID;
        this.midiFileIn = midiFile;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityID = buf.readInt();
        this.midiStreamOut = MidiFileHandler.readMidiFile(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityID);
        MidiFileHandler.writeMidiFile(this.midiFileIn, buf);
    }

    public static class Handler extends WorldModificationsMessageHandler<C1UploadMidiFile, IMessage> {

        @Override
        protected void handleMessage(C1UploadMidiFile message, MessageContext ctx, World world, EntityPlayer player) {
            Boneophone.NETWORK.sendToDimension(new S2SyncAndPlayMidi(message.entityID, message.midiStreamOut), world.provider.getDimension());
        }
    }
}
