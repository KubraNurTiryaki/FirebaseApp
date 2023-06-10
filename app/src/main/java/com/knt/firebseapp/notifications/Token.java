package com.knt.firebseapp.notifications;

public class Token {
    /*An FCM Token, or much commanly known as a registrationToken
    * An ID is issued by the GCMconnection servers to the client app that allows it to receive messages*/

    String token;

    public Token(String token) {
        this.token = token;
    }

    public Token() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
