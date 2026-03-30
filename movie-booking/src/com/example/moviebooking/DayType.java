package com.example.moviebooking;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum DayType {
    WEEKDAY,
    WEEKEND;

    public static DayType from(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return WEEKEND;
        }
        return WEEKDAY;
    }
}
