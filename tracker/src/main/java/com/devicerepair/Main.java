package com.devicerepair;

import com.devicerepair.api.ApiController;
import com.devicerepair.database.DatabaseConfig;
import com.devicerepair.service.RepairManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Main {
    public static void main(String[] args) {
        DatabaseConfig.initializeDatabase();

        RepairManager repairManager = new RepairManager();
        Javalin app = Javalin.create(config -> {
            ObjectMapper objectMapper = JavalinJackson.defaultMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            config.jsonMapper(new JavalinJackson(objectMapper));
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
            });
        });

        app.get("/", ctx -> ctx.redirect("/index.html"));
        new ApiController().registerRoutes(app, repairManager);
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7000;
        app.start(port);
        System.out.println("Device Repair Tracker running on http://localhost:" + port);
    }
}
