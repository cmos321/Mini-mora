package com.example.mini8firebase;

// Modelo simple: una tarea guardada en Cloud Firestore
public class Tarea {

    String id;       // Id del documento en Firestore (sirve para editar y eliminar)
    String texto;
    boolean hecha;   // true = tarea terminada
    long timestamp;  // Para ordenar: la más nueva arriba

    public Tarea(String id, String texto, boolean hecha, long timestamp) {
        this.id = id;
        this.texto = texto;
        this.hecha = hecha;
        this.timestamp = timestamp;
    }
}
