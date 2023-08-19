package com.example.myapplication20230129;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class SubActivity extends AppCompatActivity {

    private TextView textView;
    private Button goBackButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        textView = (TextView) findViewById(R.id.textView);
        goBackButton = (Button) findViewById(R.id.goBackButton);

        Intent intent = getIntent();
        String URL = intent.getStringExtra("URL");

        //URL = URL + "sort=-itemPrice&";

        textView.setText(URL);



        //絞り込み設定のボタンが押されたら
        goBackButton.setOnClickListener(view -> {
            Intent intent2 = new Intent(SubActivity.this, MainActivity.class);
            intent2.putExtra("URL",URL);
            finish();
        });
    }
}