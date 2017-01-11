package com.theironyard.charlotte;

import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Integer.parseInt;

public class Main {

    public static void insertRestaurant(Connection conn, String name, String price, int rating) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO restaurants VALUES (NULL, ?, ?, ?)");
        stmt.setString(1, name);
        stmt.setString(2, price);
        stmt.setInt(3, rating);
        stmt.execute();
    }

    public static void changeRestaurant(Connection conn, Restaurant rest) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE restaurants SET name = ?, price = ? ,rating = ? WHERE ID = ?");
        stmt.setInt(4, rest.getId());
        stmt.setString(1, rest.getName());
        stmt.setString(2, rest.getPrice());
        stmt.setInt(3, rest.getRating());
        stmt.execute();
    }

    public static void deleteRestaurant(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM restaurants WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }

    public static ArrayList<Restaurant> showRestaurants(Connection conn) throws SQLException {
        ArrayList<Restaurant> restaurants = new ArrayList<>();
        Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery("SELECT * FROM restaurants");
        while (results.next()) {
            int id = results.getInt("id");
            String name = results.getString("name");
            String price = results.getString("price");
            int rating = results.getInt("rating");
            restaurants.add(new Restaurant(id, name, price, rating));
        }
        return restaurants;
    }

    public static Restaurant getRestaurantById(Connection conn, int id) throws SQLException {
        Restaurant item = null;
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM restaurants where id = ?");
        stmt.setInt(1, id);
        stmt.execute();

        ResultSet result = stmt.executeQuery();
        if (result.next()) {
            String name = result.getString("name");
            String price = result.getString("price");
            int rating = result.getInt("rating");
            item = new Restaurant(id, name, price, rating);
        }
        return item;
    }


    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS restaurants (id IDENTITY, name VARCHAR, price VARCHAR, rating INT)");

        Spark.get("/", (req, res) -> {
            HashMap m = new HashMap();
            m.put("restaurants", showRestaurants(conn));
            return new ModelAndView(m, "restaurants.html");
        }, new MustacheTemplateEngine());

        Spark.get("/restaurants", (req, res) -> {
            HashMap m = new HashMap();
            m.put("restaurants", showRestaurants(conn));
            return new ModelAndView(m, "restaurants.html");
        }, new MustacheTemplateEngine());

        Spark.get("/create-restaurant", ((req, res) -> {
            HashMap m = new HashMap();
            m.put("restaurants", showRestaurants(conn));
            return new ModelAndView(m, "restaurants.html");
        }), new MustacheTemplateEngine());

        Spark.get("/restaurants/:id", (req, res) -> {
            int id = Integer.valueOf(req.params("id"));
            Restaurant current = getRestaurantById(conn, id);
            HashMap m = new HashMap();
            m.put("restaurant", current);
            return new ModelAndView(m, "restaurant.html");
        }, new MustacheTemplateEngine());

        Spark.post("/create-restaurant", (req, res) -> {
            insertRestaurant(conn, req.queryParams("name"),
                    req.queryParams("price"),
                    //must parse the int because strings are all http knows... after an hour I learned
                    parseInt(req.queryParams("rating")));
            res.redirect("/restaurants");
            return "";
        });

        Spark.post("/restaurants/:id", (req, res) -> {
            int id = Integer.valueOf(req.params("id"));

            Restaurant current = getRestaurantById(conn, id);
            current.setId(Integer.valueOf(req.queryParams("id")));
            current.setName(req.queryParams("name"));
            current.setPrice(req.queryParams("price"));
            current.setRating(parseInt(req.queryParams("rating")));

            changeRestaurant(conn, current);
            res.redirect("/");
            return "";
        });

        Spark.post("/delete-restaurant", (req, res) -> {
            deleteRestaurant(conn, parseInt(req.queryParams("id")));
            res.redirect("/restaurants");
            return new ModelAndView(showRestaurants(conn), "restaurant.html");
        }, new MustacheTemplateEngine());


    }
}
