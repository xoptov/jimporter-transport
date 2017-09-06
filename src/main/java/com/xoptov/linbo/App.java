package com.xoptov.linbo;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.CommandLineParser;

public class App 
{
    private static String maf;

    private static String mof;

    private static int category;

    private static int manufacturer;

    private static int model;

    private static String dbhost;

    private static String dbname;

    private static String dbuser;

    private static String dbpasswd;

    private static HashMap<Integer, Manufacturer> manufacturers;

    private static HashSet<Model> models;

    public static void main(String[] args)
    {
        parseCommandLine(args);
        readManufacturersData();
        readModelsData();

        String url = "jdbc:mysql://" + App.dbhost + '/' + App.dbname;
        Properties properties = new Properties();
        properties.setProperty("serverTimezone", "Europe/Moscow");
        properties.setProperty("useSSL", "false");
        properties.setProperty("characterEncoding", "utf8");
        properties.setProperty("user", App.dbuser);
        properties.setProperty("password", App.dbpasswd);

        try (Connection conn = DriverManager.getConnection(url, properties)) {
            loadManufacturers(conn);
            loadModels(conn);
            createManufacturers(conn);
            createModels(conn);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void parseCommandLine(String[] args)
    {
        Options options = new Options();
        options.addOption("maf", true, "File for input manufacturers.");
        options.addOption("mof", true, "File for input models.");
        options.addOption("c", true, "Category ID for linking values.");
        options.addOption("map", true, "Mark property ID.");
        options.addOption("mop", true, "Model property ID.");
        options.addOption("h", true, "Database host.");
        options.addOption("b", true, "Database name.");
        options.addOption("u", true, "Database username.");
        options.addOption("p", true, "Database password.");

        CommandLineParser parser = new DefaultParser();
        App.manufacturers = new HashMap<>();
        App.models = new HashSet<>();

        try {
            CommandLine cmd = parser.parse(options, args);
            App.maf = cmd.getOptionValue("maf");
            App.mof = cmd.getOptionValue("mof");
            App.category = Integer.parseInt(cmd.getOptionValue('c'));
            App.manufacturer = Integer.parseInt(cmd.getOptionValue("map"));
            App.model = Integer.parseInt(cmd.getOptionValue("mop"));
            App.dbhost = cmd.getOptionValue('h');
            App.dbname = cmd.getOptionValue('b');
            App.dbuser = cmd.getOptionValue('u');
            App.dbpasswd = cmd.getOptionValue('p');
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void readManufacturersData()
    {
        try (FileReader in = new FileReader(App.maf)) {
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);

            for (CSVRecord record: records) {
                Manufacturer manufacturer = new Manufacturer();
                manufacturer.setOriginId(Integer.parseInt(record.get("id_car_mark")));
                manufacturer.setName(record.get("name"));
                App.manufacturers.put(manufacturer.getOriginId(), manufacturer);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void readModelsData()
    {
        try (FileReader in = new FileReader(App.mof)) {
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);

            for (CSVRecord record: records) {
                int manufacturerId = Integer.parseInt(record.get("id_car_mark"));
                Model model = new Model();
                model.setOriginId(Integer.parseInt(record.get("id_car_model")));
                model.setName(record.get("name"));

                if (App.manufacturers.containsKey(manufacturerId)) {
                    model.setManufacturer(App.manufacturers.get(manufacturerId));
                    App.models.add(model);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void loadManufacturers(Connection conn)
    {
        String sql = "SELECT v.id, v.as_string " +
                "FROM `value` v " +
                "INNER JOIN `property_value` pv ON v.id = pv.value_id " +
                "WHERE pv.category_id = ? " +
                "AND pv.property_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, App.category);
            pstmt.setInt(2, App.manufacturer);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                App.manufacturers.forEach((Integer i, Manufacturer m) -> {
                    try {
                        if (m.getName().equalsIgnoreCase(rs.getString("as_string"))) {
                            m.setId(rs.getInt("id"));
                        }
                    } catch (SQLException e) {
                        System.out.println(e.getMessage());
                    }
                });
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void loadModels(Connection conn)
    {
        String sql = "SELECT v.id, v.as_string " +
                "FROM `value` v " +
                "INNER JOIN `property_value` pv ON v.id = pv.value_id " +
                "WHERE pv.category_id = ? " +
                "AND pv.property_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, App.category);
            pstmt.setInt(2, App.model);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                App.models.removeIf((Model m) -> {
                    try {
                        return (m.getName().equalsIgnoreCase(rs.getString("as_string")));
                    } catch (SQLException e) {
                        return false;
                    }
                });
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void createManufacturers(Connection conn)
    {

        App.manufacturers.forEach((Integer i, Manufacturer m) -> {
            if (m.getId() == 0) {
                try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO value(as_string) VALUES(?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, m.getName());
                    pstmt.executeUpdate();
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.first()) {
                        m.setId(rs.getInt(1));
                    }
                    rs.close();
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }

                try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO property_value(category_id, property_id, value_id) VALUES(?,?,?)")) {
                    pstmt.setInt(1, App.category);
                    pstmt.setInt(2, App.manufacturer);
                    pstmt.setInt(3, m.getId());
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
        });
    }

    private static void createModels(Connection conn)
    {
        App.models.forEach((Model m) -> {
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO value(as_string, parent_id) VALUES(?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, m.getName());
                pstmt.setInt(2, m.getManufacturer().getId());
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.first()) {
                    m.setId(rs.getInt(1));
                }
                rs.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO property_value(category_id, property_id, value_id) VALUES(?, ?, ?)")) {
                pstmt.setInt(1, App.category);
                pstmt.setInt(2, App.model);
                pstmt.setInt(3, m.getId());
                pstmt.executeUpdate();

            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        });
    }
}
