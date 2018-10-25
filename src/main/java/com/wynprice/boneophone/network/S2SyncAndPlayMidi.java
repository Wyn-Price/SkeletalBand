package com.wynprice.boneophone.network;

import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.midi.MidiFileHandler;
import com.wynprice.boneophone.midi.MidiStream;
import com.wynprice.boneophone.types.ConductorType;
import com.wynprice.boneophone.types.MusicianType;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.commons.codec.digest.DigestUtils;

public class S2SyncAndPlayMidi implements IMessage {

    private int entityID;
    private byte[] abyte;

    private boolean restartTimer;

    @SuppressWarnings("unused")
    public S2SyncAndPlayMidi() {
    }

    public S2SyncAndPlayMidi(int entityID, byte[] abyte, boolean restartTimer) {
        this.entityID = entityID;
        this.abyte = abyte;
        this.restartTimer = restartTimer;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeBoolean(this.restartTimer);
        MidiFileHandler.writeBytes(this.abyte, buf);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityID = buf.readInt();
        this.restartTimer = buf.readBoolean();
        this.abyte = MidiFileHandler.readBytes(buf);
    }

    public static class Handler extends WorldModificationsMessageHandler<S2SyncAndPlayMidi, IMessage> {

        @Override
        protected void handleMessage(S2SyncAndPlayMidi message, MessageContext ctx, World world, EntityPlayer player) {
            Entity entity = world.getEntityByID(message.entityID);
            if(entity instanceof MusicalSkeleton) {
                MusicianType type = ((MusicalSkeleton) entity).musicianType;
                if(type instanceof ConductorType) {
                    if(message.restartTimer) {
                        ((ConductorType) type).setCurrentlyPlaying(MidiFileHandler.readMidiFile(message.abyte));
                    } else {
                        ((ConductorType) type).setCurrentlyPlayingRaw(MidiFileHandler.readMidiFile(message.abyte));
                    }
                }
            }
        }
    }

}
