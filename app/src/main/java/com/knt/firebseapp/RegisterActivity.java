package com.knt.firebseapp;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    //views
    EditText mEmailEt, mPasswordEt;
    Button mRegisterBtn;
    TextView mHaveAccountTv;

    //progressbar to display while registering user
    ProgressDialog progressDialog;

    //Declare an instance of Firebase Auth
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        //Actionbar and its title

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Create Account");

        //enable back button
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        // In the onCreate() method, initialize the FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();

        tanimla();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering user...");

        mRegisterBtn.setOnClickListener(view -> {
            //input email, password
            String email = mEmailEt.getText().toString().trim();
            String password = mPasswordEt.getText().toString().trim();
            //validate
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                //set error and focus to email edittext
                mEmailEt.setError("Invalid Email");
                mEmailEt.setFocusable(true);
            } else if (password.length() < 6) {
                //set error and focus to password edittext
                mPasswordEt.setError("Password length must be at least 6 characters");
                mPasswordEt.setFocusable(true);

            } else {
                registerUser(email, password);//register the user
            }
        });

        //handle login textview click listener
        mHaveAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

    }


    private void registerUser(String email, String password) {
        //email and password pattern is valid, show progress dialog and start registerin user
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                //Sign in success, dismiss dialog and start register activity
                progressDialog.dismiss();



                FirebaseUser user = mAuth.getCurrentUser();
                //Get user email and uid from auth
                String emailWTF = user.getEmail();              //benzer kod LoginActivity'de hata vermezken burda niye veriyo? Çıççam bacağına şimdi , WTF!?
                String uid = user.getUid();
                //When user is regsitered store user info in firebase realtime database too
                //using HashMap
                HashMap <Object, String> hashMap = new HashMap<>();
                //put info in hashmap
                hashMap.put("email", emailWTF);
                hashMap.put("uid", uid);
                hashMap.put("name", ""); //will add later (e.g edit profile)
                hashMap.put("phone", "");//will add later (e.g edit profile)
                hashMap.put("image", "");//will add later (e.g edit profile)
                hashMap.put("cover", "");//will add later (e.g edit profile)

                //firebase database instance
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                //path to store user data named "Users"
                DatabaseReference reference = database.getReference("Users");
                //put data within hashmap in database
                reference.child(uid).setValue(hashMap);




                Toast.makeText(RegisterActivity.this, "Registered...\n" + user.getEmail(), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                finish();
            } else {
                //If sign in fails, display a message to the user.
                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            //error, dismiss progress dialog and get and show the error message
            progressDialog.dismiss();
            Toast.makeText(RegisterActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    public void tanimla() {

        mEmailEt = findViewById(R.id.emailET);
        mPasswordEt = findViewById(R.id.passwordET);
        mRegisterBtn = findViewById(R.id.registerBtn);
        mHaveAccountTv = findViewById(R.id.have_account);

    }

    public boolean onSupportNavigateUp() {
        onBackPressed(); //goes previous activity
        return super.onSupportNavigateUp();
    }
}