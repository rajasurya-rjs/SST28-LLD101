package com.example.moviebooking;

import java.time.LocalTime;

public enum ShowTiming {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT;

    public static ShowTiming from(LocalTime time) {
        int hour = time.getHour();
        if (hour < 12) {
            return MORNING;
        } else if (hour < 16) {
            return AFTERNOON;
        } else if (hour < 20) {
            return EVENING;
        } else {
            return NIGHT;
        }
    }
}
