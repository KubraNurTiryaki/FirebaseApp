package com.knt.firebseapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.knt.firebseapp.notifications.Token;

public class DashboardActivity extends AppCompatActivity {

    //firebase auth
    FirebaseAuth firebaseAuth;

    ActionBar actionBar;

    String mUID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        //Actionbar and its title

        actionBar = getSupportActionBar();
        actionBar.setTitle("Profile");

        //init
        firebaseAuth = FirebaseAuth.getInstance();
        tanimla();


        //bottom navigation
        NavigationBarView navigationView = findViewById(R.id.navigation);

        navigationView.setOnItemSelectedListener(selectedListener);


        //home fragment transaction (default, on start)
        actionBar.setTitle("Home"); //Change actionbar title
        HomeFragment fragment1 = new HomeFragment();
        FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
        ft1.replace(R.id.content, fragment1, "");
        ft1.commit();

        checkUserStatus();


        updateToken();



        /*        updateToken(FirebaseMessaging.getInstance().getToken());*/



    }

    @Override
    protected void onResume() {
        checkUserStatus();
        super.onResume();
    }




    private void updateToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (task.isSuccessful()) {
                            String token = task.getResult();
                            if (token != null) {
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
                                Token mToken = new Token(token);
                                if (mUID != null) {
                                    ref.child(mUID).setValue(mToken);
                                }
                                /*
                                ref.child(mUID).setValue(mToken);*/
                                //Toast.makeText(DashboardActivity.this, "Token updated in Firebase: " + token, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(DashboardActivity.this, "Failed to retrieve token: Token is null", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Exception exception = task.getException();
                            if (exception != null) {
                                Toast.makeText(DashboardActivity.this, "Failed to retrieve token: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("FCM Token", "Failed to retrieve token", exception);
                            }
                        }
                    }
                });
    }






/*    public void updateToken(String token){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        ref.child(mUID).setValue(mToken);
        Toast.makeText(this, "updatetokendaDashboard", Toast.LENGTH_SHORT).show();


    }*/


    final private NavigationBarView.OnItemSelectedListener selectedListener =
            new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    //handle item clicks
                    switch (item.getItemId()) {
                        case R.id.nav_home:
                            //home fragment transaction
                            actionBar.setTitle("Home"); //Change actionbar title
                            HomeFragment fragment1 = new HomeFragment();
                            FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                            ft1.replace(R.id.content, fragment1, "");
                            ft1.commit();
                            return true;

                        case R.id.nav_profile:
                            //profile fragment transaction
                            actionBar.setTitle("Profile"); //Change actionbar title

                            ProfileFragment fragment2 = new ProfileFragment();
                            FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                            ft2.replace(R.id.content, fragment2, "");
                            ft2.commit();
                            return true;

                        case R.id.nav_users:
                            //users fragment transaction
                            actionBar.setTitle("Users");//Change actionbar title
                            UsersFragment fragment3 = new UsersFragment();
                            FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
                            ft3.replace(R.id.content, fragment3, "").commit();
                            return true;


                        case R.id.nav_chat:
                            //users fragment transaction
                            actionBar.setTitle("Chat");//Change actionbar title
                            ChatListFragment fragment4 = new ChatListFragment();
                            FragmentTransaction ft4 = getSupportFragmentManager().beginTransaction();
                            ft4.replace(R.id.content, fragment4, "").commit();
                            return true;


                    }


                    return false;
                }
            };


    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //user is signed in stay here
            //set email of logged in user
            //mProfileTv.setText(user.getEmail()); //burayı en başta eklerken hangi sayfada olduğumuzu görmek için eklemiştik önemli bişi değil sktret

            mUID = user.getUid();

            //save uid of currently signed in user in shared preferences
            SharedPreferences sp= getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID", mUID);
            editor.apply();


        } else {
            //user not signed in, go to main activity
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        //check user start of app
        checkUserStatus();
        super.onStart();

    }



    public void tanimla() {
        //mProfileTv = findViewById(R.id.profileTv);
    }


}