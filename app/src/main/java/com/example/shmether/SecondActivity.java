package com.example.shmether;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SecondActivity extends AppCompatActivity {

    private TextView city;
    private TextView forecast_info;

    private TextView day;

    private String formattedDate;
    private String progDate;

    private String apiType;

    private Button left_btn;
    private Button right_btn;

    private int countDay;


    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);

        // Инициализация элементов интерфейса
        countDay = 0;
        day = findViewById(R.id.day);
        city = findViewById(R.id.city);
        forecast_info = findViewById(R.id.forecast_info);
        left_btn = findViewById(R.id.left_btn);
        right_btn = findViewById(R.id.right_btn);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String userInput = extras.getString("USER_INPUT");
            String city = userInput.toString();

            String key = "7f75ba93d702ac5eaa4d315f8d1bba91";

            // Устанавливаем текущую дату
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            formattedDate = sdf.format(calendar.getTime());
            day.setText(formattedDate);

            fetchCurrentWeatherAndForecast(city, key);



            // Настройка кнопок
            left_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (countDay>0){
                        countDay -= 1;
                        updateDateAndFetchData(city, key);
                    }
                }
            });

            right_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    countDay += 1;
                    updateDateAndFetchData(city, key);
                }
            });
        }
    }

    // Метод для обновления даты и загрузки данных
    private void updateDateAndFetchData(String city, String key) {
        // Обновляем дату
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, countDay); // Добавляем/вычитаем дни
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        formattedDate = sdf.format(calendar.getTime());
        day.setText(formattedDate);

        if (countDay == 0) {
            // Если countDay = 0, загружаем текущую погоду
            fetchCurrentWeatherAndForecast(city, key);
        } else {
            // Иначе загружаем прогноз на другой день
            apiType = "forecast";
            String url = "https://api.openweathermap.org/data/2.5/" + apiType + "?q="
                    + city + "&appid=" + key + "&units=metric&lang=ru";
            nextDay(url);
        }
    }


        @SuppressLint("SetTextI18n")
        private void nextDay(String urlString) {

            forecast_info.setText("Ожидайте...");
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {


                String finalResult = fetchUrl(urlString);

                handler.post(() -> {
                    if (finalResult != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(finalResult);
                            JSONArray list = jsonObject.getJSONArray("list");

                            JSONObject forecastNoon = null;
                            JSONObject forecastMidnight = null;

                            Calendar calendar = Calendar.getInstance();
                            calendar.add(Calendar.DAY_OF_YEAR, countDay); // Add one day
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            String tomorrowDate = sdf.format(calendar.getTime());

                            for (int i = 0; i < list.length(); i++) {
                                JSONObject forecast = list.getJSONObject(i);
                                String dtTxt = forecast.getString("dt_txt");

                                if (dtTxt.startsWith(tomorrowDate) && dtTxt.endsWith("12:00:00")) {
                                    forecastNoon = forecast;
                                }

                                if (dtTxt.startsWith(tomorrowDate) && dtTxt.endsWith("21:00:00")) {
                                    forecastMidnight = forecast;
                                }

                                if (forecastNoon != null && forecastMidnight != null) {
                                    break;
                                }
                            }

                            StringBuilder forecastOutput = new StringBuilder();
                            if (forecastNoon != null) {
                                JSONObject mainNoon = forecastNoon.getJSONObject("main");
                                JSONObject weatherNoon = forecastNoon.getJSONArray("weather").getJSONObject(0);

                                forecastOutput.append("Погода днём:\n")
                                        .append("Погода: ").append(weatherNoon.getString("description"))
                                        .append("\nТемпература: ").append(mainNoon.getInt("temp")).append("℃")
                                        .append("\nОщущается как: ").append(mainNoon.getInt("feels_like")).append("℃\n\n");
                            } else {
                                forecastOutput.append("Дневной прогноз не найден.\n\n");
                            }

                            if (forecastMidnight != null) {
                                JSONObject mainMidnight = forecastMidnight.getJSONObject("main");
                                JSONObject weatherMidnight = forecastMidnight.getJSONArray("weather").getJSONObject(0);

                                forecastOutput.append("Погода ночью:\n")
                                        .append("Погода: ").append(weatherMidnight.getString("description"))
                                        .append("\nТемпература: ").append(mainMidnight.getInt("temp")).append("℃")
                                        .append("\nОщущается как: ").append(mainMidnight.getInt("feels_like")).append("℃\n");
                            } else {
                                forecastOutput.append("Ночной прогноз не найден.");
                            }

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

        @SuppressLint("SetTextI18n")
        private void fetchCurrentWeatherAndForecast(String city, String key) {
            String currentUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + city +
                    "&appid=" + key + "&units=metric&lang=ru";
            String forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + city +
                    "&appid=" + key + "&units=metric&lang=ru";

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                String currentResult = fetchUrl(currentUrl);
                String forecastResult = fetchUrl(forecastUrl);

                handler.post(() -> {
                    StringBuilder combinedText = new StringBuilder();
                    // Обработка текущей погоды
                    if (currentResult != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(currentResult);
                            JSONObject weatherObject = jsonObject.getJSONArray("weather").getJSONObject(0);
                            String currentInfo = "Текущая погода: " + weatherObject.getString("description") +
                                    "\nТемпература: " + jsonObject.getJSONObject("main").getInt("temp") + "℃" +
                                    "\nОщущается как: " + jsonObject.getJSONObject("main").getInt("feels_like") + "℃\n\n";
                            combinedText.append(currentInfo);
                        } catch (JSONException e) {
                            combinedText.append("Ошибка обработки данных текущей погоды\n\n");
                        }
                    } else {
                        combinedText.append("Ошибка подключения для текущей погоды\n\n");
                    }

                    // Обработка прогноза на сегодня (12:00 и 00:00)
                    if (forecastResult != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(forecastResult);
                            JSONArray list = jsonObject.getJSONArray("list");

                            JSONObject forecastNoon = null;
                            JSONObject forecastMidnight = null;

                            // Получаем сегодняшнюю дату в формате "yyyy-MM-dd"
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            String todayDate = sdf.format(calendar.getTime());

                            for (int i = 0; i < list.length(); i++) {
                                JSONObject forecast = list.getJSONObject(i);
                                String dtTxt = forecast.getString("dt_txt");

                                if (dtTxt.startsWith(todayDate) && dtTxt.endsWith("12:00:00")) {
                                    forecastNoon = forecast;
                                }
                                if (dtTxt.startsWith(todayDate) && dtTxt.endsWith("21:00:00")) {
                                    forecastMidnight = forecast;
                                }
                                if (forecastNoon != null && forecastMidnight != null) {
                                    break;
                                }
                            }

                            if (forecastNoon != null) {
                                JSONObject mainNoon = forecastNoon.getJSONObject("main");
                                JSONObject weatherNoon = forecastNoon.getJSONArray("weather").getJSONObject(0);
                                combinedText.append("Погода днём:\n")
                                        .append("Погода: ").append(weatherNoon.getString("description"))
                                        .append("\nТемпература: ").append(mainNoon.getInt("temp")).append("℃")
                                        .append("\nОщущается как: ").append(mainNoon.getInt("feels_like")).append("℃\n\n");
                            } else {
                                combinedText.append("Дневной прогноз не найден.\n\n");
                            }

                            if (forecastMidnight != null) {
                                JSONObject mainMidnight = forecastMidnight.getJSONObject("main");
                                JSONObject weatherMidnight = forecastMidnight.getJSONArray("weather").getJSONObject(0);
                                combinedText.append("Погода ночью:\n")
                                        .append("Погода: ").append(weatherMidnight.getString("description"))
                                        .append("\nТемпература: ").append(mainMidnight.getInt("temp")).append("℃")
                                        .append("\nОщущается как: ").append(mainMidnight.getInt("feels_like")).append("℃");
                            } else {
                                combinedText.append("Ночной прогноз не найден.");
                            }

                        } catch (JSONException e) {
                            combinedText.append("Ошибка обработки данных прогноза");
                        }
                    } else {
                        combinedText.append("Ошибка подключения для прогноза");
                    }

                    forecast_info.setText(combinedText.toString());
                });
            });
        }

    // Вспомогательный метод для синхронного запроса данных по URL
    private String fetchUrl(String urlString) {
        String result;
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }
            result = buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
            result = null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}


