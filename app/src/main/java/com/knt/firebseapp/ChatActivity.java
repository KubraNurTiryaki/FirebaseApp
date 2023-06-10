package com.knt.firebseapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.knt.firebseapp.adapters.AdapterChat;
import com.knt.firebseapp.models.ModelChat;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {


    //Views from xml
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profileTv;
    TextView nameTv, userStatusTv;
    EditText messageEt;
    ImageButton sendBtn;

    //firebase auth
    FirebaseAuth firebaseAuth;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersDbRef;

    //for checking if use has seen message or not
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;

    List<ModelChat> chatList;
    AdapterChat adapterChat;

    String theirUid;
    String myUid;
    String theirImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //init views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        recyclerView = findViewById(R.id.chat_recyclerView);
        profileTv = findViewById(R.id.profileIv);
        nameTv = findViewById(R.id.nameTv);
        userStatusTv = findViewById(R.id.userStatusTv);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendBtn);

        //LinearLayout for RecyclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        //recyclerview properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        /*On clicking from user list we have passed that user's UID using intent
         * So get that uid here to get the profile, picture, name and start chat with that
         * user*/
        Intent intent = getIntent();
        theirUid = intent.getStringExtra("theirUid");


        //firebase auth insctance
        firebaseAuth = FirebaseAuth.getInstance();

        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDbRef = firebaseDatabase.getReference("Users");

        //search user to get that user's info
        Query userQuery = usersDbRef.orderByChild("uid").equalTo(theirUid);
        //get user picture and name
        userQuery.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                //check until required info is received
                for (DataSnapshot ds : datasnapshot.getChildren()) {
                    String name = "" + ds.child("name").getValue();
                    theirImage = "" + ds.child("image").getValue();
                    String typingStatus = "" + ds.child("typingTo").getValue();

                    //check typing status
                    if (typingStatus.equals(myUid)){
                        userStatusTv.setText("typing...");
                    }
                    else{


                        //get value of onlinestatus
                        String onlineStatus =  "" + ds.child("onlineStatus").getValue();

                        if(onlineStatus.equals("online")){
                            userStatusTv.setText("online");
                            userStatusTv.setText(onlineStatus);
                        }
                        else {
                            //convert timestamp to proper time date
                            //convert time stamp to dd/mm/yyyy hh:mm am/pm
                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString(); //buradan tam emin olamadım sıkıntılı olabilir
                            userStatusTv.setText("Last seen at: "+dateTime);
                            //add any time stamp to registered users in firebase database manually. But i had a problem
                        }
                    }



                    //set data
                    nameTv.setText(name);

                    try {
                        //image receiver, set it to imageview in toolnar
                        Picasso.get().load(theirImage).placeholder(R.drawable.ic_default_img_white).into(profileTv);
                    } catch (Exception e) {
                        //there is exception getting picture, set defalut picture
                        Picasso.get().load(R.drawable.ic_default_img_white).into(profileTv);
                    }

                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //get text from edit text
                String message = messageEt.getText().toString().trim();
                //check if text is empty or not
                if (TextUtils.isEmpty(message)) {
                    //text empty
                    Toast.makeText(ChatActivity.this, "Cannot send the empty message...", Toast.LENGTH_SHORT).show();
                } else {
                    // text not empty
                    sendMessage(message);
                }

            }
        });

        //check edit text change lstener
        messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(s.toString().trim().length()==0){
                    checkTypingStatus("noOne");
                }
                else {
                    checkTypingStatus(theirUid); //uid of receiver
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        readMessages();

        seenMessage();


    }

    private void seenMessage() {

        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnopshot) {
                for (DataSnapshot ds : dataSnopshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(theirUid)) {
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isSeen", true);
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void readMessages() {
        chatList = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnopshot) {
                chatList.clear();
                for (DataSnapshot ds : datasnopshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if ((chat.getReceiver().equals(myUid) && chat.getSender().equals(theirUid)) ||
                            (chat.getReceiver().equals(theirUid) && chat.getSender().equals(myUid))) {
                        chatList.add(chat);
                    }

                    //adapter
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, theirImage);
                    adapterChat.notifyDataSetChanged();
                    //set adapter to recyclerview
                    recyclerView.setAdapter(adapterChat);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(String message) {
        /*"Chat's" node will be created that will contain all chats
         * Whenever user sends message it will create new child in "Chats" node and that child will contain
         * the following key values
         * sender: UID of sender
         * receiver: UID of receiver
         * message: the actual message*/


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();


        String timestamp = String.valueOf(System.currentTimeMillis());


        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", theirUid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isSeen", false);
        databaseReference.child("Chats").push().setValue(hashMap);

        //reset edittext after sending message
        messageEt.setText("");
    }


    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //user is signed in stay here
            //set email of logged in user
            //mProfileTv.setText(user.getEmail());
            myUid = user.getUid(); //currently signedin user's uid

        } else {
            //user not signed in, go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }


    private void checkOnlineStatus(String status){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String , Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        //update value of onlineStatus of current user
        dbRef.updateChildren(hashMap);
    }

    private void checkTypingStatus(String typing){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String , Object> hashMap = new HashMap<>();
        hashMap.put("typingTo", typing);
        //update value of onlineStatus of current user
        dbRef.updateChildren(hashMap);
    }


    @Override
    protected void onStart() {
        checkUserStatus();
        //set online
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //get timestapmp
        String timestamp = String.valueOf(System.currentTimeMillis());

        //set offline with last seen time stamp
        checkOnlineStatus(timestamp);
        checkTypingStatus("noOne");
        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        //set online
        checkOnlineStatus("online");
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //hide searchview, as we dont need it here
        menu.findItem(R.id.action_search).setVisible(false);


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_logout) {
            firebaseAuth.signOut();
            checkUserStatus();
        }


        return super.onOptionsItemSelected(item);
    }
}