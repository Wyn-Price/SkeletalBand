package com.wynprice.boneophone.midi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wynprice.boneophone.Boneophone;
import io.netty.buffer.ByteBuf;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public static void writeMidiFile(File file, ByteBuf buf) {
        writeMidiFile(new MidiStream(() -> new FileInputStream(file)), buf);
    }

    public static void writeMidiFile(MidiStream stream, ByteBuf buf) {

        long start = System.currentTimeMillis();

        buf.writeFloat(stream.midiTicksPerMcTick);
        buf.writeInt(stream.min);
        buf.writeInt(stream.max);

        buf.writeInt(stream.data.length);

        for (int i = 0; i < stream.data.length; i++) {
            MidiStream.MidiTone[] dataum = stream.data[i];
            if(dataum.length != 0) {
                buf.writeInt(i);
                buf.writeInt(dataum.length);
                for (MidiStream.MidiTone midiTone : dataum) {
                    buf.writeInt(midiTone.key);
                }
            }
        }
        buf.writeInt(-1); //-1 to signify the end of the stream

        Boneophone.LOGGER.info("Written midi file, took {}ms", System.currentTimeMillis() - start);

    }

    public static MidiStream readMidiFile(ByteBuf buf) {
        long start = System.currentTimeMillis();

        Map<Integer, List<MidiStream.MidiTone>> toneMap = Maps.newHashMap();

        float ratio = buf.readFloat();
        int min = buf.readInt();
        int max = buf.readInt();

        int total = buf.readInt();

        int next = buf.readInt();

        while (next != -1) {
            int amount = buf.readInt();
            for (int i = 0; i < amount; i++) {
                int key = buf.readInt();
                MidiStream.MidiTone tone = new MidiStream.MidiTone(key);
                tone.setPosition((float)(key - min) / (float)(max - min));
                toneMap.computeIfAbsent(next, _i -> Lists.newArrayList()).add(tone);
            }
            next = buf.readInt();
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

}
