package com.example.duti.fingerprintdemo;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Imrose on 9/22/2017.
 */

public interface IRepository<T> {

    String add(T item);

    void add(ArrayList<T> items);

    void update(T item, String where, String[] whereArgs);

    void remove(String where, String[] whereArgs);

    void removeAll(String sWhere);

    JSONArray getAllJsonArray();

    String getAllJsonString(String rootKey, String orderBy);

    Object getAll(String orderBy, Class<?> cls);

    Object get(String where, String orderBy, Class<?> cls);

    String getFiledValue(String fieldName, String sql);

    public String getSpecificFiledValue(String fieldName, String fieldValue);

    public int getCountAgainstField(String fieldName, String fieldValue);
    void executeQuery(String sql);

    String getRecordCount(String tableName);


    // area duti start

    int getAllDataCount();

    void insertAllIntoDatabase(List<T> items);

    Object getAllDataFromDatabase(String queryStatement, Class<?> objectClass);

    void removeAll();

    int getMaxRecordIdValue(String fieldName);

    byte[] getByteForImage(String sqlQuery, String fieldName);

    void insertOldStyle(int userId, String username, String userAddress, byte[] image);

}