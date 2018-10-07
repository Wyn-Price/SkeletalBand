package com.wynprice.boneophone.midi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wynprice.boneophone.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;

import javax.sound.midi.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class MidiStream {

    private MidiTone[][] data = new MidiTone[0][0];
    private final ResourceLocation location;
    private int bpm = -1;

    private MidiStream(ResourceLocation location) {
        this.location = new ResourceLocation(location.getResourceDomain(), "midis/" + location.getResourcePath() + ".mid");
        this.load(Minecraft.getMinecraft().getResourceManager());
        ((IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this::load); //Hmmmmmm
    }

    public void load(IResourceManager manager) {
        Map<Long, List<MidiTone>> map = Maps.newHashMap();

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int bpm = -1;
        try {
            Sequence sequence = MidiSystem.getSequence(manager.getResource(location).getInputStream());
            for (long i = 0; i < sequence.getTickLength(); i++) {
                map.put(i, Lists.newArrayList());
            }
            boolean foundTempo = false;
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if(message instanceof MetaMessage) {
                        MetaMessage mm = (MetaMessage) message;
                        if(mm.getType() == 0x51 && !foundTempo) { //Set Tempo

                            //Tempo is stored as a 3-byte big-endian integer
                            //Microseconds per minute is calculated as 6e7 / (tt tt tt)
                            byte[] data = mm.getData();
                            int tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
                            bpm = 60000000 / tempo;
                            foundTempo = true;
                        }
                    } else if(message instanceof ShortMessage) {
                        if(!foundTempo) {
                            throw new IllegalArgumentException("Track started before tempo was established");

                        }
                        ShortMessage sm = (ShortMessage) message;
                        if(sm.getCommand() == 0x90) { //ON
                            map.get(event.getTick()).add(new MidiTone(sm.getData1()));
                            min = Math.min(min, sm.getData1());
                            max = Math.max(max, sm.getData1());
                        }
                    }
                }
            }
        } catch (IOException | InvalidMidiDataException e) {
            e.printStackTrace();
        }
        if(bpm == -1) {
            throw new IllegalArgumentException("Unable to find bpm");
        }
        this.bpm = bpm;
        this.data = new MidiTone[map.size()][];
        int t = 0;
        for (Long key : map.keySet()) {
            List<MidiTone> lis = map.get(key);
            MidiTone[] ain = new MidiTone[lis.size()];
            for (int i = 0; i < lis.size(); i++) {
                MidiTone tone = lis.get(i);
                tone.setPosition((float)(tone.key - min) / (float)(max - min));
                ain[i] = tone;
            }
            this.data[t++] = ain;
        }

    }

    public MidiTone[] getNotesAt(int ticks) {
        int bpm = this.bpm / 3;
        int start = ticks * bpm;

        List<MidiTone> list = Lists.newArrayList();
        for (int i = 0; i < bpm; i++) {
            Collections.addAll(list, this.data[(start + i) % this.data.length]);
        }
        return list.toArray(new MidiTone[0]);
    }

    private static final Map<ResourceLocation, MidiStream> CACHE = Maps.newHashMap();

    public static MidiStream getMidi(ResourceLocation location) {
        return CACHE.computeIfAbsent(location, MidiStream::new);
    }

    public class MidiTone {
        private final int key;
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
}
