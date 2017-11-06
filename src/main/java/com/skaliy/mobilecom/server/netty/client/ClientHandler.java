package com.skaliy.mobilecom.server.netty.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;

/**
 * Location: network
 * Created: IceSqueez
 * Date: 22.12.2013
 * Time: 23:26
 */

public class ClientHandler extends ChannelInboundMessageHandlerAdapter<String> {

    @Override
    public void messageReceived(ChannelHandlerContext channelHandlerContext, String message) throws Exception {
        System.out.println(message);
    }
}