package com.skaliy.mobilecom.server.netty;

import com.skaliy.dbc.dbms.PostgreSQL;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Server implements Runnable {
    private final int port;
    protected static PostgreSQL db;

    public Server(int port, String url, String user, String password) {
        this.port = port;
        db = new PostgreSQL(url, user, password);
    }

    @Override
    public void run() {
        EventLoopGroup eventWork = new NioEventLoopGroup();
        EventLoopGroup eventBoss = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(eventBoss, eventWork)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ServerInitializer(this.port));
            bootstrap.bind(this.port).sync().channel().closeFuture().sync();
        } catch (InterruptedException ignored) {
        } finally {
            eventBoss.shutdownGracefully();
            eventWork.shutdownGracefully();
        }
    }

    public PostgreSQL getDb() {
        return db;
    }
}