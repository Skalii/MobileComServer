package com.skaliy.mobilecom.server.server;

import com.skaliy.dbc.dbms.PostgreSQL;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.codehaus.plexus.util.StringUtils;

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
                || _query.startsWith("get_tariff_p-")
                || _query.startsWith("get_offer_p-")) {
            parameter = _query.substring(_query.lastIndexOf("_p-") + 3);
            _query = _query.substring(0, _query.lastIndexOf("_p-") + 2);

        } else if (_query.startsWith("get_tariff_i-")) {
            index = Integer.parseInt(_query.substring(_query.lastIndexOf("_i-") + 3));
            _query = _query.substring(0, _query.lastIndexOf("_i-") + 2);

        } else if (_query.startsWith("get_phone_c-")
                || _query.startsWith("get_tariff_c-")
                || _query.startsWith("get_offer_c-")) {
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
                                "ORDER BY id_tariff");
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

            case "get_offer_c":

                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].startsWith("Название услуги")) {
                        parameters[i] = parameters[i].replace("Название услуги", "title");
                    }
                }

                result = db.query(true,
                        "SELECT * FROM offers " +
                                "WHERE " + StringUtils.join(parameters, " AND ") +
                                " ORDER BY id_offer");
                break;

            case "get_tariff_c":

                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].startsWith("Название тарифа")) {
                        parameters[i] = parameters[i].replace("Название тарифа", "t.title");
                    }
                }

                result = db.query(true,
                        "SELECT t.id_tariff, t.title, t.price, t.description, " +
                                "array(" +
                                "   SELECT o.title " +
                                "   FROM offers o, tariffs t " +
                                "   WHERE " + StringUtils.join(parameters, " AND ") +
                                "   AND o.id_offer = ANY (t.ids_offer) " +
                                ") :: TEXT " +
                                "FROM tariffs t " +
                                "WHERE " + StringUtils.join(parameters, " AND ") +
                                "ORDER BY t.id_tariff");
                break;

            case "get_phone_c":

                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].startsWith("Производитель")) {
                        parameters[i] = parameters[i].replace("Производитель", "m.name");
                    } else if (parameters[i].startsWith("Цвет")) {
                        parameters[i] = parameters[i].replace("Цвет", "p.color");
                    } else if (parameters[i].startsWith("ОС")) {
                        parameters[i] = parameters[i].replace("ОС", "d.os");
                    } else if (parameters[i].startsWith("Оперативная память")) {
                        parameters[i] = parameters[i].replace("Оперативная память", "d.ram");
                    } else if (parameters[i].startsWith("Встроенная память")) {
                        parameters[i] = parameters[i].replace("Встроенная память", "d.rom");
                    } else if (parameters[i].startsWith("Карта памяти")) {
                        parameters[i] = parameters[i]
                                .replace("Карта памяти", "d.memory_card")
                                .replace("Поддерживает", "t")
                                .replace("Не поддерживает", "f");
                    } else if (parameters[i].startsWith("Количество SIM-карт")) {
                        parameters[i] = parameters[i].replace("Количество SIM-карт", "d.simcard_quant");
                    } else if (parameters[i].startsWith("Процессор")) {
                        parameters[i] = parameters[i].replace("Процессор", "d.processor");
                    } else if (parameters[i].startsWith("Батарея")) {
                        parameters[i] = parameters[i].replace("Батарея", "d.batary");
                    } else if (parameters[i].startsWith("Диагональ")) {
                        parameters[i] = parameters[i].replace("Диагональ", "d.diagonal");
                    } else if (parameters[i].startsWith("Разрешение")) {
                        parameters[i] = parameters[i].replace("Разрешение", "d.resolution");
                    } else if (parameters[i].startsWith("Основная камера")) {
                        parameters[i] = parameters[i].replace("Основная камера", "d.camera_main");
                    } else if (parameters[i].startsWith("Фронтальная камера")) {
                        parameters[i] = parameters[i].replace("Фронтальная камера", "d.camera_front");
                    }
                }

                result = db.query(true,
                        "SELECT p.id_phone, m.name, d.*, p.color, p.price, p.units " +
                                "FROM phones p, manufacturers m, phone_details d " +
                                "WHERE p.id_manufacturer = m.id_manufacturer " +
                                "AND p.id_phone_detail = d.id_phone_detail " +
                                "AND (" + StringUtils.join(parameters, " AND ") +
                                ") ORDER BY p.id_phone");
                break;

            case "get_e_names":
                result = db.query(true, "SELECT name FROM employees ORDER BY name");
                break;

            case "get_m_names":
                result = db.query(true, "SELECT name FROM manufacturers ORDER BY name");
                break;

            case "get_phones_colors":
                result = db.query(true, "SELECT DISTINCT color FROM phones ORDER BY color");
                break;

            case "get_pd_os":
                result = db.query(true, "SELECT DISTINCT os FROM phone_details ORDER BY os");
                break;

            case "get_pd_ram":
                result = db.query(true, "SELECT DISTINCT ram FROM phone_details ORDER BY ram");
                break;

            case "get_pd_rom":
                result = db.query(true, "SELECT DISTINCT rom FROM phone_details ORDER BY rom");
                break;

            case "get_pd_memory_card":
                result = db.query(true, "SELECT DISTINCT memory_card FROM phone_details ORDER BY memory_card");
                for (int i = 0; i < result.length; i++) {
                    result[i][0] = result[i][0]
                            .replace("t", "Поддерживает")
                            .replace("f", "Не поддерживает");
                }
                break;

            case "get_pd_sim":
                result = db.query(true, "SELECT DISTINCT simcard_quant FROM phone_details ORDER BY simcard_quant");
                break;

            case "get_pd_processor":
                result = db.query(true, "SELECT DISTINCT processor FROM phone_details ORDER BY processor");
                break;

            case "get_pd_batary":
                result = db.query(true, "SELECT DISTINCT batary FROM phone_details ORDER BY batary");
                break;

            case "get_pd_diagonal":
                result = db.query(true, "SELECT DISTINCT diagonal FROM phone_details ORDER BY diagonal");

                for (int i = 0; i < result.length; i++) {
                    result[i][0] = String.valueOf(Float.parseFloat(result[i][0]));
                }
                break;

            case "get_pd_resolution":
                result = db.query(true, "SELECT DISTINCT resolution FROM phone_details ORDER BY resolution");
                break;

            case "get_pd_camera_main":
                result = db.query(true, "SELECT DISTINCT camera_main FROM phone_details ORDER BY camera_main");

                for (int i = 0; i < result.length; i++) {
                    result[i][0] = String.valueOf(Float.parseFloat(result[i][0]));
                }
                break;

            case "get_pd_camera_front":
                result = db.query(true, "SELECT DISTINCT camera_front FROM phone_details ORDER BY camera_front");

                for (int i = 0; i < result.length; i++) {
                    result[i][0] = String.valueOf(Float.parseFloat(result[i][0]));
                }
                break;

            case "get_tariffs_title":
                result = db.query(true, "SELECT DISTINCT title FROM tariffs ORDER BY title");
                break;

            case "get_offers_title":
                result = db.query(true, "SELECT DISTINCT title FROM offers ORDER BY title");
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