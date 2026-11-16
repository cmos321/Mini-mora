package com.example.mini8firebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;

// Servicio 3: Firebase Authentication (correo y contraseña)
public class LoginActivity extends AppCompatActivity {

    // Declaración de los componentes de la interfaz
    EditText edtCorreo, edtClave;
    MaterialButton btnIniciar, btnRegistrar;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        edtCorreo = findViewById(R.id.edtCorreo);
        edtClave = findViewById(R.id.edtClave);
        btnIniciar = findViewById(R.id.btnIniciar);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        // ajuste de márgenes del sistema (barras de estado y navegación)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        // Si ya había una sesión iniciada, se entra directo a la app
        if (auth.getCurrentUser() != null) {
            irAPrincipal();
            return;
        }

        // Iniciar sesión con una cuenta existente
        btnIniciar.setOnClickListener(v -> {
            String correo = edtCorreo.getText().toString().trim();
            String clave = edtClave.getText().toString().trim();
            if (!datosValidos(correo, clave)) {
                return;
            }
            auth.signInWithEmailAndPassword(correo, clave)
                    .addOnSuccessListener(resultado -> {
                        registrarEvento(FirebaseAnalytics.Event.LOGIN);
                        irAPrincipal();
                    })
                    .addOnFailureListener(e -> Toast.makeText(LoginActivity.this,
                            "No se pudo iniciar sesión: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        // Crear una cuenta nueva
        btnRegistrar.setOnClickListener(v -> {
            String correo = edtCorreo.getText().toString().trim();
            String clave = edtClave.getText().toString().trim();
            if (!datosValidos(correo, clave)) {
                return;
            }
            auth.createUserWithEmailAndPassword(correo, clave)
                    .addOnSuccessListener(resultado -> {
                        registrarEvento(FirebaseAnalytics.Event.SIGN_UP);
                        Toast.makeText(LoginActivity.this, "¡Cuenta creada!", Toast.LENGTH_SHORT).show();
                        irAPrincipal();
                    })
                    .addOnFailureListener(e -> Toast.makeText(LoginActivity.this,
                            "No se pudo crear la cuenta: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    // Revisa que el correo y la contraseña no estén vacíos y que la clave tenga 6 o más caracteres
    boolean datosValidos(String correo, String clave) {
        if (correo.isEmpty() || clave.isEmpty()) {
            Toast.makeText(this, "Escribe tu correo y tu contraseña", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (clave.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Servicio 7: Analytics (registra el inicio de sesión o el registro)
    void registrarEvento(String evento) {
        Bundle datos = new Bundle();
        datos.putString(FirebaseAnalytics.Param.METHOD, "correo");
        FirebaseAnalytics.getInstance(this).logEvent(evento, datos);
    }

    void irAPrincipal() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
