package com.example.shmether;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText user_field;
    private Button main_btn;
    private Button second_button;
    private TextView result_info;


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


        second_button.setOnClickListener(new View.OnClickListener(){
           @Override
            public void onClick(View v){
               String userInput = user_field.getText().toString();
               // Создаем Intent для перехода на SecondActivity
               Intent intent = new Intent(MainActivity.this, SecondActivity.class);

               // Если нужно передать данные, используйте putExtra
               intent.putExtra("USER_INPUT", userInput);

               // Запускаем новую Activity
               startActivity(intent);
           }
        });
    }
}
