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

public class MidiStream implements ISelectiveResourceReloadListener {

    private MidiTone[][] data = new MidiTone[0][0];
    private final ResourceLocation location;

    public MidiStream(ResourceLocation location) {
        this.location = new ResourceLocation(location.getResourceDomain(), "midis/" + location.getResourcePath() + ".mid");
        this.load(Minecraft.getMinecraft().getResourceManager());
        ((IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this::load); //Hmmmmmm
    }

    public void load(IResourceManager manager) {
        Map<Long, List<MidiTone>> map = Maps.newHashMap();
        try {
            Sequence sequence = MidiSystem.getSequence(manager.getResource(location).getInputStream());
            for (long i = 0; i < sequence.getTickLength(); i++) {
                map.put(i, Lists.newArrayList());
            }
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if(message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if(sm.getCommand() == 0x90) { //ON
                            map.get(event.getTick()).add(new MidiTone(sm.getData1()));
                        }
                    }
                }
            }
        } catch (IOException | InvalidMidiDataException e) {
            e.printStackTrace();
        }
        this.data = new MidiTone[map.size()][];
        int t = 0;
        for (Long key : map.keySet()) {
            List<MidiTone> lis = map.get(key);
            MidiTone[] ain = new MidiTone[lis.size()];
            for (int i = 0; i < lis.size(); i++) {
                ain[i] = lis.get(i);
            }
            this.data[t++] = ain;
        }

    }

    public MidiTone[] getNotesAt(int ticks, int bpm) {
        int start = ticks * bpm;

        List<MidiTone> list = Lists.newArrayList();
        for (int i = 0; i < bpm; i++) {
            Collections.addAll(list, this.data[(start + i) % this.data.length]);
        }
        return list.toArray(new MidiTone[0]);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {

    }

    public class MidiTone {
        private SoundEvent event;
        private int key;

        public MidiTone(int key) {
            this.event = SoundHandler.BONEOPHONE_OCTAVES[(key / 12)];
            this.key = key % 12;
        }

        public SoundEvent getEvent() {
            return event;
        }

        public int getKey() {
            return key;
        }
    }
}
