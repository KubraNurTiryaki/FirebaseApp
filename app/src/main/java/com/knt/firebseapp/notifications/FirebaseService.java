package com.knt.firebseapp.notifications;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;

public class FirebaseService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String tokenRefresh) {
        super.onNewToken(tokenRefresh);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            updateToken(tokenRefresh);
        }
    }

    private void updateToken(String tokenRefresh) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token = new Token(tokenRefresh);
        ref.child(user.getUid()).setValue(token);


    }


    /*    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String tokenRefresh = FirebaseInstanceId.getInstance().getInstanceId().getResult().getToken();
    }*/
}
