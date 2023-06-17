package com.knt.firebseapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.knt.firebseapp.ChatActivity;
import com.knt.firebseapp.R;
import com.knt.firebseapp.ThereProfileActivity;
import com.knt.firebseapp.models.ModelUser;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.MyHolder> {

    Context context;
    List<ModelUser> userList;

    //constructor
    public AdapterUsers(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
       //inflate layout(row_user.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.row_users, parent, false);


        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        //get data
        final String theirUID = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        final String userEmail = userList.get(position).getEmail();

        //set data
        holder.mNameTv.setText(userName);
        holder.mEmailTv.setText(userEmail);

        try {
            Picasso.get().load(userImage).
                    placeholder(R.drawable.ic_default_img)
                    .into(holder.mAvatarIv);
        }catch (Exception e ){

        }

        //handle item click
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                //show dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (which==0){
                            //profile clicked
                            /*click to go ThereProfileActivity with uid, this uid is of clicked user
                            * which will be used to show user spesific data/posts*/
                            Intent intent = new Intent(context, ThereProfileActivity.class);
                            intent.putExtra("uid", theirUID);                                           //burda hata yok korkma
                            context.startActivity(intent);


                        }
                        if (which==1){
                            /*Click user from user list to start chatting/messaging
                             * Start activity by puttinh UID of receğiver
                             * we will use that UID to identife the user we are gonna chat*/

                            Intent intent = new Intent(context, ChatActivity.class);
                            intent.putExtra("theirUid", theirUID);                                      //burda DA hata yok korkma
                            context.startActivity(intent);
                        }


                    }
                });
                builder.create().show();


            }
        });






    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    //view holder class
    static class MyHolder extends RecyclerView.ViewHolder{

        ImageView mAvatarIv;
        TextView mNameTv, mEmailTv;



        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init views
            mAvatarIv = itemView.findViewById(R.id.avatarIv);
            mNameTv = itemView.findViewById(R.id.nameTv);
            mEmailTv = itemView.findViewById(R.id.emailTv);


        }
    }





}
