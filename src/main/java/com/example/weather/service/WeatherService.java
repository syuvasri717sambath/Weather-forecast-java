package com.example.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable; // <-- Added for Caching
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

    // CURRENT WEATHER (Added Caching)
    @Cacheable(value = "currentWeatherCache", key = "#city.trim().toLowerCase()")
    public Map<String, Object> getCurrentWeather(String city) throws Exception {

        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("Please enter a city name.");
        }

        String encodedCity = URLEncoder.encode(
                city.trim(), StandardCharsets.UTF_8);

        // OpenWeatherMap API (optional)
        if (apiKey != null
                && !apiKey.isBlank()
                && !apiKey.equals("YOUR_API_KEY")) {

            String url = "https://api.openweathermap.org/data/2.5/weather?q="
                    + encodedCity + "&appid=" + apiKey + "&units=metric";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {

                JsonNode root = mapper.readTree(response.body());
                JsonNode main = root.path("main");
                JsonNode weather = root.path("weather");

                if (!main.hasNonNull("temp")
                        || !main.hasNonNull("humidity")
                        || weather.isEmpty()
                        || !root.path("wind").hasNonNull("speed")) {
                    throw new RuntimeException(
                            "OpenWeatherMap returned incomplete weather data.");
                }

                Map<String, Object> data = new HashMap<>();

                data.put("city", root.path("name").asText());
                data.put("country", root.path("sys").path("country").asText());
                data.put("temperature", main.path("temp").asDouble());
                data.put("humidity", main.path("humidity").asInt());

                // Convert metres/second to km/h
                data.put("windSpeed",
                        root.path("wind").path("speed").asDouble() * 3.6);

                data.put("description",
                        weather.get(0).path("description").asText());

                return data;
            }

            System.out.println("OpenWeatherMap HTTP Status: "
                    + response.statusCode());
            System.out.println("OpenWeatherMap Response: "
                    + response.body());
        }

        // Open-Meteo fallback
        JsonNode geo = geocodeCity(encodedCity);

        double lat = geo.path("latitude").asDouble(Double.NaN);
        double lon = geo.path("longitude").asDouble(Double.NaN);

        if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
            throw new RuntimeException("Invalid city coordinates.");
        }

        String weatherUrl = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast"
                        + "?latitude=%f&longitude=%f"
                        + "&current=temperature_2m,relative_humidity_2m,"
                        + "wind_speed_10m,weather_code"
                        + "&wind_speed_unit=kmh&timezone=auto",
                lat, lon);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(weatherUrl))
                .GET()
                .build();

        HttpResponse<String> res = httpClient.send(
                req, HttpResponse.BodyHandlers.ofString());

        System.out.println("Open-Meteo HTTP Status: " + res.statusCode());
        System.out.println("Open-Meteo Response: " + res.body());

        if (res.statusCode() == 429) {
            throw new RuntimeException("Daily API request limit exceeded (HTTP 429). Please try again later or check cached data.");
        }

        if (res.statusCode() != 200) {
            throw new RuntimeException(
                    "Weather API failed. HTTP Status: " + res.statusCode()
                            + " Response: " + res.body());
        }

        JsonNode responseJson = mapper.readTree(res.body());
        JsonNode current = responseJson.path("current");

        if (current.isMissingNode()
                || current.isNull()
                || !current.hasNonNull("temperature_2m")
                || !current.hasNonNull("relative_humidity_2m")
                || !current.hasNonNull("wind_speed_10m")
                || !current.hasNonNull("weather_code")) {

            throw new RuntimeException(
                    "Weather API response is missing current weather data: "
                            + res.body());
        }

        Map<String, Object> data = new HashMap<>();

        data.put("city", geo.path("name").asText());
        data.put("country", geo.path("country").asText("N/A"));
        data.put("temperature",
                current.path("temperature_2m").asDouble());
        data.put("humidity",
                current.path("relative_humidity_2m").asInt());
        data.put("windSpeed",
                current.path("wind_speed_10m").asDouble());
        data.put("description",
                decodeWeatherCode(current.path("weather_code").asInt()));

        return data;
    }

    // 7-DAY WEATHER FORECAST (Added Caching)
    @Cacheable(value = "forecastCache", key = "#city.trim().toLowerCase()")
    public Map<String, Object> get7DayForecast(String city) throws Exception {

        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("Please enter a city name.");
        }

        String encodedCity = URLEncoder.encode(
                city.trim(), StandardCharsets.UTF_8);

        JsonNode geo = geocodeCity(encodedCity);

        double lat = geo.path("latitude").asDouble(Double.NaN);
        double lon = geo.path("longitude").asDouble(Double.NaN);

        if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
            throw new RuntimeException("Invalid city coordinates.");
        }

        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast"
                        + "?latitude=%f&longitude=%f"
                        + "&daily=weather_code,temperature_2m_max,"
                        + "temperature_2m_min,precipitation_probability_max"
                        + "&timezone=auto&forecast_days=7",
                lat, lon);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> res = httpClient.send(
                req, HttpResponse.BodyHandlers.ofString());

        // 7-DAY FORECAST DEBUGGING LOGS
        System.out.println("7-Day Forecast API URL: " + url);
        System.out.println("7-Day Forecast HTTP Status: " + res.statusCode());
        System.out.println("7-Day Forecast API Response: " + res.body());

        if (res.statusCode() == 429) {
            throw new RuntimeException("Daily API request limit exceeded (HTTP 429). Please try again later or check cached results.");
        }

        if (res.statusCode() != 200) {
            throw new RuntimeException(
                    "Forecast API failed. HTTP Status: "
                            + res.statusCode() + " Response: " + res.body());
        }

        JsonNode daily = mapper.readTree(res.body()).path("daily");

        JsonNode datesNode = daily.path("time");
        JsonNode maxTempNode = daily.path("temperature_2m_max");
        JsonNode minTempNode = daily.path("temperature_2m_min");
        JsonNode rainNode = daily.path("precipitation_probability_max");
        JsonNode codeNode = daily.path("weather_code");

        if (!datesNode.isArray()
                || !maxTempNode.isArray()
                || !minTempNode.isArray()
                || !rainNode.isArray()
                || !codeNode.isArray()
                || datesNode.isEmpty()) {

            throw new RuntimeException(
                    "7-day forecast response is incomplete: " + res.body());
        }

        int size = datesNode.size();

        if (maxTempNode.size() < size
                || minTempNode.size() < size
                || rainNode.size() < size
                || codeNode.size() < size) {
            throw new RuntimeException("Incomplete 7-day forecast data.");
        }

        Map<String, Object>[] daysArray = new Map[size];

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
            day.put("rainChance", rainNode.get(i).asInt());
            day.put("description",
                    decodeWeatherCode(codeNode.get(i).asInt()));

            daysArray[i] = day;
        }

        Map<String, Object> result = new HashMap<>();

        result.put("city", geo.path("name").asText());
        result.put("country", geo.path("country").asText("N/A"));
        result.put("forecast", daysArray);
        result.put("forecastDates", forecastDates);
        result.put("maxTemps", maxTemps);
        result.put("minTemps", minTemps);

        return result;
    }

    // AI WEATHER RECOMMENDATION
    public String getSmartRecommendation(
            double temp, int rainChance, String description) {

        String desc = description != null
                ? description.toLowerCase() : "";

        if (rainChance > 70
                || desc.contains("rain")
                || desc.contains("shower")
                || desc.contains("drizzle")
                || desc.contains("thunderstorm")) {

            return "🌧️ Heavy rain chance! Don't forget to carry your umbrella today.";

        } else if (temp < 15.0) {

            return "🧥 Chilly weather! Grab a warm jacket and enjoy a hot cup of coffee.";

        } else if (temp > 30.0) {

            return "☀️ It's quite hot outside! Stay hydrated, wear sunglasses, and use sunscreen.";

        } else {

            return "🌤️ Pleasant weather outside. Have a wonderful and productive day!";
        }
    }

    // WEATHER-BASED ITINERARY
    public Map<String, String> generateItinerary(
            double temp, int humidity, String description) {

        Map<String, String> itinerary = new HashMap<>();

        String descLower = description != null
                ? description.toLowerCase() : "";

        if (temp > 32) {

            itinerary.put("morning",
                    "🌅 Early Morning (6 AM - 9 AM): Great time for a quick outdoor walk before the sun gets harsh. Visit a local park or garden.");

            itinerary.put("afternoon",
                    "☀️ Afternoon (12 PM - 4 PM): High heat alert! Stay indoors in air-conditioned places. Visit a museum, art gallery, or chill at a cool café with iced coffee.");

            itinerary.put("evening",
                    "🌙 Evening (5 PM - 8 PM): Weather cools down slightly. Perfect for an ice cream run, shopping mall visit, or relaxing rooftop dinner.");

        } else if (descLower.contains("rain")
                || descLower.contains("shower")
                || descLower.contains("drizzle")
                || descLower.contains("thunderstorm")
                || humidity > 85) {

            itinerary.put("morning",
                    "🌅 Early Morning (6 AM - 9 AM): Cozy indoor morning. Enjoy hot filter coffee/tea with snacks while listening to music.");

            itinerary.put("afternoon",
                    "🌧️ Afternoon (12 PM - 4 PM): Rainy conditions outside. Best for indoor board games, watching a movie, or visiting an indoor bookstore/library.");

            itinerary.put("evening",
                    "🌙 Evening (5 PM - 8 PM): Rain slows down. Take a peaceful umbrella walk on paved streets and enjoy hot street food snacks.");

        } else if (temp < 18) {

            itinerary.put("morning",
                    "🌅 Early Morning (6 AM - 9 AM): Crisp cool weather! Great for an energizing jog or warm-up exercise with a light jacket.");

            itinerary.put("afternoon",
                    "🌤️ Afternoon (12 PM - 4 PM): Pleasant sunshine. Perfect time for sightseeing, historical monuments, or outdoor photography.");

            itinerary.put("evening",
                    "🌙 Evening (5 PM - 8 PM): Getting chilly. Enjoy a campfire, warm soup, or a cozy dinner at an open-air restaurant.");

        } else {

            itinerary.put("morning",
                    "🌅 Early Morning (6 AM - 9 AM): Perfect balanced weather! Ideal for outdoor cycling, jogging, or yoga in the park.");

            itinerary.put("afternoon",
                    "☀️ Afternoon (12 PM - 4 PM): Comfortable conditions. Good for exploring local markets, city tour, or casual outdoor cafe hopping.");

            itinerary.put("evening",
                    "🌙 Evening (5 PM - 8 PM): Beautiful evening breeze. Perfect for a long drive, beach walk, or hanging out with friends outdoors.");
        }

        return itinerary;
    }

    // UPDATED CITY SEARCH / GEOCODING METHOD
    private JsonNode geocodeCity(String encodedCity) throws Exception {

        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                + encodedCity + "&count=10&language=en&format=json";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(geoUrl))
                .header("User-Agent", "WeatherForecastApp/1.0")
                .GET()
                .build();

        HttpResponse<String> res = httpClient.send(
                req, HttpResponse.BodyHandlers.ofString());

        System.out.println("Geocoding HTTP Status: " + res.statusCode());
        System.out.println("Geocoding API Response: " + res.body());

        if (res.statusCode() != 200) {
            throw new RuntimeException(
                    "City search API failed. HTTP Status: "
                            + res.statusCode() + " Response: " + res.body());
        }

        JsonNode root = mapper.readTree(res.body());
        JsonNode results = root.path("results");

        if (!results.isArray() || results.isEmpty()) {
            throw new IllegalArgumentException(
                    "City not found: " + encodedCity
                            + ". API Response: " + res.body());
        }

        return results.get(0);
    }

    // WEATHER CODE CONVERSION
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