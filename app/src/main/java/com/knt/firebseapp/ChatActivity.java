package com.knt.firebseapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.knt.firebseapp.models.ModelUser;
import com.knt.firebseapp.notifications.Client;
import com.knt.firebseapp.notifications.Data;
import com.knt.firebseapp.notifications.Response;
import com.knt.firebseapp.notifications.Sender;
import com.knt.firebseapp.notifications.Token;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;

public class ChatActivity extends AppCompatActivity {


    //Views from xml
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profileIv;
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


    APIService apiService;
    boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //init views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        recyclerView = findViewById(R.id.chat_recyclerView);
        profileIv = findViewById(R.id.profileIv);
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

        //create api service
        apiService = Client.getRetrofit("https://fcm.googleapis.com/").create(APIService.class);



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
                        Picasso.get().load(theirImage).placeholder(R.drawable.ic_default_img_white).into(profileIv);
                    } catch (Exception e) {
                        //there is exception getting picture, set defalut picture
                        Picasso.get().load(R.drawable.ic_default_img_white).into(profileIv);
                    }

                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        sendBtn.setOnClickListener(v -> {
            notify = true;
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

            //reset edittext after sending message
            messageEt.setText("");

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
                    if(chat.getReceiver() == null){
                        chat.setReceiver("");
                    }
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
                    if(chat.getReceiver() == null){
                        chat.setReceiver("");
                    }
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(theirUid)||
                            chat.getReceiver().equals(theirUid) && chat.getSender().equals(myUid)) {
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


        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();


        String timestamp = String.valueOf(System.currentTimeMillis());


        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", theirUid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isSeen", false);
        databaseReference.child("Chats").push().setValue(hashMap);



        final DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                ModelUser user = snapshot.getValue(ModelUser.class);
                if(user.getName()==null){
                    user.setName("");
                }
                if(notify){
                sendNotification(theirUid,user.getName(),message);
                }
                notify = false;

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

//create chatlist node/ child in firebase

        final DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(myUid)
                .child(theirUid);
        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    chatRef1.child("id").setValue(theirUid);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        final DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(theirUid)
                .child(myUid);

        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()){
                    chatRef2.child("id").setValue(myUid);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendNotification(final String theirUid,final String name,final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(theirUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(myUid,name+":"+message,"New Message", theirUid, R.drawable.ic_default_img);

                    if(token.getToken()==null){
                        token.setToken("");
                    }
                    Sender sender = new Sender(data, token.getToken());
                    apiService.sendNotification(sender)
                            .enqueue(new Callback<Response>() {
                                @Override
                                public void onResponse(Call<Response> call, @NonNull retrofit2.Response<Response> response) {
                                   // Toast.makeText(ChatActivity.this, "sendNotification onResponse içinde ChatActivity"+response.message(), Toast.LENGTH_SHORT).show();
                                    //Toast.makeText(ChatActivity.this, ""+response.message(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Call<Response> call, Throwable t) {
                                    Toast.makeText(ChatActivity.this, "burda bişi oluyo mu, OLMUYOMUŞ", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


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
        super.onStart();
        checkUserStatus();
        //set online
        checkOnlineStatus("online");
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
        super.onResume();
        //set online
        checkOnlineStatus("online");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //hide searchview, add post as we dont need it here
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_add_post).setVisible(false);


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