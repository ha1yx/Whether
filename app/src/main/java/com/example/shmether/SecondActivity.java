package com.example.shmether;

import static com.example.shmether.NetworkUtils.fetchUrl;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    public RelativeLayout main;
    private TextView city, day;
    private Button left_btn, right_btn;
    private int countDay;
    private String cityName;
    private String formattedDate;

    private LinearLayout containerToday, containerFuture;
    private TextView degreesInfo;
    private ImageView todayWeatherIcon;
    private TextView todayWeatherInfo;

    private ImageView weatherIconMor, weatherIconDay, weatherIconEve, weatherIconNig;
    private TextView forecast_info, forecast_infoMor, forecast_infoDay, forecast_infoEve, forecast_infoNig;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final String key = "7f75ba93d702ac5eaa4d315f8d1bba91";

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);

        main = findViewById(R.id.main);
        city = findViewById(R.id.city);
        day = findViewById(R.id.day);
        left_btn = findViewById(R.id.left_btn);
        right_btn = findViewById(R.id.right_btn);

        containerToday = findViewById(R.id.containerToday);
        containerFuture = findViewById(R.id.containerFuture);

        degreesInfo = findViewById(R.id.degreesInfo);
        todayWeatherIcon = findViewById(R.id.todayWeatherIcon);
        todayWeatherInfo = findViewById(R.id.todayWeatherInfo);

        weatherIconMor = findViewById(R.id.weatherIconMor);
        forecast_infoMor = findViewById(R.id.forecast_infoMor);
        weatherIconDay = findViewById(R.id.weatherIconDay);
        forecast_infoDay = findViewById(R.id.forecast_infoDay);
        weatherIconEve = findViewById(R.id.weatherIconEve);
        forecast_infoEve = findViewById(R.id.forecast_infoEve);
        weatherIconNig = findViewById(R.id.weatherIconNig);
        forecast_infoNig = findViewById(R.id.forecast_infoNig);
        forecast_info = findViewById(R.id.forecast_info);

        countDay = 0;

        if (getIntent().hasExtra("USER_INPUT")) {
            cityName = getIntent().getStringExtra("USER_INPUT");
            city.setText(cityName);

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            formattedDate = sdf.format(calendar.getTime());
            day.setText(formattedDate);

            updateUI();
        }

        left_btn.setOnClickListener(v -> {
            if (countDay > 0) {
                countDay--;
                updateUI();
            }
        });

        right_btn.setOnClickListener(v -> {
            if (countDay < 4) {
                countDay++;
                updateUI();
            }
        });
    }

    private void updateUI() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, countDay);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        formattedDate = sdf.format(calendar.getTime());
        day.setText(formattedDate);

        if (countDay == 0) {
            // Для сегодняшнего дня показываем и текущую погоду, и прогноз по частям дня
            containerToday.setVisibility(View.VISIBLE);
            containerFuture.setVisibility(View.VISIBLE);
            forecast_info.setVisibility(View.GONE);
            loadTodayWeather();
            loadFutureWeather(0);
        } else {
            // Для будущих дней показываем только прогноз и общее текстовое поле
            containerToday.setVisibility(View.GONE);
            containerFuture.setVisibility(View.VISIBLE);
            forecast_info.setVisibility(View.VISIBLE);
            loadFutureWeather(countDay);
        }
    }

    private void loadTodayWeather() {
        String currentUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName +
                "&appid=" + key + "&units=metric&lang=ru";
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(() -> {
            String result = fetchUrl(currentUrl);
            handler.post(() -> {
                if (result != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        JSONObject mainObj = jsonObject.getJSONObject("main");
                        JSONArray weatherArray = jsonObject.getJSONArray("weather");
                        JSONObject weatherObj = weatherArray.getJSONObject(0);

                        int temp = mainObj.getInt("temp");
                        int feels_like = mainObj.getInt("feels_like");
                        String description = weatherObj.getString("description");
                        description = description.substring(0, 1).toUpperCase() + description.substring(1);

                        // Получаем объект "wind" из jsonObject (на одном уровне с "main")
                        JSONObject windObj = jsonObject.getJSONObject("wind");
                        double windSpeed = windObj.getDouble("speed");
                        int rounndWind = (int) Math.round(windSpeed);
                        double pressure = mainObj.getInt("pressure") * 0.75;
                        int roundPressure = (int) Math.round(pressure);

                        String info = description +
//                                "\nТемпература " + temp + "℃" +
                                "\nОщущается как " + feels_like + "℃" +
                                "\nСкорость ветра " + rounndWind + " м/с" +
                                "\nДавление " + roundPressure + " мм.рт.ст";
                        String tempInf;
                        if (temp > 0)
                            tempInf = "+" + temp;
                        else
                            tempInf = String.valueOf(temp);
                        degreesInfo.setText(tempInf);
                        todayWeatherInfo.setText(info);

                        todayWeatherIcon.setImageResource(getWeatherIconResource(description));
                        updateBackground(description);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        todayWeatherInfo.setText("Ошибка обработки данных");
                    }
                } else {
                    todayWeatherInfo.setText("Ошибка подключения");
                }
            });
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadFutureWeather(int dayOffset) {
        String forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + cityName +
                "&appid=" + key + "&units=metric&lang=ru";
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(() -> {
            String result = fetchUrl(forecastUrl);
            handler.post(() -> {
                if (result != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        JSONArray list = jsonObject.getJSONArray("list");

                        JSONObject forecastMor = null;
                        JSONObject forecastDay = null;
                        JSONObject forecastEve = null;
                        JSONObject forecastNig = null;

                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.DAY_OF_YEAR, dayOffset);
                        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
                        String targetDate = sdfDate.format(calendar.getTime());

                        // Перебираем прогнозы и выбираем записи по времени
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject forecast = list.getJSONObject(i);
                            String dtTxt = forecast.getString("dt_txt");
                            if (dtTxt.startsWith(targetDate)) {
                                if (dtTxt.endsWith("06:00:00") && forecastMor == null) {
                                    forecastMor = forecast;
                                } else if (dtTxt.endsWith("12:00:00") && forecastDay == null) {
                                    forecastDay = forecast;
                                } else if (dtTxt.endsWith("18:00:00") && forecastEve == null) {
                                    forecastEve = forecast;
                                } else if (dtTxt.endsWith("21:00:00") && forecastNig == null) {
                                    forecastNig = forecast;
                                }
                            }
                        }

                        // Обработка утреннего прогноза
                        if (forecastMor != null) {
                            JSONObject mainObj = forecastMor.getJSONObject("main");
                            JSONArray weatherArray = forecastMor.getJSONArray("weather");
                            JSONObject weatherObj = weatherArray.getJSONObject(0);
                            int temp = mainObj.getInt("temp");
                            // Показываем блок для утра
                            forecast_infoMor.setVisibility(View.VISIBLE);
                            weatherIconMor.setVisibility(View.VISIBLE);
                            forecast_infoMor.setText("Утро\n" + temp + "℃");
                            weatherIconMor.setImageResource(getWeatherIconResource(weatherObj.getString("description")));
                        } else {
                            // Если данных нет, скрываем блок
                            forecast_infoMor.setVisibility(View.GONE);
                            weatherIconMor.setVisibility(View.GONE);
                        }

                        // Обработка дневного прогноза
                        if (forecastDay != null) {
                            JSONObject mainObj = forecastDay.getJSONObject("main");
                            JSONArray weatherArray = forecastDay.getJSONArray("weather");
                            JSONObject weatherObj = weatherArray.getJSONObject(0);
                            int temp = mainObj.getInt("temp");
                            forecast_infoDay.setVisibility(View.VISIBLE);
                            weatherIconDay.setVisibility(View.VISIBLE);
                            forecast_infoDay.setText("День\n" + temp + "℃");
                            weatherIconDay.setImageResource(getWeatherIconResource(weatherObj.getString("description")));

                            // Если это не сегодняшний день (dayOffset != 0), выводим общую информацию в forecast_info
                            if (dayOffset != 0) {
                                JSONObject windObj = forecastDay.getJSONObject("wind");
                                double windSpeed = windObj.getDouble("speed");
                                int rounndWind = (int) Math.round(windSpeed);
                                double pressure = mainObj.getInt("pressure") * 0.75;
                                int roundPressure = (int) Math.round(pressure);

                                forecast_info.setVisibility(View.VISIBLE);
                                forecast_info.setText("Днем будет " + weatherObj.getString("description") +
                                        "\nСкорость ветра " + rounndWind + " м/с" +
                                        "\nДавление " + roundPressure + " мм.рт.ст");

                                updateBackground(weatherObj.getString("description"));
                            }
                        } else {
                            forecast_infoDay.setVisibility(View.GONE);
                            weatherIconDay.setVisibility(View.GONE);
                        }

                        // Обработка вечернего прогноза
                        if (forecastEve != null) {
                            JSONObject mainObj = forecastEve.getJSONObject("main");
                            JSONArray weatherArray = forecastEve.getJSONArray("weather");
                            JSONObject weatherObj = weatherArray.getJSONObject(0);
                            int temp = mainObj.getInt("temp");
                            forecast_infoEve.setVisibility(View.VISIBLE);
                            weatherIconEve.setVisibility(View.VISIBLE);
                            forecast_infoEve.setText("Вечер\n" + temp + "℃");
                            weatherIconEve.setImageResource(getWeatherIconResource(weatherObj.getString("description")));
                        } else {
                            forecast_infoEve.setVisibility(View.GONE);
                            weatherIconEve.setVisibility(View.GONE);
                        }

                        // Обработка ночного прогноза
                        if (forecastNig != null) {
                            JSONObject mainObj = forecastNig.getJSONObject("main");
                            JSONArray weatherArray = forecastNig.getJSONArray("weather");
                            JSONObject weatherObj = weatherArray.getJSONObject(0);
                            int temp = mainObj.getInt("temp");
                            forecast_infoNig.setVisibility(View.VISIBLE);
                            weatherIconNig.setVisibility(View.VISIBLE);
                            forecast_infoNig.setText("Ночь\n" + temp + "℃");
                            weatherIconNig.setImageResource(getWeatherIconResource(weatherObj.getString("description")));
                        } else {
                            forecast_infoNig.setVisibility(View.GONE);
                            weatherIconNig.setVisibility(View.GONE);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        forecast_infoMor.setText("Ошибка обработки");
                        forecast_infoDay.setText("Ошибка обработки");
                        forecast_infoEve.setText("Ошибка обработки");
                        forecast_infoNig.setText("Ошибка обработки");
                    }
                } else {
                    forecast_infoMor.setText("Ошибка подключения");
                    forecast_infoDay.setText("Ошибка подключения");
                    forecast_infoEve.setText("Ошибка подключения");
                    forecast_infoNig.setText("Ошибка подключения");
                }
            });
        });
    }

    private int getWeatherIconResource(String description) {
        description = description.toLowerCase();
        if (description.contains("ясно")) {
            return R.drawable.clear;
        } else if (description.contains("снег")) {
            return R.drawable.snow;
        } else if (description.contains("облачно") || description.contains("пасмурно")) {
            return R.drawable.cloudy;
        } else if (description.contains("дождь")) {
            return R.drawable.rain;
        }
        return R.drawable.clear;
    }

    private void updateBackground(String description) {
        description = description.toLowerCase();
        if (description.contains("ясно")) {
            main.setBackgroundResource(R.color.clearBg);
        } else if (description.contains("снег")) {
            main.setBackgroundResource(R.color.cloudBg);
        } else if (description.contains("облачно") || description.contains("пасмурно")) {
            main.setBackgroundResource(R.color.cloudBg);
        } else if (description.contains("дождь")) {
            main.setBackgroundResource(R.color.rainBg);
        } else {
            main.setBackgroundResource(R.color.clearBg);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
