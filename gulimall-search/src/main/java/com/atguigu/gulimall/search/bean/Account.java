package com.atguigu.gulimall.search.bean;

import lombok.Data;

//@ToString
@Data
public class Account{
    private int account_number;
    private int balance;
    private String firstname;
    private String lastname;
    private int age;
    private String gender;
    private String address;
    private String employer;
    private String email;
    private String city;
    private String state;
}