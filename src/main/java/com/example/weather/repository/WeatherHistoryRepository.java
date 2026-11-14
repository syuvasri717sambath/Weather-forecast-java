package com.example.weather.repository;

import com.example.weather.model.WeatherHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WeatherHistoryRepository extends JpaRepository<WeatherHistory, Long> {
    List<WeatherHistory> findAllByOrderByIdDesc();
}