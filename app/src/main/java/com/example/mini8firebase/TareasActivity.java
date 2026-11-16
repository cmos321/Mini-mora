package com.example.mini8firebase;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// Servicio 1: Cloud Firestore (CRUD de tareas)
public class TareasActivity extends AppCompatActivity {

    // Declaración de los componentes de la interfaz
    ImageView btnVolver;
    EditText edtTarea;
    MaterialButton btnAgregar;
    ListView listaTareas;
    TextView txtVacio;

    FirebaseFirestore db;
    String uid; // Id del usuario: cada uno ve solo sus tareas

    ArrayList<Tarea> tareas = new ArrayList<>();
    ArrayList<String> textos = new ArrayList<>();
    ArrayAdapter<String> adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tareas);

        btnVolver = findViewById(R.id.btnVolver);
        edtTarea = findViewById(R.id.edtTarea);
        btnAgregar = findViewById(R.id.btnAgregar);
        listaTareas = findViewById(R.id.listaTareas);
        txtVacio = findViewById(R.id.txtVacio);

        // ajuste de márgenes del sistema (barras de estado y navegación)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos);
        listaTareas.setAdapter(adaptador);

        btnVolver.setOnClickListener(v -> finish());

        // CREATE: agrega la tarea escrita
        btnAgregar.setOnClickListener(v -> {
            String texto = edtTarea.getText().toString().trim();
            if (texto.isEmpty()) {
                Toast.makeText(this, "Escribe una tarea primero", Toast.LENGTH_SHORT).show();
            } else {
                crearTarea(texto);
            }
        });

        // Al tocar una tarea se elige qué hacer con ella
        listaTareas.setOnItemClickListener((parent, view, position, id) -> mostrarOpciones(tareas.get(position)));

        leerTareas();
    }

    // READ (leer): trae las tareas del usuario y las muestra en la lista
    void leerTareas() {
        db.collection("tareas")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(consulta -> {
                    tareas.clear();
                    for (DocumentSnapshot doc : consulta.getDocuments()) {
                        Long marcaTiempo = doc.getLong("timestamp");
                        Boolean hecha = doc.getBoolean("hecha");
                        tareas.add(new Tarea(doc.getId(), doc.getString("texto"),
                                hecha != null && hecha, marcaTiempo == null ? 0 : marcaTiempo));
                    }
                    // La tarea más nueva va arriba
                    Collections.sort(tareas, (a, b) -> Long.compare(b.timestamp, a.timestamp));

                    textos.clear();
                    for (Tarea t : tareas) {
                        textos.add((t.hecha ? "✓  " : "•  ") + t.texto);
                    }
                    adaptador.notifyDataSetChanged();

                    txtVacio.setVisibility(tareas.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> Toast.makeText(TareasActivity.this,
                        "No se pudieron cargar las tareas: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // CREATE (crear): guarda una tarea nueva como un documento en la colección "tareas"
    void crearTarea(String texto) {
        Map<String, Object> tarea = new HashMap<>();
        tarea.put("uid", uid);
        tarea.put("texto", texto);
        tarea.put("hecha", false);
        tarea.put("timestamp", System.currentTimeMillis());

        db.collection("tareas")
                .add(tarea)
                .addOnSuccessListener(referencia -> {
                    edtTarea.setText("");
                    Toast.makeText(TareasActivity.this, "Tarea guardada", Toast.LENGTH_SHORT).show();
                    leerTareas();
                })
                .addOnFailureListener(e -> Toast.makeText(TareasActivity.this,
                        "No se pudo guardar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // Pregunta qué hacer con la tarea tocada
    void mostrarOpciones(Tarea tarea) {
        String[] opciones = {tarea.hecha ? "Marcar como pendiente" : "Marcar como hecha", "Editar texto", "Eliminar"};
        new AlertDialog.Builder(this)
                .setTitle(tarea.texto)
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        actualizarCampo(tarea.id, "hecha", !tarea.hecha);
                    } else if (which == 1) {
                        editarTexto(tarea);
                    } else {
                        eliminarTarea(tarea.id);
                    }
                })
                .show();
    }

    // Abre un cuadro con el texto actual para cambiarlo
    void editarTexto(Tarea tarea) {
        EditText campo = new EditText(this);
        campo.setText(tarea.texto);
        campo.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Editar tarea")
                .setView(campo)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevo = campo.getText().toString().trim();
                    if (nuevo.isEmpty()) {
                        Toast.makeText(TareasActivity.this, "La tarea no puede estar vacía",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        actualizarCampo(tarea.id, "texto", nuevo);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // UPDATE (actualizar): cambia un campo de una tarea existente ("texto" o "hecha")
    void actualizarCampo(String idDocumento, String campo, Object valor) {
        db.collection("tareas")
                .document(idDocumento)
                .update(campo, valor)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(TareasActivity.this, "Tarea actualizada", Toast.LENGTH_SHORT).show();
                    leerTareas();
                })
                .addOnFailureListener(e -> Toast.makeText(TareasActivity.this,
                        "No se pudo actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // DELETE (eliminar): borra el documento de la tarea
    void eliminarTarea(String idDocumento) {
        db.collection("tareas")
                .document(idDocumento)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(TareasActivity.this, "Tarea eliminada", Toast.LENGTH_SHORT).show();
                    leerTareas();
                })
                .addOnFailureListener(e -> Toast.makeText(TareasActivity.this,
                        "No se pudo eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
