package com.wynprice.boneophone.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

//Copied from one of my projects -> https://github.com/Dumb-Code/ProjectNublar/blob/master/src/main/java/net/dumbcode/projectnublar/server/network/WorldModificationsMessageHandler.java
public abstract class WorldModificationsMessageHandler<REQ extends IMessage, REP extends IMessage> implements IMessageHandler<REQ, REP> {
    @Override
    public REP onMessage(REQ message, MessageContext ctx) {
        EntityPlayer player = ctx.side == Side.SERVER ? ctx.getServerHandler().player : FMLClientHandler.instance().getClientPlayerEntity();
        World world = player.world;

        FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handleMessage(message, ctx, world, player));
        return answer(message, ctx, world, player);
    }

    protected abstract void handleMessage(REQ message, MessageContext ctx, World world, EntityPlayer player);
    protected REP answer(REQ message, MessageContext ctx, World world, EntityPlayer player) {
        return null;
    }
}