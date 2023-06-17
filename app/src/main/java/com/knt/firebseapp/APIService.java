package com.knt.firebseapp;

import com.knt.firebseapp.notifications.Response;
import com.knt.firebseapp.notifications.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAFQfmWr4:APA91bHiyqhIeNVnxW4ZHSvWF6z4qQXZn5IwMS1HHdqKHnLyuPeUJfzSnintqYRV8FHhhoYtIgyZAXwlhIpP-nyme1Tw8AIcvGcVBauBewNGbfEeK-Y-nE1dhh6kBDYorQVuxwxVCHNG"

    })


    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);

}
