package com.example.duti.fingerprintdemo;


import java.util.Arrays;

public class User {

    int RecordId;
    int UserId;
    String Username;
    String UserAddress;
    byte[] FingerPrint;

    public int getRecordId() {
        return RecordId;
    }

    public void setRecordId(int recordId) {
        RecordId = recordId;
    }

    public int getUserId() {
        return UserId;
    }

    public void setUserId(int userId) {
        UserId = userId;
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public String getUserAddress() {
        return UserAddress;
    }

    public void setUserAddress(String userAddress) {
        UserAddress = userAddress;
    }

    public byte[] getFingerPrint() {
        return FingerPrint;
    }

    public void setFingerPrint(byte[] fingerPrint) {
        FingerPrint = fingerPrint;
    }

    @Override
    public String toString() {
        return "User{" +
                "RecordId=" + RecordId +
                ", UserId=" + UserId +
                ", Username='" + Username + '\'' +
                ", UserAddress='" + UserAddress + '\'' +
                ", FingerPrint=" + Arrays.toString(FingerPrint) +
                '}';
    }
}
