package com.skaliy.mobilecom.server.netty.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;

/**
 * Location: service
 * Created: IceSqueez
 * Date: 24.12.2013
 * Time: 15:45
 */

public class ServerHandler extends ChannelInboundMessageHandlerAdapter<String> {
    private ChannelGroup channel = new DefaultChannelGroup();

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        channel.add(ctx.channel()); // Клиент пришел
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        channel.remove(ctx.channel());//Клиент ушел
    }

    @Override
    public void messageReceived(final ChannelHandlerContext channelHandlerContext, String message) throws Exception {
        System.out.println(message);
    }
}
