package com.example.weather.controller;

import com.example.weather.model.WeatherHistory;
import com.example.weather.repository.WeatherHistoryRepository;
import com.example.weather.service.WeatherService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class WeatherController {

    private final WeatherService weatherService;
    private final WeatherHistoryRepository historyRepo;

    public WeatherController(WeatherService weatherService, WeatherHistoryRepository historyRepo) {
        this.weatherService = weatherService;
        this.historyRepo = historyRepo;
    }

    @GetMapping("/")
    public String index(@RequestParam(value = "city", required = false) String city, Model model) {
        if (city != null && !city.isBlank()) {
            try {
                Map<String, Object> weather = weatherService.getCurrentWeather(city.trim());
                model.addAttribute("weather", weather);

                // --- AI Clothing & Activity Recommendation Integration ---
                double temp = (double) weather.get("temperature");
                int humidity = (int) weather.get("humidity");
                String description = (String) weather.get("description");
                
                String recommendation = weatherService.getSmartRecommendation(temp, humidity, description);
                model.addAttribute("recommendation", recommendation);
                // --------------------------------------------------------

                // --- AI Weather-Based Smart Trip Itinerary Integration ---
                Map<String, String> itinerary = weatherService.generateItinerary(temp, humidity, description);
                model.addAttribute("itinerary", itinerary);
                // ------------------------------------------------------------

                WeatherHistory record = new WeatherHistory(
                        (String) weather.get("city"),
                        (String) weather.get("country"),
                        (double) weather.get("temperature"),
                        (int) weather.get("humidity"),
                        (double) weather.get("windSpeed"),
                        (String) weather.get("description")
                );
                historyRepo.save(record);
            } catch (Exception e) {
                model.addAttribute("error", "Could not find weather for '" + city + "'. Please check the city name.");
            }
        }
        return "index";
    }

    @GetMapping("/forecast")
    public String forecast7Day(@RequestParam(value = "city", defaultValue = "Chennai") String city, Model model) {
        try {
            Map<String, Object> data = weatherService.get7DayForecast(city.trim());
            model.addAttribute("data", data);
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load 7-day forecast for '" + city + "'.");
        }
        return "forecast_7day";
    }

    // --- City vs City Weather Comparison Endpoint ---
    @GetMapping("/compare")
    public String compareWeather(
            @RequestParam(value = "city1", required = false) String city1,
            @RequestParam(value = "city2", required = false) String city2,
            Model model) {
        if (city1 != null && !city1.isBlank() && city2 != null && !city2.isBlank()) {
            try {
                // Fetch City 1 Weather & AI Recommendation
                Map<String, Object> weather1 = weatherService.getCurrentWeather(city1.trim());
                double t1 = (double) weather1.get("temperature");
                int h1 = (int) weather1.get("humidity");
                String d1 = (String) weather1.get("description");
                String rec1 = weatherService.getSmartRecommendation(t1, h1, d1);

                // Fetch City 2 Weather & AI Recommendation
                Map<String, Object> weather2 = weatherService.getCurrentWeather(city2.trim());
                double t2 = (double) weather2.get("temperature");
                int h2 = (int) weather2.get("humidity");
                String d2 = (String) weather2.get("description");
                String rec2 = weatherService.getSmartRecommendation(t2, h2, d2);

                model.addAttribute("w1", weather1);
                model.addAttribute("rec1", rec1);
                model.addAttribute("w2", weather2);
                model.addAttribute("rec2", rec2);
            } catch (Exception e) {
                model.addAttribute("error", "Could not find weather for one or both cities. Please check the city names.");
            }
        }
        return "compare";
    }
    // ---------------------------------------------

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("records", historyRepo.findAllByOrderByIdDesc());
        return "history";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("records", historyRepo.findAllByOrderByIdDesc());
        return "admin";
    }

    @PostMapping("/admin/delete/{id}")
    public String deleteRecord(@PathVariable Long id) {
        historyRepo.deleteById(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/clear")
    public String clearAllRecords() {
        historyRepo.deleteAll();
        return "redirect:/admin";
    }
}