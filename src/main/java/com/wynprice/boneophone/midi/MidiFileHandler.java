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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(baos))) {
            dos.writeFloat(stream.midiTicksPerMcTick);
            dos.writeInt(stream.tracks.length);

            for (MidiStream.MidiTrack track : stream.tracks) {

                dos.writeInt(track.name.length());
                for (char c : track.name.toCharArray()) {
                    dos.writeChar(c);
                }

                dos.writeInt(track.min);
                dos.writeInt(track.max);

                MidiStream.MidiTone[][] data = track.getData();
                dos.writeInt(data.length);
                for (int i = 0; i < data.length; i++) {
                    MidiStream.MidiTone[] dataum = data[i];
                    if(dataum != null && dataum.length != 0) {
                        dos.writeInt(i);
                        dos.writeInt(dataum.length);
                        for (MidiStream.MidiTone midiTone : dataum) {
                            dos.writeInt(midiTone.key);
                        }
                    }
                }
                dos.writeInt(-1); //-1 to signify the end of the stream
            }


            SkeletalBand.LOGGER.info("Written midi file, took {}ms", System.currentTimeMillis() - start);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
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
                List<MidiStream.MidiTrack> trackList = Lists.newArrayList();

                float ratio = is.readFloat();

                int total = is.readInt();

                for (int trackID = 0; trackID < total; trackID++) {
                    Map<Integer, List<MidiStream.MidiTone>> tones = Maps.newHashMap();

                    StringBuilder name = new StringBuilder();
                    int nameLength = is.readInt();
                    for (int i = 0; i < nameLength; i++) {
                        name.append(is.readChar());
                    }

                    int min = is.readInt();
                    int max = is.readInt();

                    int size = is.readInt();
                    int next = is.readInt();
                    while(next != -1) {
                        int amount = is.readInt();
                        for (int i = 0; i < amount; i++) {
                            int key = is.readInt();
                            MidiStream.MidiTone tone = new MidiStream.MidiTone(key);
                            tone.setPosition((float)(key - min) / (float)(max - min));
                            tones.computeIfAbsent(next, _i -> Lists.newArrayList()).add(tone);
                        }
                        next = is.readInt();
                    }
                    MidiStream.MidiTone[][] atone = new MidiStream.MidiTone[size][];
                    int noteAmount = 0;
                    for (Map.Entry<Integer, List<MidiStream.MidiTone>> entry : tones.entrySet()) {
                        noteAmount += entry.getValue().size();
                        atone[entry.getKey()] = entry.getValue().toArray(new MidiStream.MidiTone[0]);

                    }
                    trackList.add(new MidiStream.MidiTrack(name.toString(), trackID, noteAmount, min, max, ratio, atone));
                }
                SkeletalBand.LOGGER.info("Read midi file, took {}ms", System.currentTimeMillis() - start);
                return new MidiStream(trackList.toArray(new MidiStream.MidiTrack[0]), ratio);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Error reading midi file from byte array");
    }
}
