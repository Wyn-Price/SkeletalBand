package com.wynprice.boneophone.types;

import com.wynprice.boneophone.entity.MusicalSkeleton;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.function.BiFunction;

public final class MusicianTypeFactory extends IForgeRegistryEntry.Impl<MusicianTypeFactory> {

    private final BiFunction<MusicalSkeleton, MusicianTypeFactory, MusicianType> function;

    public MusicianTypeFactory(BiFunction<MusicalSkeleton, MusicianTypeFactory, MusicianType> function) {
        this.function = function;
    }

    public MusicianType createType(MusicalSkeleton entity) {
        return this.function.apply(entity, this);
    }
}
