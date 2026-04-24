package com.plugin;

import com.intellij.ide.util.PropertiesComponent;
import com.posthog.java.PostHog;

import java.util.Map;
import java.util.UUID;

public class AnalyticsService {
    private static final String API_KEY = "phc_zaep9SmUJKSeYob5AZQ4QAzghpZ5VhF6HrjJbSmuuQws";
    private static final String HOST = "https://us.i.posthog.com";

    private static final PostHog server = new PostHog.Builder(API_KEY)
            .host(HOST)
            .build();

    private static String getId() {
        PropertiesComponent pc = PropertiesComponent.getInstance();

        String id = pc.getValue("KOTEA-plugin.id");

        if (id == null) {
            id = UUID.randomUUID().toString();
            pc.setValue("KOTEA-plugin.id",  id);
        }

        return id;
    }

    public static void log(String event, Map<String, Object> properties) {
        String id = getId();
        server.capture(id, event, properties);
    }

}
