package com.wynprice.boneophone.midi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

import javax.sound.midi.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MidiStream {

    MidiTone[][] data = new MidiTone[0][0];
    private ExceptionSupplier<InputStream> streamSupplier;

    float midiTicksPerMcTick = -1;
    int min;
    int max;

    MidiStream(MidiTone[][] data, float midiTicksPerMcTick, int min, int max) {
        this.data = data;
        this.midiTicksPerMcTick = midiTicksPerMcTick;
        this.min = min;
        this.max = max;
    }

    MidiStream(ExceptionSupplier<InputStream> streamSupplier) {
        this.streamSupplier = streamSupplier;
        this.load();
    }

    public void load() {
        if(this.streamSupplier == null) {
            return;
        }
        Map<Long, List<MidiTone>> map = Maps.newHashMap();

        this.min = Integer.MAX_VALUE;
        this.max = Integer.MIN_VALUE;
        this.midiTicksPerMcTick = -1;
        try {
            Sequence sequence = MidiSystem.getSequence(this.streamSupplier.getWithException());
            float div = sequence.getDivisionType();
            boolean foundTempo = false;

            if(div != Sequence.PPQ) {
                this.midiTicksPerMcTick = sequence.getResolution() * (sequence.getDivisionType() / 20F); //20 ticks per second
                foundTempo = true;
            }
            for (long i = 0; i < sequence.getTickLength(); i++) {
                map.put(i, Lists.newArrayList());
            }
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if(message instanceof MetaMessage) {
                        MetaMessage mm = (MetaMessage) message;
//                        if(mm.getType() == 0x01) { //Text event
//
//                        } else
                        if(mm.getType() == 0x51 && !foundTempo) { //Set Tempo

                            //Tempo is stored as a 3-byte big-endian integer
                            //Microseconds per minute is calculated as 6e7 / (tt tt tt)
                            byte[] data = mm.getData();
                            int tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
                            int bpm = 60000000 / tempo;

                            //Formula for mc ticks per midi tick would be `1200F / (bpm * ppq)`, with 1200 being the
                            //amount of minecraft ticks in a minute (bpm is in minutes). However, I want to find
                            //the amount of midi ticks per mc tick, meaning I would have done `1 / (answer from before)`.
                            //To simplify this, i can just switch the numerator and denominator around.
                            this.midiTicksPerMcTick = (bpm * sequence.getResolution()) / 1200F;
                            foundTempo = true;
                        }
                    } else if(message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if(sm.getCommand() == 0x90) { //ON
                            if(!foundTempo) {
                                throw new IllegalArgumentException("Track started before tempo was established");
                            }
                            map.computeIfAbsent(event.getTick(), l -> Lists.newArrayList()).add(new MidiTone(sm.getData1()));
                            this.min = Math.min(this.min, sm.getData1());
                            this.max = Math.max(this.max, sm.getData1());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if(this.midiTicksPerMcTick == -1) {
            throw new IllegalArgumentException("Error loading. Unable to determine tick ratio: " + this.midiTicksPerMcTick);
        }
        this.data = new MidiTone[map.size()][];
        int t = 0;
        for (Long key : map.keySet()) {
            List<MidiTone> lis = map.get(key);
            MidiTone[] ain = new MidiTone[lis.size()];
            for (int i = 0; i < lis.size(); i++) {
                MidiTone tone = lis.get(i);
                tone.setPosition((float)(tone.getRawKey() - this.min) / (float)(this.max - this.min));
                ain[i] = tone;
            }
            this.data[t++] = ain;
        }

    }

    public MidiTone[] getNotesAt(int ticks) {
        int start = (int) Math.floor(ticks * this.midiTicksPerMcTick);
        int end = (int) Math.floor((ticks + 1) * this.midiTicksPerMcTick);

        List<MidiTone> list = Lists.newArrayList();
        for (int i = start; i < end; i++) {
            Collections.addAll(list, this.data[i % this.data.length]);
        }
        return list.toArray(new MidiTone[0]);
    }

    private static final Map<ResourceLocation, MidiStream> CACHE = Maps.newHashMap();

    public static MidiStream getMidi(ResourceLocation location) {
        ResourceLocation fullLoc = new ResourceLocation(location.getResourceDomain(), "midis/" + location.getResourcePath() + ".mid");
        return CACHE.computeIfAbsent(location, location1 -> new MidiStream(() -> Minecraft.getMinecraft().getResourceManager().getResource(fullLoc).getInputStream()));
    }

    public static class MidiTone {
        final int key;
        private float position = -1F;

        public MidiTone(int key) {
            this.key = key;
        }

        public SoundEvent getEvent() {
            return SoundHandler.BONEOPHONE_OCTAVES[(this.key / 12)];
        }

        public int getKey() {
            return this.key % 12;
        }

        public int getRawKey() {
            return this.key;
        }

        public void setPosition(float position) {
            if(this.position != -1) {
                throw new IllegalArgumentException("Position already has a value");
            }
            this.position = position;
        }

        public float getPosition() {
            return this.position;
        }
    }

    public interface ExceptionSupplier<T> extends Supplier<T> {

        @Override
        default T get() {
            try {
                return this.getWithException();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        T getWithException() throws Exception;
    }
}
