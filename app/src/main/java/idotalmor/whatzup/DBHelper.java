package idotalmor.whatzup;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.backendless.Backendless;


/**
 * Created by Ido Talmor on 04/07/2017.
 */

public class DBHelper extends SQLiteOpenHelper { //Single Tone design Pattern

    private static DBHelper dbHelper;


    public static String ChatMembers="ChatMembers",ChatWindow="ChatWindow",SHPRF = "WhatPref";;

    private DBHelper(Context context) {//create database with user email as db name
        super(context, Backendless.UserService.CurrentUser().getEmail(),null,1);
    }

    public static DBHelper GetDBHelper(Context context){//get db single tone
        if (dbHelper == null) {
            dbHelper=new DBHelper(context);
        }
        return dbHelper;
    }

    public static void Terminate(){//terminate db single tone
        dbHelper = null;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ChatMembers+" ('_id' INTEGER PRIMARY KEY AUTOINCREMENT,`status` INTEGER ,`email` VARCHAR , `name` VARCHAR NOT NULL," +
                " `UserID` INTEGER NOT NULL, `UserImg` VARCHAR NOT NULL, `lastmsg` VARCHAR,`unread` INTEGER, `Time` TIMESTAMP DEFAULT CURRENT_TIMESTAM)");//can add un read num of msg

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
