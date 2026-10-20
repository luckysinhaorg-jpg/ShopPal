package com.shoppal.seasonality;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FestivalCalendarServiceTest {
    @Test void averagesConfiguredDailyMultipliersOnceAcrossHorizon() {
        var festival=new FestivalCalendarProperties.Festival();
        festival.setName("Example"); festival.setStartDate(LocalDate.of(2026,10,20));
        festival.setEndDate(LocalDate.of(2026,10,20)); festival.setMultiplier(1.5);
        var properties=new FestivalCalendarProperties(); properties.setYear(2026); properties.setFestivals(List.of(festival));
        var calendar=new FestivalCalendarService(properties);
        assertEquals(1.25,calendar.multiplier(LocalDate.of(2026,10,20),2),0.001);
        assertEquals(2026,calendar.calendarYear());
    }
}
