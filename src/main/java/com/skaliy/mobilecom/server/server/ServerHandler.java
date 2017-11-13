package com.skaliy.mobilecom.server.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;

import java.sql.SQLException;
import java.util.Arrays;

public class ServerHandler extends ChannelInboundMessageHandlerAdapter<String> {

    private static final ChannelGroup channels = new DefaultChannelGroup();

    @Override
    public void handlerAdded(ChannelHandlerContext channelHandlerContext) throws Exception {
        Channel incoming = channelHandlerContext.channel();
        Server.addLog("[CLIENT] - " + incoming.remoteAddress() + " | has joined!");
        channels.add(channelHandlerContext.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext channelHandlerContext) throws Exception {
        Channel incoming = channelHandlerContext.channel();
        Server.addLog("[CLIENT] - " + incoming.remoteAddress() + " | has left!");
        channels.remove(channelHandlerContext.channel());
    }

    @Override
    public void messageReceived(final ChannelHandlerContext channelHandlerContext, String message) {
        Channel incoming = channelHandlerContext.channel();

        message = message.replaceAll("_p_", "\n");

        if (message.startsWith("false") || message.startsWith("true")) {
//            String bool = message.substring(0, message.indexOf(":")),
            message = message.substring(message.indexOf(":") + 1);

            Server.addLog("[CLIENT] - " + incoming.remoteAddress() + " | query: " + message);

            String[] values = message.substring(message.indexOf(",") + 1).split(",");

            for (int i = 0; i < values.length; i++) {
                values[i] = values[i].replaceAll("_c_", ", ");
            }

            boolean queryResult = Server.setResult(message, values);

            for (Channel channel : channels) {
                if (channel == incoming) {
                    Server.addLog("[CLIENT] - " + incoming.remoteAddress() + " | query state: " + queryResult);
                    channel.write("[SERVER] - query state: " + "\r\n");
                    channel.write("[" + queryResult + "]" + "\r\n");
                }
            }

        } else {

            String[][] quertResult;
            String queryState = "true";

            Server.addLog("[CLIENT] - " + incoming.remoteAddress() + " | query: " + message);

            try {
                quertResult = Server.getResult(message);
            } catch (SQLException e) {
                quertResult = new String[][]{{null}};
                queryState = "false";
            }

            for (Channel channel : channels) {
                if (channel == incoming) {
                    Server.addLog("[CLIENT] - " + incoming.remoteAddress() + " | query state: " + queryState);
                    Server.addLog("[CLIENT] - " + incoming.remoteAddress() + " | result size: " + quertResult.length);
                    channel.write("[SERVER] - accepted the query: " + message + "\r\n");
                    channel.write("[SERVER] - result size: " + quertResult.length + "\r\n");

                    for (String[] record : quertResult) {
                        Server.addLog(Arrays.toString(record));

                        for (int i = 0; i < record.length; i++) {
                            record[i] = record[i]
                                    .replaceAll("\n", "_p_")
                                    .replaceAll(", ", "_c_")
                                    .replace("{", "_bo_")
                                    .replace("}", "_bc_");
                        }

                        channel.write(Arrays.toString(record) + "\r\n");
                    }
                }
            }
        }
    }

}