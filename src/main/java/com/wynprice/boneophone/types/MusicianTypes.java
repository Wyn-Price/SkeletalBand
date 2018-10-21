package com.wynprice.boneophone.types;

import com.wynprice.boneophone.SkeletalBand;
import net.minecraftforge.fml.common.registry.GameRegistry;

@GameRegistry.ObjectHolder(SkeletalBand.MODID)
public class MusicianTypes {

    public static final MusicianTypeFactory CONDUCTOR = getNonNull();
    public static final MusicianTypeFactory BONEOPHONE = getNonNull();

    @SuppressWarnings("null")
    private static <T> T getNonNull() {
        return null;
    }

}
