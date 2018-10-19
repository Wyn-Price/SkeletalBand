package com.wynprice.boneophone.midi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wynprice.boneophone.SkeletalBand;
import io.netty.buffer.ByteBuf;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MidiFileHandler {

    public static final File folder = new File("midis");

    static {
        if(!folder.exists() && !folder.mkdirs()) {
            throw new RuntimeException("Unable to create folder " + folder.getAbsolutePath());
        }
    }

    public static List<File> getAllStreams() {
        List<File> fileList = Lists.newArrayList();
        addFiles(fileList, folder);
        return fileList;
    }

    private static void addFiles(List<File> list, File folderin) {
        for (File file : Objects.requireNonNull(folderin.listFiles())) {
            if(file.isDirectory()) {
                addFiles(list, file);
            } else {
                list.add(file);
            }
        }
    }

    public static byte[] writeMidiFile(File file) {
        return writeMidiFile(new MidiStream(() -> new FileInputStream(file)));
    }

    public static byte[] writeMidiFile(MidiStream stream) {

        long start = System.currentTimeMillis();

        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try(DataOutputStream dos = new DataOutputStream(baos)) {
               dos.writeFloat(stream.midiTicksPerMcTick);
               dos.writeInt(stream.min);
               dos.writeInt(stream.max);

               dos.writeInt(stream.data.length);

                for (int i = 0; i < stream.data.length; i++) {
                    MidiStream.MidiTone[] dataum = stream.data[i];
                    if(dataum.length != 0) {
                       dos.writeInt(i);
                       dos.writeInt(dataum.length);
                        for (MidiStream.MidiTone midiTone : dataum) {
                           dos.writeInt(midiTone.key);
                        }
                    }
                }
               dos.writeInt(-1); //-1 to signify the end of the stream

                byte[] raw = baos.toByteArray();
                ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length);
                try(GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                    gzip.write(raw);
                }
                SkeletalBand.LOGGER.info("Written midi file, took {}ms", System.currentTimeMillis() - start);
                return out.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Error writing midi file to byte array");
    }

    public static void writeBytes(byte[] bytes, ByteBuf buf) {
        buf.writeInt(bytes.length);
        for (byte aByte : bytes) {
            buf.writeByte(aByte);
        }
    }

    public static byte[] readBytes(ByteBuf buf) {
        byte[] out = new byte[buf.readInt()];
        for (int i = 0; i < out.length; i++) {
            out[i] = buf.readByte();
        }
        return out;
    }

    public static MidiStream readMidiFile(byte[] abyte) {
        long start = System.currentTimeMillis();

        InputStream baisRaw = new ByteArrayInputStream(abyte);
        try(GZIPInputStream gzipIn = new GZIPInputStream(baisRaw)) {
            try(DataInputStream is = new DataInputStream(gzipIn)) {
                Map<Integer, List<MidiStream.MidiTone>> toneMap = Maps.newHashMap();

                float ratio = is.readFloat();
                int min = is.readInt();
                int max = is.readInt();

                int total = is.readInt();

                int next = is.readInt();

                while (next != -1) {
                    int amount = is.readInt();
                    for (int i = 0; i < amount; i++) {
                        int key = is.readInt();
                        MidiStream.MidiTone tone = new MidiStream.MidiTone(key);
                        tone.setPosition((float)(key - min) / (float)(max - min));
                        toneMap.computeIfAbsent(next, _i -> Lists.newArrayList()).add(tone);
                    }
                    next = is.readInt();
                }

                MidiStream.MidiTone[][] streams = new MidiStream.MidiTone[total][];

                for (int i = 0; i < total; i++) {
                    streams[i] = new MidiStream.MidiTone[0];
                }

                for (Map.Entry<Integer, List<MidiStream.MidiTone>> entry : toneMap.entrySet()) {
                    streams[entry.getKey()] = entry.getValue().toArray(new MidiStream.MidiTone[0]);
                }

                SkeletalBand.LOGGER.info("Read midi file, took {}ms", System.currentTimeMillis() - start);

                return new MidiStream(streams, ratio, min, max);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Error reading midi file from byte array");
    }
}
