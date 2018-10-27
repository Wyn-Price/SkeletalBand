package com.wynprice.boneophone;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = SkeletalBand.MODID)
public class SoundHandler {

    public static SoundEvent[] BONEOPHONE_OCTAVES = new SoundEvent[11];
    public static SoundEvent[] BASS_OCTAVES = new SoundEvent[11];

    public static final int DRUM_OFFSET = 27;
    public static SoundEvent[] DRUM_NOTES = new SoundEvent[60];


    @SubscribeEvent
    public static void onSoundRegistry(RegistryEvent.Register<SoundEvent> event) {

        registerSounds("boneophone", 0, BONEOPHONE_OCTAVES, event);
        registerSounds("bass", 0, BASS_OCTAVES, event);

        registerSounds("drum", DRUM_OFFSET, DRUM_NOTES, event);
    }

    private static void registerSounds(String name, int offset, SoundEvent[] array, RegistryEvent.Register<SoundEvent> event) {
        for (int i = 0; i < array.length; i++) {
            String num = String.valueOf(i + offset);
            if(num.length() == 1) {
                num = "0" + num;
            }
            ResourceLocation res = new ResourceLocation(SkeletalBand.MODID, name + num);
            SoundEvent soundEvent = new SoundEvent(res).setRegistryName(res);
            event.getRegistry().register(soundEvent);
            array[i] = soundEvent;
        }
    }

}
