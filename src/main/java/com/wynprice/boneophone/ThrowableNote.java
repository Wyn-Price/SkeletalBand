package com.wynprice.boneophone;

import com.wynprice.boneophone.entity.ThrowableNoteEntity;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

public class ThrowableNote extends Item {

    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (!playerIn.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        worldIn.playSound(null, playerIn.posX, playerIn.posY, playerIn.posZ, SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));

        NBTTagCompound nbt = stack.getOrCreateSubCompound(SkeletalBand.MODID);

        if(!worldIn.isRemote) {
            ThrowableNoteEntity throwableNote = new ThrowableNoteEntity(worldIn, playerIn, nbt.getInteger("Note"), nbt.getInteger("Instrument"));
            throwableNote.shoot(playerIn, playerIn.rotationPitch, playerIn.rotationYaw, 0.0F, 1.5F, 1.0F);
            worldIn.spawnEntity(throwableNote);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public boolean getHasSubtypes() {
        return true;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if(this.isInCreativeTab(tab)) {
            for (int instrument = 0; instrument < ThrowableNoteEntity.INSTRUMENTS.size(); instrument++) {
                for (int note = 0; note < 25; note++) {
                    items.add(fromNote(note, instrument));
                }
            }
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        NBTTagCompound nbt = stack.getOrCreateSubCompound(SkeletalBand.MODID);
        String instrument = I18n.translateToLocal(SkeletalBand.MODID + ".instrument." + nbt.getInteger("Instrument"));
        return instrument + " " + nbt.getInteger("Note");
    }

    public static ItemStack fromNote(int note, int instrument) {
        ItemStack stack = new ItemStack(SkeletalBand.THROWABLE_NOTE);
        NBTTagCompound nbt = stack.getOrCreateSubCompound(SkeletalBand.MODID);
        nbt.setInteger("Note", note);
        nbt.setInteger("Instrument", instrument);
        return stack;
    }

}
