package com.knt.firebseapp;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.knt.firebseapp.adapters.AdapterPosts;
import com.knt.firebseapp.models.ModelPost;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProfileFragment extends Fragment {

    //firebase
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    //storage
    StorageReference storageReference;
    //path where images of user profile an d cover will be stored
    String storagePath = "Users_Profile_Cover_Imgs/";


    //views from xml
    ImageView avatarIv, coverIv;
    TextView nameTv, emailTv, phoneTv;

    FloatingActionButton fab;

    RecyclerView postsRecyclerView;

    ProgressDialog pd;

    //permission constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;


    //permissions array
    String[] cameraPermissions;
    String[] storagePermissions;

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;

    //uri of picked image
    Uri image_uri;


    //for checking profile or cover photo
    String profileOrCoverPhoto;


    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        //init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users"); // Remember the thing we wrote in Firebase Realtime Database? that's it.
        storageReference = FirebaseStorage.getInstance().getReference();//firenase storage reference


        //init permissions arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        //init views
        avatarIv = view.findViewById(R.id.avatarIv);
        coverIv = view.findViewById(R.id.coverIv);
        nameTv = view.findViewById(R.id.nameTv);
        emailTv = view.findViewById(R.id.emailTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        fab = view.findViewById(R.id.fab);
        postsRecyclerView = view.findViewById(R.id.recyclerview_posts);
        //init progress dialog
        pd = new ProgressDialog(getActivity());
        /*we have to get info of currentlt singed in user. We can get it using user's email or uid. I'm gonna retrieve yser detail uysing email
         * U know who i am. KNT
         *Bu using orderByChild query we will show the detail ffrom a node whose key named email has value equal to currently signed in email.
         * It will search all nodes, where the key marches it will get its detail */
        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                /*                checks until required data get*/
                for (DataSnapshot ds : snapshot.getChildren()) {
                    //get data
                    String name = "" + ds.child("name").getValue();  // Remember the thing we wrote in Firebase Realtime Database? that's it.
                    String email = "" + ds.child("email").getValue();  // Remember the thing we wrote in Firebase Realtime Database? that's it.
                    String phone = "" + ds.child("phone").getValue();  // Remember the thing we wrote in Firebase Realtime Database? that's it.
                    String image = "" + ds.child("image").getValue();  // Remember the thing we wrote in Firebase Realtime Database? that's it.
                    String cover = "" + ds.child("cover").getValue();  // Remember the thing we wrote in Firebase Realtime Database? that's it.

//set data
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    try {
                        //if image is received then set
                        Picasso.get().load(image).into(avatarIv);
                    } catch (Exception e) {
                        //if there is any exception while getting image then set default
                        Picasso.get().load(R.drawable.ic_default_img_white).into(avatarIv);

                    }
                    try {
                        //if image is received then set
                        Picasso.get().load(cover).into(coverIv);
                    } catch (Exception e) {
                        //if there is any exception while getting image then set default
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

//fab buton click
        fab.setOnClickListener(view1 -> showEditProfileDialog());

        postList = new ArrayList<>();

        checkUserStatus();
        loadMyPosts();

        return view;
    }

    private void loadMyPosts() {
        //linear layout for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest post first, for this load from last
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set this layout to recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);

        //init posts list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Posts");
        //query to load posts
        /*whenever user publish a post the uid of this yseris also saved as infı ıf post
         * so we're retrieving posts having uid equals to uid of current user*/
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelPost myPosts = ds.getValue(ModelPost.class);

                    //add to list
                    postList.add(myPosts);


                    //adapter
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //Toast.makeText(getActivity(), "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void searchMyPosts(String searchQuery) {
        //linear layout for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest post first, for this load from last
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set this layout to recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);

        //init posts list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Posts");
        //query to load posts
        /*whenever user publish a post the uid of this yseris also saved as infı ıf post
         * so we're retrieving posts having uid equals to uid of current user*/
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelPost myPosts = ds.getValue(ModelPost.class);

                    if (myPosts.getpTitle() == null) {
                        myPosts.setpTitle("");
                    }
                    if (myPosts.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            myPosts.getpDescr().toLowerCase().contains(searchQuery.toLowerCase())) {
                        //add to list
                        postList.add(myPosts);
                    }
                    //adapter
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkStoragePermission() {
        //check if storage permission is enabled or not
        //return true if enabled
        //return false if not enabled

        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission() {
        //request runtime storage permission
        // requestPermissions(storagePermissions,STORAGE_REQUEST_CODE);
        ActivityCompat.requestPermissions(getActivity(), storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        //check if camera permission is enabled or not
        //return true if enabled
        //return false if not enabled

        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private void requestCameraPermission() {
        //request runtime camera permission
        //requestPermissions(cameraPermissions,CAMERA_REQUEST_CODE);
        ActivityCompat.requestPermissions(getActivity(), cameraPermissions, CAMERA_REQUEST_CODE);
        //Toast.makeText(getActivity(), "SORUN BURDA DEĞİL", Toast.LENGTH_SHORT).show();
    }

    private void showEditProfileDialog() {
        /*Show dialog containing options
         * 1)Edit Profile Picture
         * 2)Edit Cover Photo
         * 3)Edit Name
         * 4)Edit Phone*/

        //options to show in dialog
        String[] options = {"Edit Profile Picture", "Edit Cover Photo", "Edit Name", "Edit Phone"};
        //Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Choose Action");
        //set items to dialog
        builder.setItems(options, (dialogInterface, i) -> {
            //handle dialog item clicks
            if (i == 0) {
                //Edit profile clicked
                pd.setMessage("Updating Profile Picture");
                profileOrCoverPhoto = "image";//i.e. changing profile picture, make sure to assign same values
                showImagePicDialog();
            } else if (i == 1) {
                //Edit cover clicked
                pd.setMessage("Updating Cover Photo");
                profileOrCoverPhoto = "cover";//i.e. changing cover photo, make sure to assign same values
                showImagePicDialog();
            } else if (i == 2) {
                //Edit name clicked
                pd.setMessage("Updating Name");
                //calling method and pass key "name" as parameter to update it's value in database
                showNamePhoneUpdateDialog("name");
            } else if (i == 3) {
                //Edit phone clicked
                pd.setMessage("Updating Phone");
                showNamePhoneUpdateDialog("phone");
            }
        });
        //create and show dialog
        builder.create().show();

    }

    private void showNamePhoneUpdateDialog(String key) {
//        /*parameter "key" will contain value:
//         either "name" which key in user's database which is used to update user's name*/
//         or "phone" which key in user's database which is used to update user's phone*/

        //custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update " + key);//e.g. Update name OR Update phone
        //set layout of diyalog
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10, 10, 10, 10);

        //add edit text
        EditText editText = new EditText(getActivity());
        editText.setHint("Enter " + key); //hint e.g. Edit name OR Edit phone
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        //add buttons in dialog to update
        builder.setPositiveButton("Update", (dialogInterface, i) -> {
            //input text from edit text
            final String value = editText.getText().toString().trim();
            //validate if user has entered something or not
            if (!TextUtils.isEmpty(value)) {
                pd.show();
                HashMap<String, Object> result = new HashMap<>();
                result.put(key, value);
                databaseReference.child(user.getUid()).updateChildren(result)
                        .addOnSuccessListener(aVoid -> {
                            //updated, dismiss progress
                            pd.dismiss();
                            Toast.makeText(getActivity(), "Updated...", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            //failed, dismiss progress, get and show error message
                            pd.dismiss();
                            Toast.makeText(getActivity(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                //if user edit is name, also change it from hist posts
                if (key.equals("name")) {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                    Query query = ref.orderByChild("uid").equalTo(uid);
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String child = ds.getKey();
                                snapshot.getRef().child(child).child("uName").setValue(value);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                    //update name in current users comments on posts
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String child = ds.getKey();
                                if (snapshot.child(child).hasChild("Comments")) {
                                    String child1 = "" + snapshot.child(child).getKey();
                                    Query child2 = FirebaseDatabase.getInstance().getReference("Posts").child(child1).child("Comments").orderByChild("uid").equalTo(uid);
                                    child2.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            for (DataSnapshot ds : snapshot.getChildren()) {
                                                String child = ds.getKey();
                                                snapshot.getRef().child(child).child("uName").setValue(value);
                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                }

            } else {
                Toast.makeText(getActivity(), "Please enter " + key, Toast.LENGTH_SHORT).show();
            }


        });

        //add buttons in dialog to cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //create and show dialog
        builder.create().show();

    }


    private void showImagePicDialog() {
        //show dialog containing options Camera and Gallery to pick the image

        String[] options = {"Camera", "Gallery"};
        //Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Pick Image From");
        //set items to dialog
        builder.setItems(options, (dialogInterface, i) -> {
            //handle dialog item clicks
            if (i == 0) {
                //Camera clicked

                if (!checkCameraPermission()) {
                    requestCameraPermission();
                } else {
                    pickFromCamera();
                    //Toast.makeText(getActivity(), "showImagePicDialog içinde, ÇALIŞIYOR 2", Toast.LENGTH_SHORT).show();
                }

            } else if (i == 1) {
                //Gallery clicked
                if (!checkStoragePermission()) {
                    requestStoragePermission();
                } else {
                    pickFromGallery();
                }
            }
        });
        //create and show dialog
        builder.create().show();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        /*This method called when user press Allow or Deny from permission request dialog
         * here we will handle permission cases(allowed+ denied)*/
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                //picking from camera, first check if camera and storage permissions allowed or not

                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {

                        //permissions enabled
                        pickFromCamera();

                        //Toast.makeText(getActivity(), "On request permissins result içinde , camera accepted", Toast.LENGTH_SHORT).show();

                    } else {
                        //permissions denied
                        Toast.makeText(getActivity(), "Please enable camera and storage permissions", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                //picking from gallery, first check if storage permission allowed or not
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED; //GRANTED 0 YAPTI 1 İDİ. NEDEN YAPTI?
                    if (writeStorageAccepted) {
                        //permissions enabled
                        pickFromGallery();
                    } else {
                        //permissions denied
                        Toast.makeText(getActivity(), "Please enable storage permission", Toast.LENGTH_SHORT).show();
                    }
                }

            }
            break;
        }


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //This method will be called after picking image from Camera or Gallery

        if (resultCode == RESULT_OK) {

            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //image is picked from gallery, get uri of image
                image_uri = data.getData();

                uploadProfileCoverPhoto(image_uri);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //image is picked from camera, get uri of image
                //image_uri = data.getData();
                //Toast.makeText(getActivity(), "On activity result içinde , camera. ÇALIŞIYOR 3", Toast.LENGTH_SHORT).show();
                uploadProfileCoverPhoto(image_uri);


            }

        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverPhoto(Uri uri) {
        //show progress
        pd.show();

        /*Instead of creating separate function for Profile Picture and Cover Photo
         * im doing work for both in same fuction
         *
         * To add check ill add a string variable and assign it value "image" when user clicks
         * "Edit PRofile Pic" , and assign it value "cover" when user clicks "Edit Cover Photo"
         * Here : iamge is the key in each user containing url of user's profile picture
         *        cover is the key in each user containing url of user's cover photo*/

        /*The parameter "image_uri" contains the uri of image picked either from camera or gallery
         * We will use UID of the currently signed in user as name of the image so there will be only one image
         * profile and one image for cover for each user*/

        //path and name of iamge to be storeed in firebase storage
        //e.g Users_Profile_Cover_Imgs/image_e12f3456f789.jpg
        //e.g Users_Profile_Cover_Imgs/cover_c123n4567g89.jpg
        String filePathAndName = storagePath + "" + profileOrCoverPhoto + "_" + user.getUid();

        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    //image is uploaded to storage, now get its url and store in user's databse
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();

                    while (!uriTask.isSuccessful()) ;
                    Uri downloadUri = uriTask.getResult();

                    //check if image is uploaded or not and url is received
                    if (uriTask.isSuccessful()) {
                        //image uploaded
                        //add/update url in user's database
                        HashMap<String, Object> results = new HashMap<>();
                        /*First parameter is profileORCoverPhoto that has value "image" or "cover"
                         * which are keys in user's database where url of image will be saved in one of them
                         * Second Parameter contains the url of the image stored in firebase storage, this
                         * url will be saved as a value against key "image" or "cover"*/
                        results.put(profileOrCoverPhoto, downloadUri.toString());
                        databaseReference.child(user.getUid()).updateChildren(results)
                                .addOnSuccessListener(aVoid -> {
                                    //url in databse of user is added successfully
                                    //dismiss progress bar
                                    pd.dismiss();
                                    Toast.makeText(getActivity(), "Image Updated...", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    //error adding url in databse of user
                                    //dismiss progress bar
                                    pd.dismiss();
                                    Toast.makeText(getActivity(), " Error Image Updating...", Toast.LENGTH_SHORT).show();

                                });

                        //if user edit is name, also change it from hist posts
                        if (profileOrCoverPhoto.equals("image")) {
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                            Query query = ref.orderByChild("uid").equalTo(uid);
                            query.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()) {
                                        String child = ds.getKey();
                                        snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

//                            update user image in currnet users comment on posts
                            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()) {
                                        String child = ds.getKey();
                                        if (snapshot.child(child).hasChild("Comments")) {
                                            String child1 = "" + snapshot.child(child).getKey();
                                            Query child2 = FirebaseDatabase.getInstance().getReference("Posts").child(child1).child("Comments").orderByChild("uid").equalTo(uid);
                                            child2.addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    for (DataSnapshot ds : snapshot.getChildren()) {
                                                        String child = ds.getKey();
                                                        snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());

                                                    }
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }

                    } else {
                        //error
                        pd.dismiss();
                        Toast.makeText(getActivity(), "Some error occured", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(e -> {
                    //there were some error(s), get and show error message, dismiss progress dialog
                    Toast.makeText(getActivity(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                });


    }

    private void pickFromCamera() {
        //Intent of picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        //put image uri
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        //intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //cameraIntent .setType("image/*"); //burası sanırım olmıycak burayı ben kendim gallery'den benzeterek koydum
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
        //Toast.makeText(getActivity(), "pickFromCamera içinde, ÇALIŞIYOR1", Toast.LENGTH_SHORT).show();
        //galleryActivityResultLauncher.launch(cameraIntent); //çalışmadı


    }

    private void pickFromGallery() {
        //pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
        //galleryActivityResultLauncher.launch(galleryIntent);
    }

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //user is signed in stay here
            //set email of logged in user
            //mProfileTv.setText(user.getEmail());
            uid = user.getUid();

        } else {
            //user not signed in, go to main activity
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        setHasOptionsMenu(true); //to show menu option in fragment
        super.onCreate(savedInstanceState);


    }

    /*inflate options menu*/
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //inflating menu
        inflater.inflate(R.menu.menu_main, menu);


        MenuItem item = menu.findItem(R.id.action_search);
        //v7 searchview of search user spesific posts
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called when user press search button
                if (!TextUtils.isEmpty(s)) {
                    //search
                    searchMyPosts(s);
                } else {
                    loadMyPosts();
                }


                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called whenever user type any letter
                if (!TextUtils.isEmpty(s)) {
                    //search
                    searchMyPosts(s);
                } else {
                    loadMyPosts();
                }


                return false;
            }
        });


        super.onCreateOptionsMenu(menu, inflater);
    }

    /*handle menu item clicks*/
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            firebaseAuth.signOut();
            checkUserStatus();
        }
        if (id == R.id.action_add_post) {
            startActivity(new Intent(getActivity(), AddPostActivity.class));

        }
        return super.onOptionsItemSelected(item);
    }

/*

//Alt kısmı startActivityForResult alternatifi olarak yazmıştım ama çalışmadı
    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                //here we will handle the result of our intent
                    if(result.getResultCode() == RESULT_OK){
                        //image picked
                        //get uri of image
                        Intent data = result.getData();
                        Uri imageUri = data.getData();
                        //uploadProfileCoverPhoto(imageUri);
                    }
                    else{
                        //cancelled
                        //Toast.makeText(ProfileFragment.this, "c", Toast.LENGTH_SHORT).show();
                    }
                }
            });
*/

}