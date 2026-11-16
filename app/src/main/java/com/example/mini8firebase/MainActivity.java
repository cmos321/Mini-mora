package com.example.mini8firebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MainActivity extends AppCompatActivity {

    // Servicio 6: dirección de la página publicada con Firebase Hosting
    // (cambia TU_ID_DE_PROYECTO por el ID de tu proyecto de Firebase)
    static final String URL_HOSTING = "https://mini-mora.web.app";

    // Declaración de los componentes de la interfaz
    TextView txtCorreo, txtContador, txtEstadoFoto, txtAnalytics;
    ImageView imgFoto;
    MaterialButton btnTareas, btnSumar, btnReiniciar, btnSubirFoto, btnEliminarFoto,
            btnToken, btnHosting, btnCrash, btnSalir;

    // Servicio 2: Realtime Database
    DatabaseReference contadorRef;
    ValueEventListener contadorListener;

    // Servicio 4: Cloud Storage (ruta de la foto: fotos_mini8/<uid>.jpg)
    StorageReference fotoRef;

    // Servicio 7: Analytics
    FirebaseAnalytics analytics;
    int eventosEnviados = 0;

    // Abre la galería y devuelve la imagen elegida
    ActivityResultLauncher<String> selectorImagen = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    subirFoto(uri);
                }
            });

    // Pide el permiso para mostrar notificaciones (Android 13 o superior)
    ActivityResultLauncher<String> pedirPermisoNotificaciones = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), concedido -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Servicio 3: si no hay sesión iniciada, se vuelve al login
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        txtCorreo = findViewById(R.id.txtCorreo);
        txtContador = findViewById(R.id.txtContador);
        txtEstadoFoto = findViewById(R.id.txtEstadoFoto);
        txtAnalytics = findViewById(R.id.txtAnalytics);
        imgFoto = findViewById(R.id.imgFoto);
        btnTareas = findViewById(R.id.btnTareas);
        btnSumar = findViewById(R.id.btnSumar);
        btnReiniciar = findViewById(R.id.btnReiniciar);
        btnSubirFoto = findViewById(R.id.btnSubirFoto);
        btnEliminarFoto = findViewById(R.id.btnEliminarFoto);
        btnToken = findViewById(R.id.btnToken);
        btnHosting = findViewById(R.id.btnHosting);
        btnCrash = findViewById(R.id.btnCrash);
        btnSalir = findViewById(R.id.btnSalir);

        // ajuste de márgenes del sistema (barras de estado y navegación)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtCorreo.setText("Sesión iniciada: " + usuario.getEmail());

        analytics = FirebaseAnalytics.getInstance(this);
        FirebaseCrashlytics.getInstance().setUserId(usuario.getUid()); // Crashlytics: quién tuvo el error
        fotoRef = FirebaseStorage.getInstance().getReference("fotos_mini8/" + usuario.getUid() + ".jpg");

        configurarNotificaciones();
        configurarContador();
        cargarFoto();

        // Servicio 1: Cloud Firestore
        btnTareas.setOnClickListener(v -> {
            registrarEvento("abrir_tareas");
            startActivity(new Intent(this, TareasActivity.class));
        });

        // Servicio 2: Realtime Database (suma 1 al contador compartido)
        btnSumar.setOnClickListener(v -> {
            registrarEvento("sumar_contador");
            if (contadorRef != null) {
                contadorRef.setValue(ServerValue.increment(1));
            }
        });
        btnReiniciar.setOnClickListener(v -> {
            if (contadorRef != null) {
                contadorRef.setValue(0);
            }
        });

        // Servicio 4: Cloud Storage
        btnSubirFoto.setOnClickListener(v -> {
            registrarEvento("subir_foto");
            selectorImagen.launch("image/*");
        });
        btnEliminarFoto.setOnClickListener(v -> eliminarFoto());

        // Servicio 5: Cloud Messaging (el token identifica este celular para enviarle notificaciones)
        btnToken.setOnClickListener(v -> {
            registrarEvento("copiar_token");
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(token -> {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText("token", token));
                        Toast.makeText(MainActivity.this, "Token copiado", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this,
                            "No se pudo obtener el token: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        // Servicio 6: Hosting (abre la página web publicada)
        btnHosting.setOnClickListener(v -> {
            registrarEvento("abrir_hosting");
            if (URL_HOSTING.contains("TU_ID_DE_PROYECTO")) {
                Toast.makeText(this, "Primero cambia URL_HOSTING en MainActivity por tu dirección .web.app",
                        Toast.LENGTH_LONG).show();
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_HOSTING)));
            }
        });

        // Servicio 8: Crashlytics (fuerza un error para ver el reporte en la consola)
        btnCrash.setOnClickListener(v -> {
            FirebaseCrashlytics.getInstance().log("El usuario probó Crashlytics");
            throw new RuntimeException("Prueba de Crashlytics");
        });

        // Servicio 3: Authentication (cerrar sesión)
        btnSalir.setOnClickListener(v -> {
            registrarEvento("cerrar_sesion");
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // Servicio 7: Analytics (cada vez que se usa un servicio se envía un evento)
    void registrarEvento(String nombre) {
        analytics.logEvent(nombre, null);
        eventosEnviados++;
        txtAnalytics.setText("7 · Analytics: " + eventosEnviados + " eventos enviados en esta sesión");
    }

    // Servicio 2: Realtime Database. Escucha el contador en tiempo real
    void configurarContador() {
        try {
            contadorRef = FirebaseDatabase.getInstance().getReference("contador");
            contadorListener = new ValueEventListener() {
                // Se ejecuta cada vez que cambia el valor en la base de datos
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Long valor = snapshot.getValue(Long.class);
                    txtContador.setText(String.valueOf(valor == null ? 0 : valor));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    txtContador.setText("Error");
                    Toast.makeText(MainActivity.this, "Realtime Database: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            };
            contadorRef.addValueEventListener(contadorListener);
        } catch (Exception e) {
            // Pasa si Realtime Database no está creada o falta la URL en google-services.json
            contadorRef = null;
            txtContador.setText("-");
            Toast.makeText(this, "Crea Realtime Database y vuelve a descargar google-services.json",
                    Toast.LENGTH_LONG).show();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    // Servicio 4: READ. Descarga y muestra la foto si existe
    void cargarFoto() {
        fotoRef.getDownloadUrl()
                .addOnSuccessListener(url -> {
                    imgFoto.setPadding(0, 0, 0, 0);
                    Glide.with(MainActivity.this).load(url).circleCrop().into(imgFoto);
                    txtEstadoFoto.setText("Foto guardada en Cloud Storage");
                })
                .addOnFailureListener(e -> txtEstadoFoto.setText("Aún no tienes foto de perfil"));
    }

    // Servicio 4: CREATE / UPDATE. Sube (o reemplaza) la foto
    void subirFoto(Uri uri) {
        txtEstadoFoto.setText("Subiendo foto...");
        fotoRef.putFile(uri)
                .addOnSuccessListener(tarea -> {
                    Toast.makeText(MainActivity.this, "Foto actualizada", Toast.LENGTH_SHORT).show();
                    cargarFoto();
                })
                .addOnFailureListener(e -> {
                    txtEstadoFoto.setText("No se pudo subir la foto");
                    Toast.makeText(MainActivity.this,
                            "Error al subir: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Servicio 4: DELETE. Borra la foto de Cloud Storage
    void eliminarFoto() {
        fotoRef.delete()
                .addOnSuccessListener(unused -> {
                    imgFoto.setImageResource(R.drawable.ic_persona);
                    int relleno = (int) (28 * getResources().getDisplayMetrics().density);
                    imgFoto.setPadding(relleno, relleno, relleno, relleno);
                    txtEstadoFoto.setText("Aún no tienes foto de perfil");
                    Toast.makeText(MainActivity.this, "Foto eliminada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this,
                        "No hay foto para eliminar", Toast.LENGTH_SHORT).show());
    }

    // Servicio 5: Cloud Messaging (canal, permiso y suscripción al tema "mini8")
    void configurarNotificaciones() {
        // En Android 8 o superior las notificaciones necesitan un canal
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel canal = new NotificationChannel("mini8_canal",
                    "Notificaciones", NotificationManager.IMPORTANCE_DEFAULT);
            getSystemService(NotificationManager.class).createNotificationChannel(canal);
        }

        // En Android 13 o superior se pide permiso al usuario
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        // Todos los que se suscriben al tema "mini8" reciben los mensajes enviados a ese tema
        FirebaseMessaging.getInstance().subscribeToTopic("mini8");
    }

    // Se deja de escuchar la base de datos al cerrar la pantalla
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (contadorRef != null && contadorListener != null) {
            contadorRef.removeEventListener(contadorListener);
        }
    }
}
