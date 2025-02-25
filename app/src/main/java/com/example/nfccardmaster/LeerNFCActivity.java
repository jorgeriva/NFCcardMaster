package com.example.nfccardmaster;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

public class LeerNFCActivity extends AppCompatActivity {

    private static final String TAG = "LeerNFCActivity";
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private TextView textViewRFID;
    private TextToSpeech textToSpeech; // Variable para TextToSpeech

    @SuppressLint({"SetTextI18n", "ResourceType"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.xml.leernfcactivity); // Carga la interfaz desde un recurso XML

        textViewRFID = findViewById(R.id.LeerViewRFID);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Verifica si el dispositivo es compatible con NFC
        if (nfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta NFC", Toast.LENGTH_SHORT).show();
            finish(); // Cierra la actividad si no hay NFC
            return;
        }

        // Verifica si el NFC está activado
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "Por favor, habilita el NFC en la configuración del dispositivo", Toast.LENGTH_SHORT).show();
            finish(); // Cierra la actividad si NFC está deshabilitado
            return;
        }

        // Determina los flags para el PendingIntent dependiendo de la versión de Android
        int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                ? PendingIntent.FLAG_MUTABLE // Si el sistema es Android 12 (S) o superior, se requiere FLAG_MUTABLE
                : 0; // Para versiones anteriores, no se necesita

        // Se crea un PendingIntent que permite que la actividad maneje eventos NFC cuando la app está en primer plano
        pendingIntent = PendingIntent.getActivity(
                this, // Contexto de la aplicación
                0, // Identificador de la solicitud, en este caso no se usa y se deja en 0
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), // Intento para reabrir la misma actividad si ya está en ejecución
                flags // Parámetro de control de permisos y comportamiento del PendingIntent
        );

        // Botón para regresar a la pantalla principal
        Button backButton = findViewById(R.id.btn_back_to_main);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(LeerNFCActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Cierra la actividad actual
        });

        // Inicialización de TextToSpeech para lectura en voz alta
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Idioma no soportado para TextToSpeech.");
                }
            } else {
                Log.e(TAG, "Inicialización de TextToSpeech fallida.");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }

        // Detener TextToSpeech si la actividad está en pausa
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Liberar recursos de TextToSpeech
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            String rfid = obtenerRFID(tag);
            textViewRFID.setText(rfid);

            // Leer el texto en voz alta
            if (!rfid.isEmpty()) {
                textToSpeech.speak(rfid, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }
    }

    // Método para obtener el contenido de la etiqueta NFC
    public String obtenerRFID(Tag tag) {
        if (tag == null) {
            Toast.makeText(this, "No se detectó una etiqueta NFC", Toast.LENGTH_SHORT).show();
            return "";
        }

        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            Toast.makeText(this, "La etiqueta NFC no es compatible", Toast.LENGTH_SHORT).show();
            return "";
        }

        try {
            ndef.connect(); // Establece conexión con la etiqueta NFC
            NdefMessage ndefMessage = ndef.getNdefMessage(); // Obtiene el mensaje almacenado en la etiqueta
            if (ndefMessage != null) {
                NdefRecord[] records = ndefMessage.getRecords(); // Obtiene todos los registros almacenados en la etiqueta
                for (NdefRecord record : records) {
                    // Verifica si el registro es de tipo texto
                    if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                            Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                        byte[] payload = record.getPayload(); // Extrae el contenido del registro

                        // Determina la codificación del texto (UTF-8 o UTF-16)
                        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

                        // Determina la longitud del código de idioma
                        int languageCodeLength = payload[0] & 63;

                        // Extrae el texto almacenado en la etiqueta NFC
                        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
                    }
                }
            }
        } catch (IOException | FormatException e) {
            Log.e(TAG, "Error al leer la etiqueta NFC", e);
        } finally {
            try {
                ndef.close(); // Cierra la conexión NFC
            } catch (IOException e) {
                Log.e(TAG, "Error al cerrar la conexión NFC", e);
            }
        }

        return "";
    }
}
