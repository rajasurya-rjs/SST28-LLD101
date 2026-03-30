package com.example.moviebooking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Theater {
    private final String id;
    private final String name;
    private final String cityId;
    private final List<Screen> screens;

    public Theater(String id, String name, String cityId) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.cityId = Objects.requireNonNull(cityId);
        this.screens = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCityId() { return cityId; }
    public List<Screen> getScreens() { return Collections.unmodifiableList(screens); }

    public void addScreen(Screen screen) {
        screens.add(screen);
    }

    @Override
    public String toString() {
        return "Theater{" + name + ", screens=" + screens.size() + "}";
    }
}
