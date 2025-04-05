package com.example.shmether;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText user_field;
    private Button second_button;
    private TextView result_info;
    private ListView cityListView;

    private ArrayAdapter<String> adapter;
    private List<String> formattedCities;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user_field = findViewById(R.id.user_field);
        result_info = findViewById(R.id.result_info);
        second_button = findViewById(R.id.next_page_btn);
        cityListView = findViewById(R.id.cityListView);

        second_button.setOnClickListener(v -> {
            String cityInput = user_field.getText().toString().trim();
            if (cityInput.isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.no_user_input, Toast.LENGTH_LONG).show();
                return;
            }
            checkCityValidity(cityInput);
        });

        // Загрузка и отображение списка городов
        formattedCities = new ArrayList<>();
        for (String c : getSavedCities()) {
            formattedCities.add(formatCityName(c));
        }

        adapter = new ArrayAdapter<>(this, R.layout.list_item_city, R.id.cityNameText, formattedCities);
        cityListView.setAdapter(adapter);

        cityListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = formattedCities.get(position);
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            intent.putExtra("USER_INPUT", selectedCity);
            startActivity(intent);
        });
    }

    private void checkCityValidity(String city) {
        String key = "7f75ba93d702ac5eaa4d315f8d1bba91";
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city +
                "&appid=" + key + "&units=metric&lang=ru";

        Handler handler = new Handler(Looper.getMainLooper());

        executorService.execute(() -> {
            String result = NetworkUtils.fetchUrl(url);
            handler.post(() -> {
                if (result != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        int cod = jsonObject.optInt("cod", 0);
                        if (cod == 200) {
                            String formattedCity = formatCityName(city);
                            SharedPreferences prefs = getSharedPreferences("cities", MODE_PRIVATE);
                            Set<String> currentSet = prefs.getStringSet("city_list", new HashSet<>());
                            LinkedHashSet<String> citiesSet = new LinkedHashSet<>(currentSet);

                            // Удаляем город если он уже есть (для перемещения в конец)
                            citiesSet.remove(formattedCity);
                            citiesSet.add(formattedCity);

                            // Ограничение на 5 городов
                            if (citiesSet.size() > 5) {
                                String oldest = citiesSet.iterator().next();
                                citiesSet.remove(oldest);
                            }

                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putStringSet("city_list", citiesSet);
                            editor.apply();

                            formattedCities.remove(formattedCity);
                            formattedCities.add(0, formattedCity);

                            if (formattedCities.size() > 5) {
                                formattedCities.remove(formattedCities.size() - 1);
                            }

                            adapter.notifyDataSetChanged();

                            // Переход на экран с прогнозом
                            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                            intent.putExtra("USER_INPUT", formattedCity);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "Такого города нет ;(", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Ошибка обработки данных", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Ошибка подключения", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private List<String> getSavedCities() {
        SharedPreferences prefs = getSharedPreferences("cities", MODE_PRIVATE);
        Set<String> citiesSet = prefs.getStringSet("city_list", new HashSet<>());
        List<String> list = new ArrayList<>(citiesSet);
        java.util.Collections.reverse(list);
        return list;

    }

    private String formatCityName(String city) {
        if (city == null || city.isEmpty()) return city;

        String[] parts = city.trim().toLowerCase().split("[-\\s]");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].length() == 0) continue;
            formatted.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1));
            if (i < parts.length - 1) {
                // Сохраняем разделитель
                char sep = city.contains("-") ? '-' : ' ';
                formatted.append(sep);
            }
        }

        return formatted.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
