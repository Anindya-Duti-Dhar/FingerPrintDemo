package com.example.duti.fingerprintdemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.duti.fingerprintdemo.Constant.DATABASE_VERSION;
import static com.example.duti.fingerprintdemo.Constant.DB_NAME;

/**
 * Created by Imrose on 9/22/2017.
 */

public class Repository<T> extends SQLiteOpenHelper implements IRepository<T> {

    T object;
    public String mTableName = "";
    private String primaryKeyField = "";
    String mPKField = "";
    List<String> excludeFields = new ArrayList<>();
    HashMap<String, String> tableMap = new HashMap<>();

    public Repository(Context context, T object) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        this.object = object;

        mTableName = object.getClass().getName().toString().replace(object.getClass()
                .getPackage().toString().replace("package ", ""), "").replace(".", "");
    }

    public String getTableName() {
        return mTableName;
    }

    //if class name is not same as table name.. so need to map..
    public void addInCustomTableMap(String className, String tableName) {
        tableMap.put(className, tableName);
    }

    public List<String> getExcludeFields() {
        return excludeFields;
    }

    public void addExcludeFields(String excludeField) {
        this.excludeFields.add(excludeField);
    }


    private void getPrimaryKey() {
        SQLiteDatabase db = this.getWritableDatabase();
        String fieldName = "";
        Cursor c = null;
        try {
            c = db.rawQuery("pragma table_info(" + mTableName + ");", null);
            while (c != null && c.moveToNext()) {
                int fieldValue = c.getInt(c.getColumnIndex("pk"));
                if (fieldValue == 1) fieldName = c.getString(c.getColumnIndex("name"));
                break;
            }
            c.close();
            primaryKeyField = fieldName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        db.close();
    }


    //region Create Table SQL
    public String create(String primaryKey, boolean isAutoIncremental) {
        String sql = "", fieldName = "", fieldType = "";
        String s = object.toString();
        String s2 = s.substring(s.indexOf("{"));
        String[] objectToString = s2.replace("{", "").replace("}", "").split(",");
        for (int i = 0; i < objectToString.length; i++) {
            fieldName = objectToString[i].split("=")[0];
            fieldType = getFieldType(object, fieldName);
            if (!TextUtils.isEmpty(fieldType)) {
                if (fieldName.equals(primaryKey)) {
                    fieldName = getAutoIncrementalPrimaryKey(fieldName + " " + fieldType, isAutoIncremental);
                    sql = sql + fieldName + ", ";
                } else {
                    sql = sql + fieldName + " " + fieldType + ", ";
                }
            }
        }
        String finalStatement = "CREATE TABLE IF NOT EXISTS " + mTableName + "(" + sql.substring(0, sql.length() - 2) + ")";
        Log.i("duti", "Table Statement: "+finalStatement);
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(finalStatement);
        db.close();
        return finalStatement;
    }

    private String getAutoIncrementalPrimaryKey(String primaryKeyFieldName, boolean isAutoIncremental) {
        if (isAutoIncremental) {
            primaryKeyField = primaryKeyFieldName;
            return primaryKeyFieldName + " PRIMARY KEY AUTOINCREMENT";
        } else return primaryKeyFieldName + " PRIMARY KEY";
    }

    private String getFieldType(Object obj, String fieldName) {
        String fieldTypeName = "";
        Field field = null;
        try {
            field = obj.getClass().getDeclaredField(fieldName.trim());
            fieldTypeName = getSqlFieldType(field.toString());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return fieldTypeName;
    }

    private String getSqlFieldType(String s) {
        String type = "";
        if (s.contains("String")) type = " TEXT";
        else if (s.contains("byte[]")) type = " BLOB";
        else if (s.contains("long")) type = " INTEGER";
        else if (s.contains("int")) type = " INTEGER";
        else type = "";
        return type;
    }
    //endregion

    public void insertOldStyle(int userId, String username, String userAddress, byte[] image) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("UserId", userId);
        values.put("Username", username);
        values.put("UserAddress", userAddress);
        values.put("FingerPrint", image);
        db.insert(mTableName, null, values);
        db.close();
    }

    @Override
    public String add(T item) {

        getPrimaryKey();

        ContentValues values = new ContentValues();
        SQLiteDatabase db = this.getWritableDatabase();

        for (Field field : item.getClass().getDeclaredFields()) {
            field.setAccessible(true); // if you want to modify private fields

            try {
                if (!field.getName().toString().equalsIgnoreCase(mPKField)) {
                    String fieldName = field.getName().toString();
                    if (fieldName.equalsIgnoreCase("$change")) {
                    } else if (fieldName.equalsIgnoreCase("serialVersionUID")) {
                    } else if (excludeFields.contains(fieldName)) {
                    } else {
                        if (!field.getName().toString().equals(primaryKeyField))
                            values.put(field.getName().toString(), field.get(item) != null ? field.get(item).toString() : "");
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        long val = db.insert(mTableName, null, values);

        String sql = "SELECT last_insert_rowid()";
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor != null) cursor.moveToFirst();
        String sId = cursor.getString(0);
        db.close();
        return sId;
    }

    @Override
    public void add(ArrayList<T> items) {
        for (T o : items) {
            add(o);
        }
    }

    @Override
    public void update(T item, String where, String[] whereArgs) {

        ContentValues values = new ContentValues();
        SQLiteDatabase db = this.getWritableDatabase();

        for (Field field : item.getClass().getDeclaredFields()) {
            field.setAccessible(true); // if you want to modify private fields

            try {
                String fieldName = field.getName().toString();
                if (fieldName.equalsIgnoreCase("$change")) {
                } else if (fieldName.equalsIgnoreCase("serialVersionUID")) {
                } else if (excludeFields.contains(fieldName)) {
                } else
                    values.put(field.getName().toString(), field.get(item) != null ? field.get(item).toString() : "");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        db.update(mTableName, values, where, whereArgs);
        db.close();
    }

    @Override
    public void remove(String where, String[] whereArgs) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(mTableName, where, whereArgs);
        db.close();
    }

    @Override
    public void removeAll(String sWhere) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (sWhere != "") {
            db.execSQL("DELETE FROM " + mTableName + " WHERE " + sWhere);
        } else {
            db.execSQL("DELETE FROM " + mTableName);
        }
        db.close();
    }

    private JSONArray getJSON(Cursor cursor) {

        JSONArray resultSet = new JSONArray();

        cursor.moveToFirst();
        while (cursor.isAfterLast() == false) {

            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();

            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        if (cursor.getString(i) != null) {
                            Log.d("TAG_NAME", cursor.getString(i));
                            rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                        } else {
                            rowObject.put(cursor.getColumnName(i), "");
                        }
                    } catch (Exception e) {
                        Log.d("TAG_NAME", e.getMessage());
                    }
                }
            }
            resultSet.put(rowObject);
            cursor.moveToNext();
        }
        cursor.close();
        Log.d("TAG_NAME", resultSet.toString());
        return resultSet;
    }

    @Override
    public JSONArray getAllJsonArray() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT  * FROM " + mTableName;

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) cursor.moveToFirst();

        JSONArray jArr = getJSON(cursor);

        return jArr;
    }

    @Override
    public String getAllJsonString(String rootKey, String orderBy) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + mTableName + " " + orderBy;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) cursor.moveToFirst();

        JSONArray jArr = getJSON(cursor);
        String jsonStr = jArr.toString();

        if (rootKey != "") jsonStr = rootKey + " : {" + jsonStr + "}";

        return jsonStr;
    }

    @Override
    public Object getAll(String orderBy, Class<?> cls) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + mTableName + " " + orderBy;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null)
            cursor.moveToFirst();

        JSONArray jArr = getJSON(cursor);

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        Object obj = gson.fromJson(jArr.toString(), cls);
        db.close();

        return obj;
    }

    @Override
    public Object get(String where, String orderBy, Class<?> cls) {
        SQLiteDatabase db = this.getReadableDatabase();

        if (!TextUtils.isEmpty(tableMap.get(mTableName))) {
            mTableName = tableMap.get(mTableName);
        }

        String selectQuery = "SELECT * FROM " + mTableName + " WHERE " + where + " " + orderBy;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null)
            cursor.moveToFirst();

        JSONArray jArr = getJSON(cursor);

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        Object obj = gson.fromJson(jArr.toString(), cls);
        db.close();

        return obj;

    }

    @Override
    public String getFiledValue(String fieldName, String sql) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor != null) cursor.moveToFirst();
        String _fieldName = cursor.getString(cursor.getColumnIndex(fieldName));
        db.close();
        return _fieldName;
    }

    @Override
    public String getSpecificFiledValue(String fieldName, String fieldValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "select * from " + mTableName + " where " + fieldName + " = '" + fieldValue + "'";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) cursor.moveToFirst();
        String value = cursor.getString(cursor.getColumnIndex(fieldName));
        db.close();
        return value;
    }

    @Override
    public byte[] getByteForImage(String whereParam, String fieldName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "select * from " + mTableName + " where " + whereParam;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) cursor.moveToFirst();
        byte[] image = cursor.getBlob(cursor.getColumnIndex(fieldName));
        db.close();
        return image;
    }

    @Override
    public int getCountAgainstField(String fieldName, String fieldValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "select * from " + mTableName + " where " + fieldName + " = '" + fieldValue + "'";
        Cursor cursor = db.rawQuery(query, null);
        int count = cursor.getCount();
        db.close();
        return count;
    }


    @Override
    public void executeQuery(String sql) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(sql);
        db.close();
    }

    @Override
    public String getRecordCount(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(*) count from " + tableName, null);
        if (cursor != null) cursor.moveToFirst();
        String _fieldName = cursor.getString(cursor.getColumnIndex("count"));
        db.close();
        return _fieldName;
    }

    public long getRecordCount(String where, int move) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(*) count from " + mTableName + " where " + where, null);
        if (cursor != null) cursor.moveToFirst();
        String rowCoount = cursor.getString(cursor.getColumnIndex("count"));
        db.close();
        return Long.parseLong(rowCoount);
    }

    public long getRecordCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(*) count from " + mTableName, null);
        if (cursor != null) cursor.moveToFirst();
        String _fieldName = cursor.getString(cursor.getColumnIndex("count"));
        db.close();
        return Long.parseLong(_fieldName);
    }

    public boolean isRecordExists(String where) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(*) count from " + mTableName + " where " + where, null);
        if (cursor != null) cursor.moveToFirst();
        long _fieldVal = cursor.getLong(cursor.getColumnIndex("count"));
        db.close();
        if (_fieldVal == 0) return false;
        else return true;
    }

    public long getRecordMaxValue(String fieldName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select max(" + fieldName + ") maxid from " + mTableName, null);
        if (cursor != null) cursor.moveToFirst();
        long _val = cursor.getLong(cursor.getColumnIndex("maxid"));
        db.close();
        return _val;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }

    // area duti start

    @Override
    public int getAllDataCount() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "select * from " + mTableName;
        Cursor cursor = db.rawQuery(query, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }

    @Override
    public void insertAllIntoDatabase(List<T> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            for (T o : items) {
                for (Field field : o.getClass().getDeclaredFields()) {
                    field.setAccessible(true);   // if you want to modify private fields
                    try {
                        values.put(field.getName().toString(), field.get(o) != null ? field.get(o).toString() : "");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                db.insert(mTableName, null, values);
            }
            db.setTransactionSuccessful();
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.i("duti", "error transaction: " + e.toString());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Log.i("duti", "error transaction: " + e.toString());
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    @Override
    public Object getAllDataFromDatabase(String queryStatement, Class<?> objectClass) {
        SQLiteDatabase db = this.getWritableDatabase();

        String selectQuery = "SELECT  * FROM " + mTableName + " " + queryStatement;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null)
            cursor.moveToFirst();

        JSONArray jsonArray = getJSON(cursor);
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        Object object = gson.fromJson(jsonArray.toString(), objectClass);
        db.close();

        return object;
    }

    @Override
    public void removeAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(mTableName, null, null);
        db.close();
    }

    @Override
    public int getMaxRecordIdValue(String fieldName) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("select max(" + fieldName + ") maxid from " + mTableName, null);
        if (cursor != null) cursor.moveToFirst();
        int value = cursor.getInt(cursor.getColumnIndex("maxid"));
        db.close();
        return value;
    }

}