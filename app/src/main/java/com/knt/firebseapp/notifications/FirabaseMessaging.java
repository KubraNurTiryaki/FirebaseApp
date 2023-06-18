package com.knt.firebseapp.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.knt.firebseapp.ChatActivity;

public class FirabaseMessaging extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String tokenRefresh) {
        super.onNewToken(tokenRefresh);


    }


    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        //get currently user from shared preferences from DashboardActivity
        SharedPreferences sp= getSharedPreferences("SP_USER", MODE_PRIVATE);
        String savedCurrentUser = sp.getString("Current_USERID","None");

        String sent = message.getData().get("sent");
        String user = message.getData().get("user");
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        //Toast.makeText(this, "FirebaseMessaging onMessageReceived içinde", Toast.LENGTH_SHORT).show();
        if (fUser!= null && sent.equals(fUser.getUid())){
            PendingIntent pendingIntent = null;

            if(!savedCurrentUser.equals(user)){


                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S){

                    String icon = message.getData().get("icon");
                    String title = message.getData().get("title");
                    String body = message.getData().get("body");

                    RemoteMessage.Notification notification = message.getNotification();
                    int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
                    Intent intent = new Intent(this, ChatActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("theirUid", user);
                    intent.putExtras(bundle);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    PendingIntent pIntent = PendingIntent.getActivity(this,i,intent,PendingIntent.FLAG_MUTABLE);


                    Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                    OreoAndAboveNotification notification1 = new OreoAndAboveNotification(this);
                    Notification.Builder builder = notification1.getONotifications(title,body,pIntent,defSoundUri,icon);


                    int j = 0;
                    if(i>0){
                        j = i;
                    }
                    notification1.getManager().notify(j,builder.build());


                    //Toast.makeText(this, "o and above içinde", Toast.LENGTH_SHORT).show();
                }
                else {
                    sendNormalNotification(message);
                    Toast.makeText(this, "normal içinde", Toast.LENGTH_SHORT).show();

                }
            }
        }


/*

        if (fUser!= null && sent.equals(fUser.getUid())){
            if(!savedCurrentUser.equals(user)){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    sendOAndAboveNotification(message);
                    Toast.makeText(this, "o and above içinde", Toast.LENGTH_SHORT).show();
                }
                else {
                    sendNormalNotification(message);
                    Toast.makeText(this, "normal içinde", Toast.LENGTH_SHORT).show();

                }
            }
        }
*/

    }











    private void sendNormalNotification(RemoteMessage message) {

        String user = message.getData().get("user");
        String icon = message.getData().get("icon");
        String title = message.getData().get("title");
        String body = message.getData().get("body");

        RemoteMessage.Notification notification = message.getNotification();
        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("theirUid", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pIntent = PendingIntent.getActivity
                    (this, i, intent, PendingIntent.FLAG_MUTABLE);
        }
        else
        {
            pIntent = PendingIntent.getActivity
                    (this, i, intent, PendingIntent.FLAG_ONE_SHOT);
        }


       // PendingIntent pIntent = PendingIntent.getActivity(this,i,intent,PendingIntent.FLAG_ONE_SHOT);


        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(Integer.parseInt(icon))
                .setContentText(body)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(defSoundUri)
                .setContentIntent(pIntent);


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int j = 0;
        if(i>0){
            j = i;
        }
        notificationManager.notify(j,builder.build());

    }

    private void sendOAndAboveNotification(RemoteMessage message) {

        String user = message.getData().get("user");
        String icon = message.getData().get("icon");
        String title = message.getData().get("title");
        String body = message.getData().get("body");

        RemoteMessage.Notification notification = message.getNotification();
        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("theirUid", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this,i,intent,PendingIntent.FLAG_ONE_SHOT);


        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        OreoAndAboveNotification notification1 = new OreoAndAboveNotification(this);
        Notification.Builder builder = notification1.getONotifications(title,body,pIntent,defSoundUri,icon);


        int j = 0;
        if(i>0){
            j = i;
        }
        notification1.getManager().notify(j,builder.build());



    }
}
