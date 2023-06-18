package com.knt.firebseapp;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.knt.firebseapp.adapters.AdapterComments;
import com.knt.firebseapp.models.ModelComment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {

    //to get detail of user and post
    String theirUid, myUid, myEmail, myName, myDp, postId, pLikes, theirDp, theirName, pImage;


    boolean mProcessComment = false;
    boolean mProcessLike = false;


    //progress bar
    ProgressDialog pd;


    //vşews
    ImageView uPictureIv, pImageIv;
    TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
    ImageButton moreBtn;
    Button likeBtn, shareBtn;
    LinearLayout profileLayout;
    RecyclerView recyclerView;

    List<ModelComment> commentList;
    AdapterComments adapterComments;


    //add comment views
    EditText commentEt;
    ImageButton sendBtn;
    ImageView cAvatarIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);


        //Action bar and its propertes
        ActionBar actionBar = getSupportActionBar();
        if (actionBar.getTitle() == null) {
            actionBar.setTitle("");
        }
        actionBar.setTitle("Post Detail");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //get id pf post using intent
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");


        //init views
        uPictureIv = findViewById(R.id.uPictureIv);
        pImageIv = findViewById(R.id.pImageIv);
        uNameTv = findViewById(R.id.uNameTv);
        pTimeTv = findViewById(R.id.pTimeTv);
        pTitleTv = findViewById(R.id.pTitleTv);
        pDescriptionTv = findViewById(R.id.pDescriptionTv);
        pLikesTv = findViewById(R.id.pLikesTv);
        pCommentsTv = findViewById(R.id.pCommentsTv);
        moreBtn = findViewById(R.id.moreBtn);
        likeBtn = findViewById(R.id.likeBtn);
        shareBtn = findViewById(R.id.shareBtn);
        profileLayout = findViewById(R.id.profileLayout);
        recyclerView = findViewById(R.id.recyclerView);

        commentEt = findViewById(R.id.commentEt);
        sendBtn = findViewById(R.id.sendBtn);
        cAvatarIv = findViewById(R.id.cAvatarIv);


        loadPostInfo();

        checkUserStatus();


        loadUserInfo();

        setLikes();


        //set subtitle of actionbar
        actionBar.setSubtitle("SignedIn as: " + myEmail);

        loadComments();

        //send comment button click
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postComment();
            }
        });

        //like button click handle
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likePost();
            }
        });


        //more button click handle
        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showMoreOptions();
            }
        });


    }

    private void loadComments() {
        //layout(Linear) for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        //set layout to recyclerView
        recyclerView.setLayoutManager(layoutManager);

        //init comments lsit
        commentList = new ArrayList<>();

        //path of the post, to get its commments
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelComment modelComment = ds.getValue(ModelComment.class);

                    commentList.add(modelComment);

                    //pass myUid and postId as parameter of constructor of Comment Adapter


                    //setup adapter
                    adapterComments = new AdapterComments(getApplicationContext(), commentList, myUid, postId);
                    //set adapter
                    recyclerView.setAdapter(adapterComments);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void showMoreOptions() {
        //creating popup menu currently having option Delete, we will add more opions later
        PopupMenu popupMenu = new PopupMenu(this, moreBtn, Gravity.END);

        //show delete option in only post(s) of currently signed in user
        if (theirUid.equals(myUid)) {
            //add items in menu
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit"); // Bu satır normalde post editlemek için var ama daha yazmadım onun kodunu
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == 0) {
                    //delete is clicked
                    beginDelete();
                } else if (id == 1) {
                    Intent intent = new Intent(PostDetailActivity.this, AddPostActivity.class);

                }
                return false;
            }
        });

        //show menu
        popupMenu.show();

    }
    private void beginDelete() {

        //post can be with or wo image

        if (pImage.equals("noImage")) {
            //post without image
            deleteWithoutImage();

        } else {
            //post with image
            deleteWithImage();

        }


    }

    private void deleteWithImage() {

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting...");

        /*Steps:
         * 1) Delete image using url
         * 2) delete from database using post id*/

        StorageReference pickRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        pickRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //image deleted , now delte database

                        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    ds.getRef().removeValue(); //remove values from pid matches
                                }
                                //deleted
                                Toast.makeText(PostDetailActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                pd.dismiss();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                })
                .addOnFailureListener((e) -> {
                    //failed, cant go further
                    pd.dismiss();
                    Toast.makeText(PostDetailActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                });

    }

    private void deleteWithoutImage() {

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting...");

        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ds.getRef().removeValue(); //remove values from pid matches
                }
                //deleted
                Toast.makeText(PostDetailActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void setLikes() {
        //when the details of post is loading, also check if current user has liked it or not
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference("Likes");

        likesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postId).hasChild(myUid)) {
                    //user has liked this post
                    /* To indicate that the post is liked by this(SignedIn) user
                    Change drawable left icon of like button
                    Change text of like button from "Like" to "Liked" */
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                    likeBtn.setText("Liked");

                } else {
                    //user ha s not liked this post
                    /* To indicate that the post is not liked by this(SignedIn) user
                    Change drawable left icon of like button
                    Change text of like button from "Liked" to "Like" */
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0, 0, 0);
                    likeBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void likePost() {
        /*get total number of likes for the post, whose like button clicked
         * if currently sigbed in user has not liked it before
         * increase value by 1, otherwise decrease value by 1*/

        mProcessLike = true;
        //get id of the post clicked
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference("Likes");
        final DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("Posts");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessLike) {

                    if (dataSnapshot.child(postId).hasChild(myUid)) {
                        // already lijked, so remove
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) - 1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;


                    } else {
                        //not liked,  like it
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) + 1));
                        likesRef.child(postId).child(myUid).setValue("Liked"); //set any value
                        mProcessLike = false;


                    }


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void postComment() {


        pd = new ProgressDialog(PostDetailActivity.this);

        pd.setMessage("Adding comment...");

        if (commentEt.getText()==null){
            commentEt.setText("");
        }


        //get data from comment edit text
        String comment = commentEt.getText().toString().trim();
        //valdiate
        if (TextUtils.isEmpty(comment)) {
            //no value is entered
            Toast.makeText(this, "Comment is empty...", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = String.valueOf(System.currentTimeMillis());

        //each cpost will have a child "Comments" that will contain comments of that post
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");


        HashMap<String, Object> hashMap = new HashMap<>();
        //put info in hashmao
        hashMap.put("cId", timeStamp);
        hashMap.put("comment", comment);
        hashMap.put("timeStamp", timeStamp);
        hashMap.put("uid", myUid);
        hashMap.put("uEmail", myEmail);
        hashMap.put("uDp", myDp);
        hashMap.put("uName", myName);

        //put this data in db
        ref.child(timeStamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //added
                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this, "Comment Added...", Toast.LENGTH_SHORT).show();
                        commentEt.setText("");
                        updateCommentCount();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed, not added
                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();


                    }
                });


    }

    private void updateCommentCount() {
        //whenever user adds comment increase the comment count as we did for like count
        mProcessComment = true;
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mProcessComment) {



                    String comments = "" + snapshot.child("pComments").getValue();
                    int newCommentVal = Integer.parseInt(comments) + 1;
                    ref.child("pComments").setValue("" + newCommentVal);
                    mProcessComment = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void loadUserInfo() {
        //get currnet user info
        Query myRef = FirebaseDatabase.getInstance().getReference("Users");
        myRef.orderByChild("uid").equalTo(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    myName = "" + ds.child("name").getValue();
                    myDp = "" + ds.child("image").getValue();

                    //set data
                    try {
                        //if image is received then set
                        Picasso.get().load(myDp).placeholder(R.drawable.ic_default_img).into(cAvatarIv);
                    } catch (Exception e) {

                        Picasso.get().load(R.drawable.ic_default_img).into(cAvatarIv);
                    }


                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void loadPostInfo() {
        //get post using the id of the post
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //keeep checking the posts until the required post
                for (DataSnapshot ds : snapshot.getChildren()) {
                    //get data
                    String pTitle = "" + ds.child("pTıtle").getValue();
                    String pDescr = "" + ds.child("pDescr").getValue();
                    pLikes = "" + ds.child("pLikes").getValue();
                    String pTimeStamp = "" + ds.child("pTime").getValue();
                    pImage = "" + ds.child("pImage").getValue();
                    theirDp = "" + ds.child("uDp").getValue();
                    theirUid = "" + ds.child("uid").getValue();
                    String uEmail = "" + ds.child("uEmail").getValue();
                    theirName = "" + ds.child("uName").getValue();
                    String commentCount = "" + ds.child("pComments").getValue();


                    //convert timestamp to dd/mm/yyyy hh:mm am/pm
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
                    String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString(); //DateFormat importunu sildi , onda çalıştı bende niye çalışmadı

                    //set data
                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDescr);
                    pLikesTv.setText(pLikes + "Likes");
                    pTimeTv.setText(pTime);
                    pCommentsTv.setText(commentCount + " Comments");


                    uNameTv.setText(theirName);

                    //set image of the user who posted
                    //if there is no image i.e. pImage.equals("noImage") then hide ImageView
                    if (pImage.equals("noImage")) {
                        //hide imageview
                        pImageIv.setVisibility(View.GONE);
                    } else {

                        //show imageview
                        pImageIv.setVisibility(View.VISIBLE);  //make sure to correct this


                        try {
                            Picasso.get().load(pImage).into(pImageIv);
                        } catch (Exception e) {

                        }

                    }

                    // set user image in the comment part cAvatarIv

                    try {
                        Picasso.get().load(theirDp).placeholder(R.drawable.ic_default_img).into(uPictureIv);
                    } catch (Exception e) {
                        Picasso.get().load(R.drawable.ic_default_img).into(uPictureIv);
                    }


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void checkUserStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            //user is signed in
            myEmail = user.getEmail();
            myUid = user.getUid();

        } else {
            //user not signed in , go to main actibity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        //hide some menu items
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            checkUserStatus();
        }


        return super.onOptionsItemSelected(item);
    }
}