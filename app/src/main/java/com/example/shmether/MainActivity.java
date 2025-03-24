package com.example.shmether;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText user_field;
    private Button second_button;
    private TextView result_info;

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

        second_button.setOnClickListener(v -> {
            String cityInput = user_field.getText().toString().trim();
            if (cityInput.isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.no_user_input, Toast.LENGTH_LONG).show();
                return;
            }
            checkCityValidity(cityInput);
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
                            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                            intent.putExtra("USER_INPUT", city);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
