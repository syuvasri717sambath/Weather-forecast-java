package com.example.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class WeatherService {

    @Value("${weather.api.key:YOUR_API_KEY}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> getCurrentWeather(String city) throws Exception {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);

        if (apiKey != null && !apiKey.equals("YOUR_API_KEY") && !apiKey.isBlank()) {
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + encodedCity + "&appid=" + apiKey + "&units=metric";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                Map<String, Object> data = new HashMap<>();
                data.put("city", root.path("name").asText());
                data.put("country", root.path("sys").path("country").asText());
                data.put("temperature", root.path("main").path("temp").asDouble());
                data.put("humidity", root.path("main").path("humidity").asInt());
                data.put("windSpeed", root.path("wind").path("speed").asDouble());
                data.put("description", root.path("weather").get(0).path("description").asText());
                return data;
            }
        }

        JsonNode geo = geocodeCity(encodedCity);
        double lat = geo.path("latitude").asDouble();
        double lon = geo.path("longitude").asDouble();

        String weatherUrl = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code",
                lat, lon);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(weatherUrl)).GET().build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode current = mapper.readTree(res.body()).path("current");

        Map<String, Object> data = new HashMap<>();
        data.put("city", geo.path("name").asText());
        data.put("country", geo.path("country").asText("N/A"));
        data.put("temperature", current.path("temperature_2m").asDouble());
        data.put("humidity", current.path("relative_humidity_2m").asInt());
        data.put("windSpeed", current.path("wind_speed_10m").asDouble());
        data.put("description", decodeWeatherCode(current.path("weather_code").asInt()));
        return data;
    }

    public Map<String, Object> get7DayForecast(String city) throws Exception {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        JsonNode geo = geocodeCity(encodedCity);
        double lat = geo.path("latitude").asDouble();
        double lon = geo.path("longitude").asDouble();

        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto",
                lat, lon);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode daily = mapper.readTree(res.body()).path("daily");

        JsonNode datesNode = daily.path("time");
        JsonNode maxTempNode = daily.path("temperature_2m_max");
        JsonNode minTempNode = daily.path("temperature_2m_min");
        
        int size = datesNode.size();
        Map<String, Object>[] daysArray = new Map[size];
        
        // Arrays for Chart.js Graphs
        String[] forecastDates = new String[size];
        double[] maxTemps = new double[size];
        double[] minTemps = new double[size];

        for (int i = 0; i < size; i++) {
            String dateStr = datesNode.get(i).asText();
            double maxT = maxTempNode.get(i).asDouble();
            double minT = minTempNode.get(i).asDouble();

            forecastDates[i] = dateStr;
            maxTemps[i] = maxT;
            minTemps[i] = minT;

            Map<String, Object> day = new HashMap<>();
            day.put("date", dateStr);
            day.put("maxTemp", maxT);
            day.put("minTemp", minT);
            day.put("rainChance", daily.path("precipitation_probability_max").get(i).asInt());
            day.put("description", decodeWeatherCode(daily.path("weather_code").get(i).asInt()));
            daysArray[i] = day;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("city", geo.path("name").asText());
        result.put("country", geo.path("country").asText("N/A"));
        result.put("forecast", daysArray);
        
        // Adding chart datasets
        result.put("forecastDates", forecastDates);
        result.put("maxTemps", maxTemps);
        result.put("minTemps", minTemps);

        return result;
    }

    // --- AI Clothing & Activity Recommendation Feature ---
    public String getSmartRecommendation(double temp, int rainChance, String description) {
        String desc = description != null ? description.toLowerCase() : "";
        if (rainChance > 70 || desc.contains("rain") || desc.contains("shower")) {
            return "🌧️ Heavy rain chance! Don't forget to carry your umbrella today.";
        } else if (temp < 15.0) {
            return "🧥 Chilly weather! Grab a warm jacket and enjoy a hot cup of coffee.";
        } else if (temp > 30.0) {
            return "☀️ It's quite hot outside! Stay hydrated, wear sunglasses, and use sunscreen.";
        } else {
            return "🌤️ Pleasant weather outside. Have a wonderful and productive day!";
        }
    }

    // --- NEW: AI Weather-Based Smart Trip & Itinerary Generator ---
    public Map<String, String> generateItinerary(double temp, int humidity, String description) {
        Map<String, String> itinerary = new HashMap<>();
        String descLower = description != null ? description.toLowerCase() : "";

        if (temp > 32) {
            itinerary.put("morning", "🌅 Early Morning (6 AM - 9 AM): Great time for a quick outdoor walk before the sun gets harsh. Visit a local park or garden.");
            itinerary.put("afternoon", "☀️ Afternoon (12 PM - 4 PM): High heat alert! Stay indoors in air-conditioned places. Visit a museum, art gallery, or chill at a cool café with iced coffee.");
            itinerary.put("evening", "🌙 Evening (5 PM - 8 PM): Weather cools down slightly. Perfect for an ice cream run, shopping mall visit, or relaxing rooftop dinner.");
        } else if (descLower.contains("rain") || descLower.contains("shower") || humidity > 85) {
            itinerary.put("morning", "🌅 Early Morning (6 AM - 9 AM): Cozy indoor morning. Enjoy hot filter coffee/tea with snacks while listening to music.");
            itinerary.put("afternoon", "🌧️ Afternoon (12 PM - 4 PM): Rainy conditions outside. Best for indoor board games, watching a movie, or visiting an indoor bookstore/library.");
            itinerary.put("evening", "🌙 Evening (5 PM - 8 PM): Rain slows down. Take a peaceful umbrella walk on paved streets and enjoy hot street food snacks.");
        } else if (temp < 18) {
            itinerary.put("morning", "🌅 Early Morning (6 AM - 9 AM): Crisp cool weather! Great for an energizing jog or warm-up exercise with a light jacket.");
            itinerary.put("afternoon", "🌤️ Afternoon (12 PM - 4 PM): Pleasant sunshine. Perfect time for sightseeing, historical monuments, or outdoor photography.");
            itinerary.put("evening", "🌙 Evening (5 PM - 8 PM): Getting chilly. Enjoy a campfire, warm soup, or a cozy dinner at an open-air restaurant.");
        } else {
            itinerary.put("morning", "🌅 Early Morning (6 AM - 9 AM): Perfect balanced weather! Ideal for outdoor cycling, jogging, or yoga in the park.");
            itinerary.put("afternoon", "☀️ Afternoon (12 PM - 4 PM): Comfortable conditions. Good for exploring local markets, city tour, or casual outdoor cafe hopping.");
            itinerary.put("evening", "🌙 Evening (5 PM - 8 PM): Beautiful evening breeze. Perfect for a long drive, beach walk, or hanging out with friends outdoors.");
        }
        return itinerary;
    }

    private JsonNode geocodeCity(String encodedCity) throws Exception {
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedCity + "&count=1";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(geoUrl)).GET().build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode results = mapper.readTree(res.body()).path("results");
        if (results.isMissingNode() || results.isEmpty()) {
            throw new IllegalArgumentException("City not found: " + encodedCity);
        }
        return results.get(0);
    }

    private String decodeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Clear Sky";
            case 1, 2, 3 -> "Partly Cloudy";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Light Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snowfall";
            case 80, 81, 82 -> "Rain Showers";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Moderate Conditions";
        };
    }
}