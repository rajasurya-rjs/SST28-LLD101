package com.example.moviebooking;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Movie {
    private final String id;
    private final String title;
    private final Map<String, String> localizedTitles;
    private final int durationMinutes;
    private final String genre;

    public Movie(String id, String title, Map<String, String> localizedTitles,
                 int durationMinutes, String genre) {
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.localizedTitles = Collections.unmodifiableMap(new HashMap<>(localizedTitles));
        this.durationMinutes = durationMinutes;
        this.genre = Objects.requireNonNull(genre);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getGenre() { return genre; }
    public Map<String, String> getLocalizedTitles() { return localizedTitles; }

    public String getTitle(String languageCode) {
        return localizedTitles.getOrDefault(languageCode, title);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movie movie)) return false;
        return id.equals(movie.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "Movie{" + title + ", " + genre + ", " + durationMinutes + "min}";
    }
}
