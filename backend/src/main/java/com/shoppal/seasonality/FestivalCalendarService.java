package com.shoppal.seasonality;

import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class FestivalCalendarService {
    private final FestivalCalendarProperties properties;
    public FestivalCalendarService(FestivalCalendarProperties properties) { this.properties = properties; }

    public double multiplier(LocalDate startDate, int horizonDays) {
        if (horizonDays < 1) return 1.0;
        double total = 0;
        for (int d = 0; d < horizonDays; d++) {
            LocalDate day = startDate.plusDays(d);
            double daily = properties.getFestivals().stream()
                    .filter(f -> f.getStartDate() != null && f.getEndDate() != null
                            && !day.isBefore(f.getStartDate()) && !day.isAfter(f.getEndDate()))
                    .mapToDouble(FestivalCalendarProperties.Festival::getMultiplier).max().orElse(1.0);
            total += daily;
        }
        return Math.round(total / horizonDays * 100.0) / 100.0;
    }

    public int calendarYear() { return properties.getYear(); }
}
