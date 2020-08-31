package com.example.demo.model;

public class User {
    private int id;
    private String userName;

    public User(int id, String userName){
        this.id = id;
        this.userName = userName;
    }

    public int getId(){ return this.id;}
    public String getUserName(){ return this.userName;}
}