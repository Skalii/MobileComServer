package com.skaliy.mobilecom.server.server;

import com.skaliy.mobilecom.server.connection.dbms.PostgreSQL;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.codehaus.plexus.util.StringUtils;

import java.sql.SQLException;

import java.util.Objects;

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

        if (_query.startsWith("get_id_employee_p-")
                || _query.startsWith("get_phone_p-")
                || _query.startsWith("get_tariff_p-")
                || _query.startsWith("get_offer_p-")
                || _query.startsWith("get_email_employee_p-")) {
            parameter = _query.substring(_query.lastIndexOf("_p-") + 3);
            _query = _query.substring(0, _query.lastIndexOf("_p-") + 2);

        } else if (_query.startsWith("get_phone_c-")
                || _query.startsWith("get_tariff_c-")
                || _query.startsWith("get_offer_c-")
                || _query.startsWith("get_pass_employee_c-")) {
            parameters = _query.substring(_query.lastIndexOf("_c-") + 3).split(";");
            _query = _query.substring(0, _query.lastIndexOf("_c-") + 2);
        }

        switch (_query) {

            case "get_news":
                result = db.query(true, "SELECT * FROM news ORDER BY id_news");
                break;

            case "get_employees":
                result = db.query(true,
                        "SELECT id_employee, name, phone_number, email, address, hiring, position, salary " +
                                "FROM employees ORDER BY id_employee");

                for (int i = 0; i < result.length; i++) {
                    result[i][7] = String.valueOf(Float.parseFloat(
                            result[i][7]
                                    .substring(0, result[i][7].length() - 1)
                                    .replace(",", ".")
                                    .replace(" ", "")));
                }
                break;

            case "get_offers":
                result = db.query(true, "SELECT * FROM offers ORDER BY id_offer");
                for (int i = 0; i < result.length; i++) {
                    result[i][2] = String.valueOf(Float.parseFloat(
                            result[i][2]
                                    .substring(0, result[i][2].length() - 1)
                                    .replace(",", ".")
                                    .replace(" ", "")));
                }
                break;

            case "get_manufacturers":
                result = db.query(true, "SELECT * FROM manufacturers ORDER BY id_manufacturer");
                break;

            case "get_phones":
                result = db.query(true,
                        "SELECT p.id_phone, m.name, d.model, d.os, d.ram, d.rom, d.memory_card, " +
                                "d.simcard_quant, d.processor, d.batary, d.diagonal, d.resolution, " +
                                "d.camera_main, d.camera_main_two, d.camera_front, p.color, p.price, p.units " +
                                "FROM phones p, manufacturers m, phone_details d " +
                                "WHERE p.id_manufacturer = m.id_manufacturer " +
                                "AND p.id_phone_detail = d.id_phone_detail " +
                                "ORDER BY p.id_phone");
                for (int i = 0; i < result.length; i++) {
                    result[i][6] = result[i][6]
                            .replace("t", "Поддерживает")
                            .replace("f", "Не поддерживает");
                    result[i][10] = String.valueOf(Float.parseFloat(result[i][10]));
                    result[i][12] = String.valueOf(Float.parseFloat(result[i][12]));
                    try {
                        result[i][13] = String.valueOf(Float.parseFloat(result[i][13]));
                    } catch (NullPointerException e) {
                        result[i][13] = "Нет";
                    }
                    result[i][14] = String.valueOf(Float.parseFloat(result[i][14]));
                    result[i][16] = String.valueOf(Float.parseFloat(
                            result[i][16]
                                    .substring(0, result[i][16].length() - 1)
                                    .replace(",", ".")
                                    .replace(" ", "")));
                }
                break;

            case "get_sales":
                String[][] isNull = db.query(true,
                        "SELECT id_sale FROM sales WHERE id_employee IS NULL ORDER BY id_sale"),
                        isNotNull = db.query(true,
                                "SELECT s.id_sale " +
                                        "FROM sales s, employees e " +
                                        "WHERE s.id_employee = e.id_employee " +
                                        "ORDER BY id_sale");

                result = new String[isNull.length + isNotNull.length][12];

                for (int i = 0, j = 0; i < result.length; i++) {
                    if (i < isNull.length) {
                        result[i][0] = isNull[i][0];
                        result[i] = db.query(true,
                                "SELECT id_sale, date_sale, amount, id_employee, is_sold, " +
                                        "client_name, client_pnumber, client_email, " +
                                        "array(" +
                                        "   SELECT concat(m.name, ' ', pd.model) " +
                                        "   FROM phones p, phone_details pd, manufacturers m, sales s " +
                                        "   WHERE p.id_phone_detail = pd.id_phone_detail " +
                                        "   AND p.id_manufacturer = m.id_manufacturer " +
                                        "   AND s.id_sale = " + result[i][0] +
                                        "   AND p.id_phone = ANY (s.ids_phones)) :: TEXT, " +
                                        "units_phones, " +
                                        "array(" +
                                        "   SELECT t.title " +
                                        "   FROM tariffs t, sales s " +
                                        "   WHERE s.id_sale = " + result[i][0] +
                                        "   AND t.id_tariff = ANY (s.ids_tariffs)) :: TEXT, " +
                                        "units_tariffs " +
                                        "FROM sales " +
                                        "WHERE id_employee IS NULL " +
                                        "AND id_sale = " + result[i][0])[0];
                    } else {
                        result[i][0] = isNotNull[j++][0];
                        result[i] = db.query(true,
                                "SELECT s.id_sale, s.date_sale, s.amount, e.name, s.is_sold, " +
                                        "s.client_name, s.client_pnumber, s.client_email, " +
                                        "array(" +
                                        "   SELECT concat(m.name, ' ', pd.model) " +
                                        "   FROM phones p, phone_details pd, manufacturers m, sales s " +
                                        "   WHERE p.id_phone_detail = pd.id_phone_detail " +
                                        "   AND p.id_manufacturer = m.id_manufacturer " +
                                        "   AND s.id_sale = " + result[i][0] +
                                        "   AND p.id_phone = ANY (s.ids_phones)) :: TEXT, " +
                                        "s.units_phones, " +
                                        "array(" +
                                        "   SELECT t.title " +
                                        "   FROM tariffs t, sales s " +
                                        "   WHERE s.id_sale = " + result[i][0] +
                                        "   AND t.id_tariff = ANY (s.ids_tariffs)) :: TEXT, " +
                                        "s.units_tariffs " +
                                        "FROM sales s, employees e " +
                                        "WHERE s.id_employee = e.id_employee " +
                                        "AND s.id_sale = " + result[i][0])[0];
                    }
                }
                for (int i = 0; i < result.length; i++) {
                    result[i][4] = result[i][4]
                            .replace("t", "Продано")
                            .replace("f", "Заказ");
                }
                break;

            case "get_tariffs":
                result = new String[Integer.parseInt(
                        db.query(true, "SELECT COUNT(*) FROM tariffs")[0][0])][5];

                for (int i = 0; i < result.length; i++) {
                    result[i][0] = db.query(true,
                            "SELECT id_tariff FROM tariffs ORDER BY id_tariff")[i][0];
                    result[i] = db.query(true,
                            "SELECT id_tariff, title, price, description, " +
                                    "array(" +
                                    "   SELECT o.title " +
                                    "   FROM offers o, tariffs t " +
                                    "   WHERE t.id_tariff = " + result[i][0] +
                                    "   AND o.id_offer = ANY (t.ids_offer) " +
                                    ") :: TEXT " +
                                    "FROM tariffs " +
                                    "WHERE id_tariff = " + result[i][0] +
                                    " ORDER BY id_tariff")[0];
                    result[i][2] = String.valueOf(Float.parseFloat(
                            result[i][2]
                                    .substring(0, result[i][2].length() - 1)
                                    .replace(",", ".")
                                    .replace(" ", "")));
                }
                break;

            case "get_last_news":
                result = db.query(true,
                        "SELECT * FROM news WHERE id_news = (SELECT max(id_news) FROM news)");
                break;

            case "get_last_sales":
                result = db.query(true,
                        "SELECT id_sale, date_sale, amount, id_employee, is_sold, " +
                                "client_name, client_pnumber, client_email, " +
                                "array(" +
                                "   SELECT concat(m.name, ' ', pd.model) " +
                                "   FROM phones p, phone_details pd, manufacturers m, sales s " +
                                "   WHERE p.id_phone_detail = pd.id_phone_detail " +
                                "   AND p.id_manufacturer = m.id_manufacturer " +
                                "   AND s.id_sale = (SELECT max(id_sale) FROM sales) " +
                                "   AND p.id_phone = ANY (s.ids_phones)) :: TEXT, " +
                                "units_phones, " +
                                "array(" +
                                "   SELECT t.title " +
                                "   FROM tariffs t, sales s " +
                                "   WHERE s.id_sale = (SELECT max(id_sale) FROM sales) " +
                                "   AND t.id_tariff = ANY (s.ids_tariffs)) :: TEXT, " +
                                "units_tariffs " +
                                "FROM sales " +
                                "WHERE id_sale = (SELECT max(id_sale) FROM sales)");

                result[0][4] = result[0][4]
                        .replace("t", "Продано")
                        .replace("f", "Заказ");
                break;

            case "get_last_employees":
                result = db.query(true,
                        "SELECT id_employee, name, phone_number, email, address, hiring, position, salary " +
                                "FROM employees " +
                                "WHERE id_employee = (SELECT max(id_employee) FROM employees)");
                break;

            case "get_last_tariffs":
                result = db.query(true,
                        "SELECT id_tariff, title, price, description, " +
                                "array(" +
                                "   SELECT o.title " +
                                "   FROM offers o, tariffs t " +
                                "   WHERE t.id_tariff = (SELECT max(id_tariff) FROM tariffs) " +
                                "   AND o.id_offer = ANY (t.ids_offer) " +
                                ") :: TEXT " +
                                "FROM tariffs " +
                                "WHERE id_tariff = (SELECT max(id_tariff) FROM tariffs)");

                result[0][2] = String.valueOf(Float.parseFloat(
                        result[0][2]
                                .substring(0, result[0][2].length() - 1)
                                .replace(",", ".")
                                .replace(" ", "")));
                break;

            case "get_last_offers":
                result = db.query(true,
                        "SELECT * FROM offers " +
                                "WHERE id_offer = (SELECT max(id_offer) FROM offers)");

                result[0][2] = String.valueOf(Float.parseFloat(
                        result[0][2]
                                .substring(0, result[0][2].length() - 1)
                                .replace(",", ".")
                                .replace(" ", "")));
                break;

            case "get_last_phones":
                result = db.query(true,
                        "SELECT p.id_phone, m.name, d.model, d.os, d.ram, d.rom, d.memory_card, " +
                                "d.simcard_quant, d.processor, d.batary, d.diagonal, d.resolution, " +
                                "d.camera_main, d.camera_main_two, d.camera_front, p.color, p.price, p.units " +
                                "FROM phones p, manufacturers m, phone_details d " +
                                "WHERE p.id_manufacturer = m.id_manufacturer " +
                                "AND p.id_phone_detail = d.id_phone_detail " +
                                "AND id_phone = (SELECT max(id_phone) FROM phones)");

                result[0][6] = result[0][6]
                        .replace("t", "Поддерживает")
                        .replace("f", "Не поддерживает");
                result[0][10] = String.valueOf(Float.parseFloat(result[0][10]));
                result[0][12] = String.valueOf(Float.parseFloat(result[0][12]));
                try {
                    result[0][13] = String.valueOf(Float.parseFloat(result[0][13]));
                } catch (NullPointerException e) {
                    result[0][13] = "Нет";
                }
                result[0][14] = String.valueOf(Float.parseFloat(result[0][14]));
                result[0][16] = String.valueOf(Float.parseFloat(
                        result[0][16]
                                .substring(0, result[0][16].length() - 1)
                                .replace(",", ".")
                                .replace(" ", "")));
                break;

            case "get_last_manufacturers":
                result = db.query(true,
                        "SELECT * FROM manufacturers " +
                                "WHERE id_manufacturer = (SELECT max(id_manufacturer) FROM manufacturers)");
                break;

            case "get_last_sale":
                result = db.query(true, "SELECT max(id_sale) FROM sales");
                break;

            case "get_last_pd":
                result = db.query(true, "SELECT max(id_phone_detail) FROM phone_details");
                break;

            case "get_email_employee_p":
                result = db.query(true,
                        "SELECT email " +
                                "FROM employees " +
                                "WHERE email = '" + parameter + "' " +
                                "AND position = 'Admin'");
                break;

            case "get_id_employee_p":
                result = db.query(true,
                        "SELECT id_employee " +
                                "FROM employees " +
                                "WHERE name = '" + parameter + "' " +
                                "ORDER BY id_employee");
                break;

            case "get_phone_p":
                result = db.query(true,
                        "SELECT p.id_phone, m.name, d.model, d.os, d.ram, d.rom, d.memory_card, " +
                                "d.simcard_quant, d.processor, d.batary, d.diagonal, d.resolution, d.camera_main, " +
                                "d.camera_main_two, d.camera_front, p.color, p.price, p.units " +
                                "FROM phones p, manufacturers m, phone_details d " +
                                "WHERE p.id_manufacturer = m.id_manufacturer " +
                                "AND p.id_phone_detail = d.id_phone_detail " +
                                "AND (LOWER(m.name) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(p.color) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(d.model) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(d.os) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(d.processor) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(d.resolution) LIKE LOWER('%" + parameter + "%') " +
                                "OR d.ram = '" + parameter +
                                "' OR d.rom = '" + parameter +
                                "' OR d.batary = '" + parameter +
                                "' OR d.diagonal = '" + parameter +
                                "' OR d.camera_main = '" + parameter +
                                "' OR d.camera_main_two = '" + parameter +
                                "' OR d.camera_front = '" + parameter
                                + "') ORDER BY p.id_phone");

                for (int i = 0; i < result.length; i++) {
                    result[i][6] = result[i][6]
                            .replace("t", "Поддерживает")
                            .replace("f", "Не поддерживает");
                    result[i][10] = String.valueOf(Float.parseFloat(result[i][10]));
                    result[i][12] = String.valueOf(Float.parseFloat(result[i][12]));
                    try {
                        result[i][13] = String.valueOf(Float.parseFloat(result[i][13]));
                    } catch (NullPointerException e) {
                        result[i][13] = "Нет";
                    }
                    result[i][14] = String.valueOf(Float.parseFloat(result[i][14]));
                    result[i][16] = String.valueOf(Float.parseFloat(
                            result[i][16]
                                    .substring(0, result[i][16].length() - 1)
                                    .replace(",", ".")
                                    .replace(" ", "")));
                }
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

                for (int i = 0; i < result.length; i++) {
                    result[i][2] = String.valueOf(Float.parseFloat(
                            result[i][2]
                                    .substring(0, result[i][2].length() - 1)
                                    .replace(",", ".")
                                    .replace(" ", "")));
                }
                break;

            case "get_offer_p":
                result = db.query(true,
                        "SELECT * FROM offers " +
                                "WHERE LOWER(title) LIKE LOWER('%" + parameter + "%') " +
                                "OR LOWER(description) LIKE LOWER('%" + parameter + "%') " +
                                "ORDER BY id_offer");
                for (int i = 0; i < result.length; i++) {
                    result[i][2] = String.valueOf(Float.parseFloat(
                            result[i][2]
                                    .substring(0, result[i][2].length() - 1)
                                    .replace(",", ".")
                                    .replace(" ", "")));
                }
                break;


            case "get_pass_employee_c":
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].startsWith("Е")) {
                        parameters[i] = parameters[i].replace("Е", "email");
                    } else if (parameters[i].startsWith("П")) {
                        parameters[i] = parameters[i].replace("П", "password");
                    }
                }
                result = db.query(true,
                        "SELECT password " +
                                "FROM employees " +
                                "WHERE " + StringUtils.join(parameters, " AND "));
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

                for (int i = 0; i < result.length; i++) {
                    result[0][2] = String.valueOf(Float.parseFloat(
                            result[0][2]
                                    .substring(0, result[0][2].length() - 1)
                                    .replace(",", ".")
                                    .replace(" ", "")));
                }
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
                                " ORDER BY t.id_tariff");

                for (int i = 0; i < result.length; i++) {
                    result[i][2] = String.valueOf(Float.parseFloat(
                            result[i][2]
                                    .substring(0, result[i][2].length() - 1)
                                    .replace(",", ".")
                                    .replace(" ", "")));
                }
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
                        "SELECT p.id_phone, m.name, d.model, d.os, d.ram, d.rom, d.memory_card, " +
                                "d.simcard_quant, d.processor, d.batary, d.diagonal, d.resolution, d.camera_main, " +
                                "d.camera_main_two, d.camera_front, p.color, p.price, p.units " +
                                "FROM phones p, manufacturers m, phone_details d " +
                                "WHERE p.id_manufacturer = m.id_manufacturer " +
                                "AND p.id_phone_detail = d.id_phone_detail " +
                                "AND (" + StringUtils.join(parameters, " AND ") +
                                ") ORDER BY p.id_phone");

                for (int i = 0; i < result.length; i++) {
                    result[i][6] = result[i][6]
                            .replace("t", "Поддерживает")
                            .replace("f", "Не поддерживает");
                    result[i][10] = String.valueOf(Float.parseFloat(result[i][10]));
                    result[i][12] = String.valueOf(Float.parseFloat(result[i][12]));
                    try {
                        result[i][13] = String.valueOf(Float.parseFloat(result[i][13]));
                    } catch (NullPointerException e) {
                        result[i][13] = "Нет";
                    }
                    result[i][14] = String.valueOf(Float.parseFloat(result[i][14]));
                    result[i][16] = String.valueOf(Float.parseFloat(
                            result[i][16]
                                    .substring(0, result[i][16].length() - 1)
                                    .replace(",", ".")
                                    .replace(" ", "")));
                }
                break;

            case "get_employees_names":
                result = db.query(true, "SELECT name FROM employees ORDER BY name");
                break;

            case "get_manufacturers_names":
                result = db.query(true, "SELECT name FROM manufacturers ORDER BY name");
                break;

            case "get_phones_colors":
                result = db.query(true, "SELECT DISTINCT color FROM phones ORDER BY color");
                break;

            case "get_pd_model":
                result = db.query(true, "SELECT model FROM phone_details ORDER BY id_phone_detail");
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

            case "add_employees":
                try {
                    db.query(false,
                            "INSERT INTO employees(name, phone_number, email, address, hiring, position, salary) " +
                                    "VALUES('" + values[0] + "', '" + values[1] + "', '" + values[2] + "', '" +
                                    values[3] + "', '" + values[4] + "', '" + values[5] + "', '" + values[6] + "')");
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "add_tariffs":
                try {
                    db.query(false,
                            "INSERT INTO tariffs(title, price, description) " +
                                    "VALUES ('" + values[0] + "', " + values[1] + ", '" + values[2] + "')");
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "add_offers":
                try {
                    db.query(false,
                            "INSERT INTO offers(title, price, date_start, date_end, description) " +
                                    "VALUES ('" + values[0] + "', " + values[1] + ", '" + values[2] + "', '" +
                                    values[3] + "', '" + values[4] + "')");
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "add_phones":

                try {
                    db.query(false,
                            "INSERT INTO phones(id_manufacturer, id_phone_detail, color, price, units) " +
                                    "VALUES (" +
                                    "(SELECT id_manufacturer FROM manufacturers WHERE name = '" + values[0] + "'), " +
                                    "(SELECT id_phone_detail FROM phone_details WHERE model = '" + values[1] + "'), '" +
                                    values[2] + "', " + values[3] + ", " + values[4] + ")");
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "add_phones_pd":

                try {
                    db.query(false,
                            "INSERT INTO phones(id_manufacturer, id_phone_detail, color, price, units) " +
                                    "VALUES (" +
                                    "(SELECT id_manufacturer FROM manufacturers WHERE name = '" + values[0] + "'), " +
                                    values[1] + ", '" + values[2] + "', " + values[3] + ", " + values[4] + ")");
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "add_phone_details":
                values[12] = values[12]
                        .replace("Поддерживает", "t")
                        .replace("Не поддерживает", "f");
                values[10] = values[10]
                        .replace("Нет", "null");
                try {
                    db.query(false,
                            "INSERT INTO phone_details(model, os, ram, rom, simcard_quant, processor, " +
                                    "batary, diagonal, resolution, camera_main, camera_main_two, camera_front, memory_card) " +
                                    "VALUES('" + values[0] + "', '" + values[1] + "', " +
                                    values[2] + ", " + values[3] + ", " + values[4] + ", '" +
                                    values[5] + "', " + values[6] + ", " + values[7] + ", '" +
                                    values[8] + "', " + values[9] + ", " + values[10] + ", " +
                                    values[11] + ", '" + values[12] + "')");
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "add_manufacturers":
                try {
                    db.query(false,
                            "INSERT INTO manufacturers(name, country) " +
                                    "VALUES('" + values[0] + "', '" + values[1] + "')");
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
                break;

            case "add_union_tariff":
                try {
                    db.query(false, "INSERT INTO union_sales_tariffs " +
                            "VALUES (" + values[0] + ", " + values[1] + ", " + values[2] + ")");
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "update_news":
                String fieldNews = null;

                switch (values[0]) {
                    case "1":
                        fieldNews = "title";
                        break;
                    case "2":
                        fieldNews = "content";
                        break;
                }

                try {
                    db.query(false,
                            "UPDATE news " +
                                    "SET " + fieldNews + " = '" + values[1] + "' " +
                                    "WHERE id_news = " + values[2]);
                } catch (SQLException e) {
                    result = false;
                }
                break;


            case "update_sales":

                String fieldSale = null;

                switch (values[0]) {
                    case "1":
                        fieldSale = "date_sale";
                        break;
                    case "2":
                        fieldSale = "amount";
                        break;
                    case "3":
                        fieldSale = "name";
                        break;
                    case "4":
                        fieldSale = "is_sold";
                        values[1] = values[1]
                                .replace("Продано", "t")
                                .replace("Заказ", "f");
                        break;
                    case "5":
                        fieldSale = "client_name";
                        break;
                    case "6":
                        fieldSale = "client_pnumber";
                        break;
                    case "7":
                        fieldSale = "client_email";
                        break;
                }
                try {
                    db.query(false,
                            "UPDATE sales " +
                                    "SET " +
                                    (!Objects.equals(fieldSale, "name") ? fieldSale : "id_employee")
                                    + " = " +
                                    (!Objects.equals(fieldSale, "name")
                                            ? "'" + values[1] + "'"
                                            : "(SELECT id_employee FROM employees " +
                                            "               WHERE name = '" + values[1] + "')")
                                    + " WHERE id_sale = " + values[2]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "update_manufacturers":
                String fieldManufacturer = null;

                switch (values[0]) {
                    case "1":
                        fieldManufacturer = "name";
                        break;
                    case "2":
                        fieldManufacturer = "country";
                        break;
                }

                try {
                    db.query(false,
                            "UPDATE manufacturers " +
                                    "SET " + fieldManufacturer + " = '" + values[1] + "' " +
                                    "WHERE id_manufacturer = " + values[2]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "update_employees":
                String fieldEmployee = null;

                switch (values[0]) {
                    case "1":
                        fieldEmployee = "name";
                        break;
                    case "2":
                        fieldEmployee = "phone_number";
                        break;
                    case "3":
                        fieldEmployee = "email";
                        break;
                    case "4":
                        fieldEmployee = "address";
                        break;
                    case "5":
                        fieldEmployee = "hiring";
                        break;
                    case "6":
                        fieldEmployee = "position";
                        break;
                    case "7":
                        fieldEmployee = "salary";
                        break;
                }
                try {
                    db.query(false,
                            "UPDATE employees " +
                                    "SET " + fieldEmployee + " = '" + values[1] + "' " +
                                    "WHERE id_employee = " + values[2]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "update_offers":
                String fieldOffer = null;

                switch (values[0]) {
                    case "1":
                        fieldOffer = "title";
                        break;
                    case "2":
                        fieldOffer = "price";
                        break;
                    case "3":
                        fieldOffer = "date_start";
                        break;
                    case "4":
                        fieldOffer = "date_end";
                        break;
                    case "5":
                        fieldOffer = "description";
                        break;
                }
                try {
                    db.query(false,
                            "UPDATE offers " +
                                    "SET " + fieldOffer + " = '" + values[1] + "' " +
                                    "WHERE id_offer = " + values[2]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "update_tariffs":
                String fieldTariff = null;

                switch (values[0]) {
                    case "1":
                        fieldTariff = "title";
                        break;
                    case "2":
                        fieldTariff = "price";
                        break;
                    case "3":
                        fieldTariff = "description";
                        break;
                }
                try {
                    db.query(false,
                            "UPDATE tariffs " +
                                    "SET " + fieldTariff + " = '" + values[1] + "' " +
                                    "WHERE id_tariff = " + values[2]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "update_phones":
                String table = "phones", fieldPhone = null;

                switch (values[0]) {
                    case "1":
                        fieldPhone = "name";
                        break;
                    case "15":
                        fieldPhone = "color";
                        break;
                    case "16":
                        fieldPhone = "price";
                        break;
                    case "17":
                        fieldPhone = "units";
                        break;

                    case "2":
                        table = "phone_details";
                        fieldPhone = "model";
                        break;
                    case "3":
                        table = "phone_details";
                        fieldPhone = "os";
                        break;
                    case "4":
                        table = "phone_details";
                        fieldPhone = "ram";
                        break;
                    case "5":
                        table = "phone_details";
                        fieldPhone = "rom";
                        break;
                    case "6":
                        table = "phone_details";
                        fieldPhone = "memory_card";
                        values[1] = values[1]
                                .replace("Поддерживает", "t")
                                .replace("Не поддерживает", "f");
                        break;
                    case "7":
                        table = "phone_details";
                        fieldPhone = "simcard_quant";
                        break;
                    case "8":
                        table = "phone_details";
                        fieldPhone = "processor";
                        break;
                    case "9":
                        table = "phone_details";
                        fieldPhone = "batary";
                        break;
                    case "10":
                        table = "phone_details";
                        fieldPhone = "diagonal";
                        break;
                    case "11":
                        table = "phone_details";
                        fieldPhone = "resolution";
                        break;
                    case "12":
                        table = "phone_details";
                        fieldPhone = "camera_main";
                        break;
                    case "13":
                        table = "phone_details";
                        fieldPhone = "camera_main_two";
                        values[1] = values[1]
                                .replace("Нет", "null");
                        break;
                    case "14":
                        table = "phone_details";
                        fieldPhone = "camera_front";
                        break;
                }
                try {
                    if (Objects.equals(table, "phones")) {

                        db.query(false,
                                "UPDATE phones " +
                                        "SET " +
                                        (!Objects.equals(fieldPhone, "name") ? fieldPhone : "id_manufacturer")
                                        + " = " +
                                        (!Objects.equals(fieldPhone, "name")
                                                ? (Objects.equals(values[0], "15") ? "'" + values[1] + "'" : values[1])
                                                : "(SELECT id_manufacturer FROM manufacturers " +
                                                "               WHERE name = '" + values[1] + "')")
                                        + " WHERE id_phone = " + values[2]);

                    } else if (Objects.equals(table, "phone_details")) {

                        db.query(false,
                                "UPDATE phone_details " +
                                        "SET " + fieldPhone + " = '" + values[1] + "' " +
                                        "WHERE id_phone_detail = " +
                                        "(SELECT id_phone_detail FROM phones where id_phone = " + values[2] + ")");
                    }
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "del_news":
                try {
                    db.query(false,
                            "DELETE FROM news WHERE id_news = " + values[0]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "del_sales":
                try {
                    db.query(false,
                            "DELETE FROM sales WHERE id_sale = " + values[0]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "del_employees":
                try {
                    db.query(false,
                            "DELETE FROM employees WHERE id_employee = " + values[0]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "del_tariffs":
                try {
                    db.query(false,
                            "DELETE FROM tariffs WHERE id_tariff = " + values[0]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "del_offers":
                try {
                    db.query(false,
                            "DELETE FROM offers WHERE id_offer = " + values[0]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "del_phones":
                try {
                    db.query(false,
                            "DELETE FROM phones WHERE id_phone = " + values[0]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

            case "del_manufacturers":
                try {
                    db.query(false,
                            "DELETE FROM manufacturers WHERE id_manufacturer = " + values[0]);
                } catch (SQLException e) {
                    result = false;
                }
                break;

        }

        return result;
    }

    public PostgreSQL getDb() {
        return db;
    }

    public void setTextAreaLogs(TextArea textAreaLogs) {
        Server.textAreaLogs = textAreaLogs;
    }

    public static void addLog(String log) {
        //try {
        textAreaLogs.appendText(log + "\n");
//        } catch (ArrayIndexOutOfBoundsException e) {
//            textAreaLogs.clear();
//            addLog(log);
//        }
    }

}