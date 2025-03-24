package com.example.shmether;

import static com.example.shmether.NetworkUtils.fetchUrl;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecondActivity extends AppCompatActivity {

    private TextView city;
    private TextView forecast_info;
    private TextView day;
    private String formattedDate;
    private String apiType;
    private Button left_btn;
    private Button right_btn;
    private int countDay;
    private String cityName;
    public ImageView weatherIcon;
    public RelativeLayout main;

    String weatherDescription = "";

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);

        countDay = 0;
        day = findViewById(R.id.day);
        city = findViewById(R.id.city);
        forecast_info = findViewById(R.id.forecast_info);
        left_btn = findViewById(R.id.left_btn);
        right_btn = findViewById(R.id.right_btn);
        weatherIcon = findViewById(R.id.weatherIcon);
        main = findViewById(R.id.main);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String userInput = extras.getString("USER_INPUT");
            cityName = userInput;
            city.setText(cityName);

            String key = "7f75ba93d702ac5eaa4d315f8d1bba91";

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            formattedDate = sdf.format(calendar.getTime());
            day.setText(formattedDate);

            fetchCurrentWeatherAndForecast(cityName, key);

            left_btn.setOnClickListener(v -> {
                if (countDay > 0) {
                    countDay--;
                    updateDateAndFetchData(cityName, key);
                }
            });

            right_btn.setOnClickListener(v -> {
                if (countDay < 4) {
                    countDay++;
                    updateDateAndFetchData(cityName, key);
                }
            });
        }
    }

    private void updateDateAndFetchData(String cityName, String key) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, countDay);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        formattedDate = sdf.format(calendar.getTime());
        day.setText(formattedDate);

        if (countDay == 0) {
            fetchCurrentWeatherAndForecast(cityName, key);
        } else {
            apiType = "forecast";
            String url = "https://api.openweathermap.org/data/2.5/" + apiType + "?q="
                    + cityName + "&appid=" + key + "&units=metric&lang=ru";
            nextDay(url);
        }
    }

    @SuppressLint("ResourceAsColor")
    private void nextDay(String urlString) {
        forecast_info.setText("Ожидайте...");
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(() -> {
            String finalResult = fetchUrl(urlString);
            handler.post(() -> {
                if (finalResult != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(finalResult);
                        JSONArray list = jsonObject.getJSONArray("list");
                        JSONObject forecastNoon = null;
                        JSONObject forecastMidnight = null;
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.DAY_OF_YEAR, countDay);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        String targetDate = sdf.format(calendar.getTime());

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject forecast = list.getJSONObject(i);
                            String dtTxt = forecast.getString("dt_txt");
                            if (dtTxt.startsWith(targetDate) && dtTxt.endsWith("12:00:00")) {
                                forecastNoon = forecast;
                            }
                            if (dtTxt.startsWith(targetDate) && dtTxt.endsWith("00:00:00")) {
                                forecastMidnight = forecast;
                            }
                            if (forecastNoon != null && forecastMidnight != null) {
                                break;
                            }
                        }

                        StringBuilder forecastOutput = new StringBuilder();
                        if (forecastMidnight != null) {
                            JSONObject mainMidnight = forecastMidnight.getJSONObject("main");
                            JSONObject weatherMidnight = forecastMidnight.getJSONArray("weather").getJSONObject(0);
                            forecastOutput.append("Погода ночью:\n")
                                    .append(weatherMidnight.getString("description"))
                                    .append("\nТемпература: ").append(mainMidnight.getInt("temp")).append("℃")
                                    .append("\nОщущается как: ").append(mainMidnight.getInt("feels_like")).append("℃\n\n");
                        }

                        if (forecastNoon != null) {
                            JSONObject mainNoon = forecastNoon.getJSONObject("main");
                            JSONObject weatherNoon = forecastNoon.getJSONArray("weather").getJSONObject(0);
                            forecastOutput.append("Погода днём:\n")
                                    .append(weatherNoon.getString("description"))
                                    .append("\nТемпература: ").append(mainNoon.getInt("temp")).append("℃")
                                    .append("\nОщущается как: ").append(mainNoon.getInt("feels_like")).append("℃\n\n");

                            try{
                                weatherDescription = forecastNoon.getJSONArray("weather").getJSONObject(0).getString("description");
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        weatherCase(weatherDescription.toLowerCase());


                        forecast_info.setText(forecastOutput.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        forecast_info.setText("Ошибка обработки данных");
                    }
                } else {
                    forecast_info.setText("Ошибка подключения");
                }
            });
        });
    }

    private void fetchCurrentWeatherAndForecast(String cityName, String key) {
        String currentUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName +
                "&appid=" + key + "&units=metric&lang=ru";
        String forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + cityName +
                "&appid=" + key + "&units=metric&lang=ru";

        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(() -> {
            String currentResult = fetchUrl(currentUrl);
            String forecastResult = fetchUrl(forecastUrl);
            handler.post(() -> {
                StringBuilder combinedText = new StringBuilder();
                if (currentResult != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(currentResult);
                        JSONObject weatherObject = jsonObject.getJSONArray("weather").getJSONObject(0);
                        String currentInfo = "Текущая погода: " + weatherObject.getString("description") +
                                "\nТемпература: " + jsonObject.getJSONObject("main").getInt("temp") + "℃" +
                                "\nОщущается как: " + jsonObject.getJSONObject("main").getInt("feels_like") + "℃\n\n";
                        combinedText.append(currentInfo);

                        try{
                            weatherDescription = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                    } catch (JSONException e) {
                        combinedText.append("Ошибка обработки данных текущей погоды\n\n");
                    }
                } else {
                    combinedText.append("Ошибка подключения для текущей погоды\n\n");
                }

                forecast_info.setText(combinedText.toString());

                if (forecastResult != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(forecastResult);
                        JSONArray list = jsonObject.getJSONArray("list");
                        JSONObject forecastNoon = null;
                        JSONObject forecastMidnight = null;
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        String todayDate = sdf.format(calendar.getTime());
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject forecast = list.getJSONObject(i);
                            String dtTxt = forecast.getString("dt_txt");
                            if (dtTxt.startsWith(todayDate) && dtTxt.endsWith("12:00:00")) {
                                forecastNoon = forecast;
                            }
                            if (dtTxt.startsWith(todayDate) && dtTxt.endsWith("00:00:00")) {
                                forecastMidnight = forecast;
                            }
                            if (forecastNoon != null && forecastMidnight != null) {
                                break;
                            }
                        }
                        if (forecastMidnight != null) {
                            JSONObject mainMidnight = forecastMidnight.getJSONObject("main");
                            JSONObject weatherMidnight = forecastMidnight.getJSONArray("weather").getJSONObject(0);
                            combinedText.append("Погода ночью:\n")
                                    .append(weatherMidnight.getString("description"))
                                    .append("\nТемпература: ").append(mainMidnight.getInt("temp")).append("℃")
                                    .append("\nОщущается как: ").append(mainMidnight.getInt("feels_like")).append("℃\n\n");
                        }
                        if (forecastNoon != null) {
                            JSONObject mainNoon = forecastNoon.getJSONObject("main");
                            JSONObject weatherNoon = forecastNoon.getJSONArray("weather").getJSONObject(0);
                            combinedText.append("Погода днём:\n")
                                    .append(weatherNoon.getString("description"))
                                    .append("\nТемпература: ").append(mainNoon.getInt("temp")).append("℃")
                                    .append("\nОщущается как: ").append(mainNoon.getInt("feels_like")).append("℃\n\n");
                        }

                        weatherCase(weatherDescription.toLowerCase());

                    } catch (JSONException e) {
                        combinedText.append("Ошибка обработки данных прогноза");
                    }
                } else {
                    combinedText.append("Ошибка подключения для прогноза");
                }
            });
        });
    }

    private void weatherCase(String s){
        switch (s){
            case "ясно":
                weatherIcon.setImageResource(R.drawable.clear);
                main.setBackgroundResource(R.color.clearBg);
                break;
            case "снег":
                weatherIcon.setImageResource(R.drawable.snow);
                break;
            case "пасмурно":
            case "облачно с прояснениями":
            case "небольшая облачность":
                weatherIcon.setImageResource(R.drawable.cloudy);
                main.setBackgroundResource(R.color.cloudBg);
                break;
            case "небольшой дождь":
            case "дождь":
                weatherIcon.setImageResource(R.drawable.rain);
                main.setBackgroundResource(R.color.rainBg);
                break;
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
