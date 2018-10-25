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

    @SubscribeEvent
    public static void onSoundRegistry(RegistryEvent.Register<SoundEvent> event) {
        for (int i = 0; i < 11; i++) {
            String name = String.valueOf(i);
            if(name.length() == 1) {
                name = "0" + name;
            }
            ResourceLocation bone = new ResourceLocation(SkeletalBand.MODID, "boneophone" + name);
            SoundEvent soundEvent = new SoundEvent(bone).setRegistryName(bone);
            event.getRegistry().register(soundEvent);
            BONEOPHONE_OCTAVES[i] = soundEvent;

            ResourceLocation bass = new ResourceLocation(SkeletalBand.MODID, "bass" + name);
            soundEvent = new SoundEvent(bass).setRegistryName(bass);
            event.getRegistry().register(soundEvent);
            BASS_OCTAVES[i] = soundEvent;
        }
    }

}
