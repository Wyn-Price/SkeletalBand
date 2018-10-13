package com.wynprice.boneophone.midi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wynprice.boneophone.Boneophone;
import io.netty.buffer.ByteBuf;
import io.netty.util.internal.PlatformDependent;

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

            writeInt(baos, Float.floatToRawIntBits(stream.midiTicksPerMcTick));
            writeInt(baos, stream.min);
            writeInt(baos, stream.max);

            writeInt(baos, stream.data.length);

            for (int i = 0; i < stream.data.length; i++) {
                MidiStream.MidiTone[] dataum = stream.data[i];
                if(dataum.length != 0) {
                    writeInt(baos, i);
                    writeInt(baos, dataum.length);
                    for (MidiStream.MidiTone midiTone : dataum) {
                        writeInt(baos, midiTone.key);
                    }
                }
            }
            writeInt(baos, -1); //-1 to signify the end of the stream

            byte[] raw = baos.toByteArray();

            try(ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length)) {
                try(GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                    gzip.write(raw);
                }
                Boneophone.LOGGER.info("Written midi file, took {}ms", System.currentTimeMillis() - start);
                return out.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Error writing midi file to byte array");
    }

    private static void writeInt(ByteArrayOutputStream baos, int value) {
        baos.write(value >>> 24);
        baos.write(value >>> 16);
        baos.write(value >>> 8);
        baos.write(value);
    }

    private static int readInt(InputStream is) throws IOException {
        return is.read() << 24 |
                (is.read() & 0xff) << 16 |
                (is.read() & 0xff) <<  8 |
                is.read() & 0xff;
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

        try(InputStream baisRaw = new ByteArrayInputStream(abyte)) {
            try(InputStream is = new GZIPInputStream(baisRaw)) {
                Map<Integer, List<MidiStream.MidiTone>> toneMap = Maps.newHashMap();

                float ratio = Float.intBitsToFloat(readInt(is));
                int min = readInt(is);
                int max = readInt(is);

                int total = readInt(is);

                int next = readInt(is);

                while (next != -1) {
                    int amount = readInt(is);
                    for (int i = 0; i < amount; i++) {
                        int key = readInt(is);
                        MidiStream.MidiTone tone = new MidiStream.MidiTone(key);
                        tone.setPosition((float)(key - min) / (float)(max - min));
                        toneMap.computeIfAbsent(next, _i -> Lists.newArrayList()).add(tone);
                    }
                    next = readInt(is);
                }


                MidiStream.MidiTone[][] streams = new MidiStream.MidiTone[total][];

                for (int i = 0; i < total; i++) {
                    streams[i] = new MidiStream.MidiTone[0];
                }

                for (Map.Entry<Integer, List<MidiStream.MidiTone>> entry : toneMap.entrySet()) {
                    streams[entry.getKey()] = entry.getValue().toArray(new MidiStream.MidiTone[0]);
                }

                Boneophone.LOGGER.info("Read midi file, took {}ms", System.currentTimeMillis() - start);

                return new MidiStream(streams, ratio, min, max);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Error reading midi file from byte array");
    }
}
