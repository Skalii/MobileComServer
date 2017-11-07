package com.skaliy.mobilecom.server.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;

import java.util.Arrays;

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
        System.out.println("Query from client: " + message + "\nResult: ");

        String[][] result = Server.db.queryResult(message);

        for (String[] aResult : result) {
            System.out.println(Arrays.toString(aResult));
        }
    }
}
