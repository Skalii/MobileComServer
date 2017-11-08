package com.skaliy.mobilecom.server.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;

import java.util.Arrays;

public class ServerHandler extends ChannelInboundMessageHandlerAdapter<String> {

    private static final ChannelGroup channels = new DefaultChannelGroup();

    @Override
    public void handlerAdded(ChannelHandlerContext channelHandlerContext) throws Exception {
        Channel incoming = channelHandlerContext.channel();

        System.out.println("[CLIENT] - " + incoming.remoteAddress() + " has joined!\n");

        channels.add(channelHandlerContext.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext channelHandlerContext) throws Exception {
        Channel incoming = channelHandlerContext.channel();

        System.out.println("[CLIENT] - " + incoming.remoteAddress() + " has left!\n");

        channels.remove(channelHandlerContext.channel());
    }

    @Override
    public void messageReceived(final ChannelHandlerContext channelHandlerContext, String message) throws Exception {
        Channel incoming = channelHandlerContext.channel();

        System.out.println("[CLIENT] - " + incoming.remoteAddress() + " query: " + message + "" +
                "\n[CLIENT] - " + incoming.remoteAddress() + " result: ");

        String[][] result = Server.db.queryResult(message);

        for (Channel channel : channels) {
            if (channel == incoming) {
                channel.write("[SERVER] result: \n");
                for (String[] aResult : result) {
                    channel.write(Arrays.toString(aResult) + "\n");

                    System.out.println(Arrays.toString(aResult));
                }
            }
        }
        System.out.println();
    }
}
