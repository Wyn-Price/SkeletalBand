package com.wynprice.boneophone.midi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import javax.sound.midi.*;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MidiStream {

    public final String hash;
    MidiTrack[] tracks = new MidiTrack[0];
    //    MidiTone[][] data = new MidiTone[0][0];
    private ExceptionSupplier<InputStream> streamSupplier;

    float midiTicksPerMcTick = -1;

    MidiStream(MidiTrack[] tracks, float midiTicksPerMcTick) {
        this.tracks = tracks;
        this.midiTicksPerMcTick = midiTicksPerMcTick;
        this.hash = MidiFileHandler.getMD5(this);
    }

    public MidiStream(ExceptionSupplier<InputStream> streamSupplier) {
        this.streamSupplier = streamSupplier;
        this.load();
        this.hash = MidiFileHandler.getMD5(this);
    }

    public void load() {
        if(this.streamSupplier == null) {
            return;
        }
        List<MidiTrack> trackList = Lists.newArrayList();
        this.midiTicksPerMcTick = -1;


        try {
            Sequence sequence = MidiSystem.getSequence(this.streamSupplier.getWithException());
            float div = sequence.getDivisionType();
            boolean foundTempo = false;

            long size = sequence.getTickLength() + 1;

            if(div != Sequence.PPQ) {
                this.midiTicksPerMcTick = sequence.getResolution() * (sequence.getDivisionType() / 20F); //20 ticks per second
                foundTempo = true;
            }

            for (Track track : sequence.getTracks()) {
                Map<Long, List<MidiTone>> map = Maps.newHashMap();

                int min = Integer.MAX_VALUE;
                int max = Integer.MIN_VALUE;

                String name04 = "";
                String name03 = "";

                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if(message instanceof MetaMessage) {
                        MetaMessage mm = (MetaMessage) message;
//                        if(mm.getType() == 0x01) { //Text event
//
//                        } else
                        if(mm.getType() == 0x04 || mm.getType() == 0x03) { //Instruemtn name
                            StringBuilder sb = new StringBuilder();
                            for (byte b : mm.getData()) {
                                sb.append((char)b);
                            }
                            if(mm.getType() == 0x04) {
                                name04 = sb.toString();
                            } else {
                                name03 = sb.toString();
                            }
                        } else if(mm.getType() == 0x51 && !foundTempo) { //Set Tempo

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
                                //110 seems to be the default value if no bpm is set. Substituting that in with the same
                                //Comments applying from before
                                this.midiTicksPerMcTick = (110 * sequence.getResolution()) / 1200F;
                                foundTempo = true;
                            }
                            map.computeIfAbsent(event.getTick(), l -> Lists.newArrayList()).add(new MidiTone(sm.getData1()));
                            min = Math.min(min, sm.getData1());
                            max = Math.max(max, sm.getData1());
                        }
                    }
                }
                int totalNotes = 0;
                MidiTone[][] data = new MidiTone[Math.toIntExact(size)][];
                for (Long key : map.keySet()) {
                    List<MidiTone> lis = map.get(key);
                    MidiTone[] ain = new MidiTone[lis.size()];
                    for (int i = 0; i < lis.size(); i++) {
                        totalNotes++;
                        MidiTone tone = lis.get(i);
                        tone.setPosition((float)(tone.getRawKey() - min) / (float)(max - min));
                        ain[i] = tone;
                    }
                    data[Math.toIntExact(key)] = ain;
                }
                if(totalNotes != 0) {
                    trackList.add(new MidiTrack(name04.isEmpty() ? name03 : name04, trackList.size(), totalNotes, min, max, this.midiTicksPerMcTick, data));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if(this.midiTicksPerMcTick == -1) {
            throw new IllegalArgumentException("Error loading. Unable to determine tick ratio");
        }


        this.tracks = trackList.toArray(new MidiTrack[0]);
    }

    public MidiTrack getTrackAt(int index) {
        if(this.tracks.length == 0) {
            return MidiTrack.EMPTY;
        }
        if(index < 0 || index >= this.tracks.length) {
            return this.tracks[0];
        }
        return this.tracks[index];
    }

    public List<MidiTrack> getTracks() {
        List<MidiTrack> out = Lists.newLinkedList();
        Collections.addAll(out, this.tracks);
        return out;
    }

    private static final Map<ResourceLocation, MidiStream> CACHE = Maps.newHashMap();

    public static MidiStream getMidi(ResourceLocation location) {
        ResourceLocation fullLoc = new ResourceLocation(location.getResourceDomain(), "midis/" + location.getResourcePath() + ".mid");
        return CACHE.computeIfAbsent(location, location1 -> new MidiStream(() -> Minecraft.getMinecraft().getResourceManager().getResource(fullLoc).getInputStream()));
    }

    public static class MidiTrack {

        public static final MidiTrack EMPTY = new MidiTrack("", 0, 0, 0, 0, 0, new MidiTone[0][]);

        public final String name;
        public final int id;
        public final int totalNotes;
        public final int min;
        public final int max;
        private final float midiTicksPerMcTick;
        private final MidiTone[][] data;

        public MidiTrack(String name, int id, int totalNotes, int min, int max, float midiTicksPerMcTick, MidiTone[][] data) {
            this.name = name;
            this.id = id;
            this.totalNotes = totalNotes;
            this.min = min;
            this.max = max;
            this.midiTicksPerMcTick = midiTicksPerMcTick;
            this.data = data;
        }

        public MidiTone[] getNotesAt(int ticks) {
            int start = (int) Math.floor(ticks * this.midiTicksPerMcTick);
            int end = (int) Math.floor((ticks + 1) * this.midiTicksPerMcTick);

            List<MidiTone> list = Lists.newArrayList();
            for (int i = start; i < end; i++) {
                MidiTone[] dataum = this.data[i % this.data.length];
                if(dataum != null) {
                    Collections.addAll(list, dataum);
                }
            }
            return list.toArray(new MidiTone[0]);
        }

        public MidiTone[][] getData() {
            return data;
        }
    }

    public static class MidiTone {
        final int key;
        private float position = -1F;

        public MidiTone(int key) {
            this.key = key;
        }

        public int getOctave() {
            return this.key / 12;
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
