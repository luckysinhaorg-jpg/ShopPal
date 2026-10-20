package com.shoppal.seasonality;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "shoppal.seasonality")
public class FestivalCalendarProperties {
    private int year = 2026;
    private List<Festival> festivals = new ArrayList<>();
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public List<Festival> getFestivals() { return festivals; }
    public void setFestivals(List<Festival> festivals) { this.festivals = festivals; }

    public static class Festival {
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private double multiplier = 1.0;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public double getMultiplier() { return multiplier; }
        public void setMultiplier(double multiplier) { this.multiplier = multiplier; }
    }
}
