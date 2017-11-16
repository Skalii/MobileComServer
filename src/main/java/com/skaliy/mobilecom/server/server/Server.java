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
        String[] parameters = new String[0];
        int index = 0;

        if (_query.startsWith("get_id_employee_p-")
                || _query.startsWith("get_phone_p-")
                || _query.startsWith("get_phone_c-")
                || _query.startsWith("get_tariff_p-")
                || _query.startsWith("get_offer_p-")) {
            parameter = _query.substring(_query.lastIndexOf("_p-") + 3);
            _query = _query.substring(0, _query.lastIndexOf("_p-") + 2);

        } else if (_query.startsWith("get_tariff_i-")) {
            index = Integer.parseInt(_query.substring(_query.lastIndexOf("_i-") + 3));
            _query = _query.substring(0, _query.lastIndexOf("_i-") + 2);

        } else if (_query.startsWith("get_phone_c-")) {
            parameters = _query.substring(_query.lastIndexOf("_c-") + 3).split(";");
            _query = _query.substring(0, _query.lastIndexOf("_c-") + 2);
        }

        switch (_query) {

            case "get_news":
                result = db.query(true, "SELECT * FROM news ORDER BY id_news");
                break;

            case "get_offers":
                result = db.query(true, "SELECT * FROM offers ORDER BY id_offer");
                break;

            case "get_phones":
                result = db.query(true,
                        "SELECT p.id_phone, m.name, d.*, p.color, p.price, p.units " +
                                "FROM phones p, manufacturers m, phone_details d " +
                                "WHERE p.id_manufacturer = m.id_manufacturer " +
                                "AND p.id_phone_detail = d.id_phone_detail " +
                                "ORDER BY p.id_phone");
                break;

            case "get_tariff_i":
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

            case "get_tariffs_count":
                result = db.query(true, "SELECT COUNT(*) FROM tariffs");
                break;

            case "get_phones_count":
                result = db.query(true, "SELECT COUNT(*) FROM phones");
                break;

            case "get_last_sale":
                result = db.query(true, "SELECT MAX(id_sale) FROM sales");
                break;

            case "get_id_employee_p":
                result = db.query(true,
                        "SELECT id_employee FROM employees WHERE name = '" + parameter + "'");
                break;

            case "get_phone_p":
                result = db.query(true,
                        "SELECT p.id_phone, m.name, d.*, p.color, p.price, p.units " +
                                "FROM phones p, manufacturers m, phone_details d " +
                                "WHERE p.id_manufacturer = m.id_manufacturer " +
                                "AND p.id_phone_detail = d.id_phone_detail " +
                                "AND (LOWER(m.name) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(p.color) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(d.model) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(d.os) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(d.processor) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(d.resolution) LIKE LOWER('%" + parameter + "%') " +/*
                                (parameter.matches("^\\d*$") || parameter.contains(".")
                                        ? "OR p.price = " + parameter +
                                        "OR d.ram = " + parameter +
                                        "OR d.rom = " + parameter +
                                        "OR d.batary = " + parameter +
                                        "OR d.diagonal = " + parameter +
                                        "OR d.camera_main = " + parameter +
                                        "OR d.camera_main_two = " + parameter +
                                        "OR d.camera_front = " + parameter
                                        : "")
                                + */") ORDER BY p.id_phone");
                break;

            case "get_tariff_p":
                result = db.query(true,
                        "SELECT id_tariff, title, price, description, " +
                                "array(" +
                                "   SELECT o.title " +
                                "   FROM offers o, tariffs t " +
                                "   WHERE (LOWER(t.title) LIKE LOWER('%" + parameter + "%') " +
                                "   OR LOWER(t.description) LIKE LOWER('%" + parameter + "%'))" +
                                "   AND o.id_offer = ANY (t.ids_offer) " +
                                ") :: TEXT " +
                                "FROM tariffs " +
                                "WHERE LOWER(title) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(description) LIKE LOWER('%" + parameter + "%') " +
                                "ORDER BY id_tariff;");
                break;

            case "get_offer_p":
                result = db.query(true,
                        "SELECT * FROM offers " +
                                "WHERE LOWER(title) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(description) LIKE LOWER('%" + parameter + "%')");
                break;

            case "get_phone_c":

                String _query_ = "SELECT p.id_phone, m.name, d.*, p.color, p.price, p.units " +
                        "FROM phones p, manufacturers m, phone_details d " +
                        "WHERE p.id_manufacturer = m.id_manufacturer " +
                        "AND p.id_phone_detail = d.id_phone_detail";

                _query_ = _query_.concat(" AND (");

                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].startsWith("Производитель")) {
                        parameters[i] = parameters[i].replace("Производитель", "m.name");
                        // TODO: 16.11.2017 ADDS ALL PARAMETERS
                    }
                }

                _query_ = _query_.concat(")");

                result = db.query(true, _query_);
                break;

            case "get_employees_names":
                result = db.query(true, "SELECT name FROM employees ORDER BY id_employee");
                break;

            case "get_manufacturers_names":
                result = db.query(true, "SELECT name FROM manufacturers ORDER BY id_manufacturer");
                break;

            case "get_phones_colors":
                result = db.query(true, "SELECT color FROM phones ORDER BY id_phone");
                break;

            case "get_pd_os":
                result = db.query(true, "SELECT os FROM phone_details ORDER BY id_phone_detail");
                break;

            case "get_pd_ram":
                result = db.query(true, "SELECT ram FROM phone_details ORDER BY id_phone_detail");
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
                                    + values[4] + "', '" + values[5] + "', '" + values[6] + "')")
                                    : (values[2] + ", '" + values[3] + "', '" + values[4] + "', '" + values[5] + "')")));
                } catch (SQLException e) {
                    result = false;
                    e.printStackTrace();
                }
                break;

            case "add_union_phone":
                try {
                    db.query(false, "INSERT INTO union_sales_phones " +
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