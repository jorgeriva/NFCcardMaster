package com.example.nfccardmaster;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.xml.activity_main);

        // Referencia al botón en el layout
        Button btnReadCards = findViewById(R.id.btn_read_cards);

        // Configurar el OnClickListener para el botón
        btnReadCards.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navegar a LeerNFCActivity
                Intent intent = new Intent(MainActivity.this, LeerNFCActivity.class);
                startActivity(intent);
            }
        });
    }
}
