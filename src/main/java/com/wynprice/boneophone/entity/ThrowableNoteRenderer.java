package com.wynprice.boneophone.entity;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.ThrowableNote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.item.ItemStack;

public class ThrowableNoteRenderer extends RenderSnowball<ThrowableNoteEntity> {
    public ThrowableNoteRenderer(RenderManager renderManagerIn) {
        super(renderManagerIn, SkeletalBand.THROWABLE_NOTE, Minecraft.getMinecraft().getRenderItem());
    }

    @Override
    public ItemStack getStackToRender(ThrowableNoteEntity entityIn) {
        return ThrowableNote.fromNote(entityIn.note, entityIn.instrument);
    }
}
