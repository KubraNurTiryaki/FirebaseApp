package com.knt.firebseapp.notifications;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;

public class FirebaseService extends FirebaseMessagingService {


    @Override
    public void onNewToken(@NonNull String tokenRefresh) {

        super.onNewToken(tokenRefresh);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user != null){
            updateToken();
        }
    }



    private void updateToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (task.isSuccessful()) {
                            String tokenRefresh = task.getResult();
                            if (tokenRefresh != null) {

                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
                                Token token = new Token(tokenRefresh);
                                ref.child(user.getUid()).setValue(token);
                                Toast.makeText(FirebaseService.this, "Token updated in FirebaseService:", Toast.LENGTH_SHORT).show();

                            } else {
                                Toast.makeText(FirebaseService.this, "FirebaseService: Failed to retrieve token: Token is null", Toast.LENGTH_SHORT).show();

                            }
                        } else {
                            Exception exception = task.getException();
                            if (exception != null) {
                                Toast.makeText(FirebaseService.this, "FirebaseService: bi exception oldu:"+ exception.getMessage(), Toast.LENGTH_SHORT).show();

                            }
                        }
                    }
                });
    }







/*

    private void updateToken(String tokenRefresh) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token = new Token(tokenRefresh);
        ref.child(user.getUid()).setValue(token);


    }
*/


    /*    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String tokenRefresh = FirebaseInstanceId.getInstance().getInstanceId().getResult().getToken();
    }*/
}
