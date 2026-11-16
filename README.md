# 🌦️ WeatherCast Java – 7-Day Weather Forecast & Analytics

A web-based weather forecasting application developed using **Java, Spring Boot, Thymeleaf, and SQLite**. WeatherCast provides real-time weather information, 7-day weather forecasts, city comparisons, search history, and intelligent weather-based recommendations through an interactive web interface.

## 🌐 Live Demo

**Live Application:** https://weather-forecast-java.onrender.com

**GitHub Repository:** https://github.com/syuvasri717sambath/Weather-forecast-java

---

## 📌 Project Overview

WeatherCast Java is designed to provide users with accurate and accessible weather information for different cities.

The application integrates external weather APIs to retrieve current weather conditions and forecast data. It also provides temperature analytics, weather-based recommendations, itinerary suggestions, and historical search information.

The application follows a Spring Boot MVC architecture, with Thymeleaf used for dynamic page rendering and SQLite used for persistent weather history storage.

## ✨ Key Features

- **Current Weather:** Displays temperature, humidity, wind speed, and weather conditions.
- **7-Day Weather Forecast:** Provides daily maximum and minimum temperatures, rainfall probability, and weather descriptions.
- **Weather Analytics:** Interactive temperature comparison charts using Chart.js.
- **Compare Cities:** Allows users to compare weather conditions across different cities.
- **Search History:** Stores and displays previously searched weather information.
- **Smart Weather Recommendations:** Generates recommendations based on temperature and rainfall conditions.
- **Weather-Based Itinerary:** Suggests suitable morning, afternoon, and evening activities according to weather conditions.
- **Admin Panel:** Provides access to the application's administration page.
- **Responsive Interface:** User-friendly interface designed with Bootstrap.

## 🛠️ Technology Stack

| Category | Technologies |
|---|---|
| Programming Language | Java 21 |
| Backend Framework | Spring Boot 3.3.4 |
| Frontend | HTML5, CSS3, JavaScript |
| UI Framework | Bootstrap 5 |
| Template Engine | Thymeleaf |
| Database | SQLite |
| ORM | Spring Data JPA, Hibernate |
| Data Visualization | Chart.js |
| Weather API | Open-Meteo API |
| Additional Weather API | OpenWeatherMap |
| Build Tool | Apache Maven |
| Containerization | Docker |
| Version Control | Git & GitHub |
| Cloud Deployment | Render |

## 🏗️ Application Architecture

The application follows a layered Spring Boot architecture.

```text
              USER
                |
                v
       FRONTEND INTERFACE
       HTML / CSS / Bootstrap
                |
                v
       THYMELEAF TEMPLATES
                |
                v
       SPRING BOOT CONTROLLERS
                |
                v
          SERVICE LAYER
                |
          +-----+------+
          |            |
          v            v
      WEATHER API    JPA LAYER
          |            |
          v            v
      Open-Meteo     SQLite
      OpenWeatherMap
```

## 📂 Project Structure

```text
Weather-forecast-java/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/weather/
│       │       │
│       │       ├── WeatherApplication.java
│       │       │
│       │       ├── controller/
│       │       │
│       │       ├── model/
│       │       │
│       │       ├── repository/
│       │       │
│       │       ├── service/
│       │       │   └── WeatherService.java
│       │       │
│       │       └── config/
│       │
│       └── resources/
│           ├── templates/
│           │   ├── index.html
│           │   ├── forecast_7day.html
│           │   ├── compare.html
│           │   ├── history.html
│           │   ├── admin.html
│           │   └── login.html
│           │
│           └── application.properties
│
├── Dockerfile
├── pom.xml
├── .gitignore
└── README.md
```

*Note: This represents the main application structure. Additional files and packages may exist in the repository.*

## ⚙️ Prerequisites

Make sure the following software is installed:

- Java Development Kit (JDK) 21
- Apache Maven (or use the Maven Wrapper if included)
- Git
- IntelliJ IDEA, Eclipse, or Visual Studio Code
- Internet connection for weather API access

## 🚀 Installation and Setup

### 1. Clone the Repository

```bash
git clone https://github.com/syuvasri717sambath/Weather-forecast-java.git
```

### 2. Navigate to the Project Folder

```bash
cd Weather-forecast-java
```

### 3. Configure the Weather API

The application uses Open-Meteo for geocoding and 7-day forecast data.

Open-Meteo does not require an API key for its free non-commercial API.

An optional OpenWeatherMap API key can be configured for current weather.

Set the environment variable:

**Windows PowerShell:**

```powershell
$env:WEATHER_API_KEY="YOUR_API_KEY"
```

Do not commit API keys or other credentials to GitHub.

### 4. Build the Application

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

### 6. Open in Your Browser

```text
http://localhost:8080
```

## 🌍 External API Integration

### Open-Meteo API

Used for:

- City geocoding and location coordinates.
- Current weather fallback data.
- Seven-day weather forecasts.
- Daily maximum and minimum temperatures.
- Precipitation probability.

Official Website: https://open-meteo.com/

### OpenWeatherMap API

Used as an optional provider for current weather information.

Official Website: https://openweathermap.org/api

**API Usage Note:** Free weather API services have request limits and usage conditions. Excessive requests may result in HTTP 429 responses. Caching and request optimization are recommended for production deployments.

## 📊 Weather Analytics

WeatherCast uses Chart.js to display forecast temperature trends.

The analytics include:

- Daily maximum temperature.
- Daily minimum temperature.
- Seven-day temperature comparison.
- Visual representation of temperature changes.

## 🐳 Docker Deployment

Build the Docker image:

```bash
docker build -t weathercast-java .
```

Run the container:

```bash
docker run -p 8080:8080 weathercast-java
```

Make sure the application is configured to listen on the required deployment port.

## ☁️ Cloud Deployment

The application is deployed using **Render**.

Live URL:

https://weather-forecast-java.onrender.com

Deployment workflow:

1. Push the source code to GitHub.
2. Connect the GitHub repository to Render.
3. Configure the Java/Docker build and start settings.
4. Add required environment variables securely.
5. Deploy the application.
6. Access the live application through the Render URL.

## 🔒 Security and Reliability

- External API credentials are configured through environment variables.
- Weather data is retrieved through HTTP requests.
- Spring Data JPA manages database operations.
- Input validation is used for city searches.
- API responses are checked before processing.
- External API rate limits should be handled appropriately.

## 🔮 Future Enhancements

- Implement forecast caching to reduce unnecessary API requests.
- Add weather maps and interactive location visualization.
- Introduce severe-weather alerts and notifications.
- Add user accounts and personalized dashboards.
- Improve multi-city weather analytics.
- Integrate additional weather data providers.
- Enhance mobile responsiveness and accessibility.

## 🎯 Project Objective

The primary objective of WeatherCast Java is to develop a reliable, interactive, and user-friendly weather forecasting platform using modern Java technologies and external weather APIs.

The project demonstrates practical implementation of REST API integration, Spring Boot application development, database connectivity, dynamic web rendering, data visualization, and cloud deployment.

## 👩‍💻 Developer

**Developed as a Java Full Stack Development Project.**

**Technologies:** Java | Spring Boot | Thymeleaf | SQLite | REST APIs | Bootstrap | Docker

---

⭐ If you find this project useful, consider giving the repository a star on GitHub!

**Repository:** https://github.com/syuvasri717sambath/Weather-forecast-java
