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
    private static PostgreSQL db;

    @FXML
    private static TextArea textAreaLogs;

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

        String _query = query, parameter = "";
        int index = 0;

        if (_query.startsWith("get_tariff_")
                || _query.startsWith("get_phone_")) {
            index = Integer.parseInt(_query.substring(_query.lastIndexOf("_") + 1));
            _query = _query.substring(0, _query.lastIndexOf("_"));
        }

        if (_query.startsWith("get_id_employee_")) {
            parameter = _query.substring(_query.lastIndexOf("_") + 1);
            _query = _query.substring(0, _query.lastIndexOf("_"));
        }

        switch (_query) {

            case "get_news":
                result = db.query(true, "SELECT * FROM news ORDER BY id_news");
                break;

            case "get_tariffs_count":
                result = db.query(true, "SELECT COUNT(*) FROM tariffs");
                break;

            case "get_tariff":
                result = db.query(true,
                        "SELECT id_tariff, title, price, description, " +
                                "array(" +
                                "   SELECT o.title " +
                                "   FROM offers o, tariffs t " +
                                "   WHERE t.id_tariff = " + index +
                                "   AND o.id_offer = ANY (t.ids_offer) " +
                                ") :: TEXT " +
                                "FROM tariffs " +
                                "WHERE id_tariff = " + index +
                                "ORDER BY id_tariff;");
                break;

            case "get_tariffs":
                result = db.query(true, "SELECT * FROM tariffs ORDER BY id_tariff");
                break;

            case "get_offers":
                result = db.query(true, "SELECT * FROM offers ORDER BY id_offer");
                break;

            case "get_phones_count":
                result = db.query(true, "SELECT COUNT(*) FROM phones");
                break;

            case "get_phones":
                result = db.query(true,
                        "SELECT p.id_phone, m.name, d.*, p.color, p.price, p.units " +
                                "FROM phones p, manufacturers m, phone_details d " +
                                "WHERE p.id_manufacturer = m.id_manufacturer " +
                                "AND p.id_phone_detail = d.id_phone_detail " +
                                "ORDER BY p.id_phone");
                break;

            case "get_employees_name":
                result = db.query(true,
                        "SELECT CONCAT(last_name, ' ', first_name, ' ', patr_name) " +
                                "FROM employees " +
                                "ORDER BY id_employee");
                break;

            case "get_id_employee":
                result = db.query(true,
                        "SELECT id_employee " +
                                "FROM employees " +
                                "WHERE CONCAT(last_name, ' ', first_name, ' ', patr_name) = '" + parameter + "'");
                break;

            case "get_last_sale":
                result = db.query(true, "SELECT MAX(id_sale) FROM sales");
                break;

            case "get_last_sale_detail":
                result = db.query(true, "SELECT MAX(id_sale_detail) FROM sale_details");
                break;

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

            case "add_tariff":
                try {
                    db.query(false,
                            "INSERT INTO tariffs(title, price, description) " +
                                    "VALUES ('" + values[0] + "', " + values[1] + ", '" + values[2] + "')");
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "add_sale":
                try {
                    db.query(false,
                            "INSERT INTO sales(date_sale, amount,"
                                    + (values[2].startsWith("id") ? " id_employee," : "") + " is_sold, "
                                    + "client_name, client_pnumber, client_email) " +
                                    "VALUES('" + values[0] + "', " + values[1] + ", "
                                    + (values[2].startsWith("id")
                                    ? (values[2].substring(2) + ", " + values[3] + ", '"
                                    + values[4] + "', '" + values[5] + "', '" + values[6] + "'")
                                    : (values[2] + ", '" + values[3] + "', '" + values[4] + "', '" + values[5] + "'")));
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "add_sale_detail":
                try {
                    db.query(false, "INSERT INTO sale_details(id_sale) VALUES (" + values[0] + ")");
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "add_union_phone":
                try {
                    db.query(false, "INSERT INTO union_sale_details_phones " +
                            "VALUES (" + values[0] + ", " + values[1] + ", " + values[2] + ")");
                } catch (SQLException e) {
                    result = false;
                }
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