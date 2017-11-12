package com.skaliy.mobilecom.server.server;

import com.skaliy.dbc.dbms.PostgreSQL;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.sql.SQLException;

public class Server implements Runnable {

    private final int port;
    static PostgreSQL db;

    @FXML
    static TextArea textAreaLogs;

    public Server(int port, String url, String user, String password) throws SQLException, ClassNotFoundException {
        this.port = port;
        db = new PostgreSQL(url, user, password);
    }

    @Override
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(workerGroup, bossGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ServerInitializer(port));

            bootstrap.bind(port).sync().channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    static String[][] getResult(String query) throws SQLException {
        String[][] result = new String[0][];

        String _query = query;
        int index = 0;

        if (_query.startsWith("get_news_")
//                || _query.startsWith("get_list_connects_")
//                || _query.startsWith("get_list_from_section_")
//                || _query.startsWith("get_category_from_")
//                || _query.startsWith("get_bookmarks_from_")
                /*|| _query.startsWith("get_bookmarks_ids_from_")*/) {
            index = Integer.parseInt(_query.substring(_query.lastIndexOf("_") + 1));
            _query = _query.substring(0, _query.lastIndexOf("_"));
        }

        switch (_query) {

            case "get_news":
                result = db.query(true, "SELECT * FROM news ORDER BY id_news");
                System.out.println(result[1][2]);
                break;

            case "get_tariffs":
                result = db.query(true,
                        "SELECT * FROM tariffs ORDER BY id_tariff");
                break;

            /*case "get_count_links":
                result = db.query(true, "SELECT count(link) FROM sites");
                break;*/

            /*case "get_list_connects":
                result = db.query(true,
                        "SELECT si.* " +
                                "FROM sites si, sections se " +
                                "WHERE se.id_section = " + index +
                                " AND si.id_category = ANY(se.ids_category)");
                break;*/

            /*case "get_list_from_section":
                result = db.query(true,
                        "SELECT si.* " +
                                "FROM sites si, sections se " +
                                "WHERE se.id_section = " + index +
                                " AND si.id_category = ANY(se.ids_category)");
                break;*/

            /*case "get_category_from":
                result = db.query(true,
                        "SELECT title " +
                                "FROM categories " +
                                "WHERE id_category = " + index);
                break;*/

           /* case "get_logins_emails":
                result = db.query(true, "SELECT login, email FROM profiles");
                break;*/

            /*case "get_profiles":
                result = db.query(true, "SELECT * FROM profiles");
                break;*/

            /*case "get_bookmarks_from":
                result = db.query(true,
                        "SELECT s.link " +
                                "FROM sites s, profiles p " +
                                "WHERE p.id_profile = " + index +
                                " AND s.id_site = ANY(p.ids_bookmark)");
                break;*/

            /*case "get_bookmarks_ids_from":
                result = db.query(true,
                        "SELECT ids_bookmark " +
                                "FROM profiles " +
                                "WHERE id_profile = " + index);
                break;*/
        }

        return result;
    }

    static boolean setResult(String query, String... values) {

        boolean result = true;

        String _query = query.substring(0, query.indexOf(","));

        switch (_query) {

            case "add_news":
                try {
                    db.query(false,
                            "INSERT INTO news(title, content) " +
                                    "VALUES('" + values[0] + "', '" + values[1] + "')");
                } catch (SQLException e) {
                    result = false;
                }
                break;

            /*case "add_bookmark":
                try {
                    db.query(false,
                            "INSERT INTO bookmarks VALUES (" +
                                    values[0] + ", " +
                                    "(SELECT id_site " +
                                    "FROM sites " +
                                    "WHERE link = '" + values[1] + "')" + ")");
                } catch (SQLException e) {
                    result = false;
                }
                break;*/

            /*case "delete_bookmark":
                try {
                    db.query(false,
                            "DELETE FROM bookmarks " +
                                    "WHERE id_profile = " + values[0] +
                                    " AND id_site = (SELECT id_site " +
                                    "FROM sites " +
                                    "WHERE link = '" + values[1] + "')");
                } catch (SQLException e) {
                    result = false;
                }
                break;*/
        }

        return result;
    }

    public PostgreSQL getDb() {
        return db;
    }

    public void setTextAreaLogs(TextArea textAreaLogs) {
        Server.textAreaLogs = textAreaLogs;
    }

    static void addLog(String log) {
        Server.textAreaLogs.appendText(log + "\n");
    }

}