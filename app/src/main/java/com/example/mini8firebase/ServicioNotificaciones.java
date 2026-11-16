package com.example.mini8firebase;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

// Servicio 5: Cloud Messaging. Recibe las notificaciones enviadas desde la consola de Firebase
public class ServicioNotificaciones extends FirebaseMessagingService {

    // Se ejecuta cuando llega una notificación con la app abierta
    @Override
    public void onMessageReceived(@NonNull RemoteMessage mensaje) {
        String titulo = "Mini8 Firebase";
        String texto = "";
        if (mensaje.getNotification() != null) {
            titulo = mensaje.getNotification().getTitle();
            texto = mensaje.getNotification().getBody();
        }
        mostrarNotificacion(titulo, texto);
    }

    // Firebase entrega un token nuevo (identifica a este celular)
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("FCM", "Nuevo token: " + token);
    }

    void mostrarNotificacion(String titulo, String texto) {
        // En Android 13 o superior hace falta el permiso de notificaciones
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Al tocar la notificación se abre la app
        Intent intent = new Intent(this, LoginActivity.class);
        PendingIntent pendiente = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificacion = new NotificationCompat.Builder(this, "mini8_canal")
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setAutoCancel(true)
                .setContentIntent(pendiente);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), notificacion.build());
    }
}
