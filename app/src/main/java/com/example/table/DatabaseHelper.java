package com.example.table;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "table.db";
    public static final String TABLE_NAME = "table_data";
    public static final String COL_1 = "SAVE_NAME";
    public static final String COL_2 = "RANDOM_SEED";
    public static final String COL_3 = "CREATION_DATE";
    public static final String COL_4 = "USER_ARRAY";
    public static final String COL_5 = "IS_HARD";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (SAVE_NAME TEXT PRIMARY KEY, RANDOM_SEED TEXT, CREATION_DATE TEXT, USER_ARRAY TEXT, IS_HARD INTEGER) ");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String save_name, String random_seed, String creation_date, String user_array, Integer is_hard){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1, save_name);
        contentValues.put(COL_2, random_seed);
        contentValues.put(COL_3, creation_date);
        contentValues.put(COL_4, user_array);
        contentValues.put(COL_5, is_hard);
        long result = db.insert(TABLE_NAME, null, contentValues);

        return result != -1;
    }
    public String fetch_seed(String save_name){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("select random_seed from table_data where save_name = '" + save_name +"'", null);
        cursor.moveToFirst();
        String seed = cursor.getString(0);
        cursor.close();
        return seed;
    }
    public String fetch_array(String save_name) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("select user_array from table_data where save_name = '" + save_name + "'", null);
        cursor.moveToFirst();
        String array = cursor.getString(0);
        cursor.close();
        return array;
    }

    public Cursor get_data(){
        SQLiteDatabase db = this.getWritableDatabase();
        //Cursor res = db.rawQuery("select save_name, creation_date from table_data where save_name ='savediag'", null);
        return db.rawQuery("select save_name, creation_date, random_seed from table_data order by creation_date asc", null);
    }

    public Boolean update_data(String save_name, String random_seed, String creation_date, String user_array, Integer is_hard){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1, save_name);
        contentValues.put(COL_2, random_seed);
        contentValues.put(COL_3, creation_date);
        contentValues.put(COL_4, user_array);
        contentValues.put(COL_5, is_hard);
        db.update(TABLE_NAME, contentValues, "save_name = ?", new String[] {save_name});
        return true;
    }

    public Integer delete_data(String save_name){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "save_name = ?", new String[] {save_name});

    }
}
