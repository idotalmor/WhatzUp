package idotalmor.whatzup;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.persistence.DataQueryBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;


/**
 * Created by Ido Talmor on 02/07/2017.
 */

public class BackendLessHelper{//Single Tone

    public static BackendLessHelper backendLessHelper;
    public final static String pname="name",pmail="email",pUserID="UserID",pImg="UserImg";
    public SQLiteDatabase db;
    Context context;


    private BackendLessHelper(Context context){
        this.context=context;
        db= DBHelper.GetDBHelper(context).getWritableDatabase();

    }

    public static void Terminate(){
        backendLessHelper = null;
    }

    public static BackendLessHelper GetBackEndLessHelper(Context context){
        if (backendLessHelper==null){backendLessHelper=new BackendLessHelper(context);}
        return backendLessHelper;
    }

    public boolean[] AddFriend(final String email, final boolean intent){ //the array - [if success][if user doesn't exist - true]
        final boolean [] b =new boolean[1];
        b[0]=false;
        Thread AddFR=new Thread(){
            @Override
            public void run() {

                Cursor dbcursor = db.rawQuery("SELECT * FROM "+DBHelper.ChatMembers+" WHERE email = ?",new String []{email});//check if user exist in local DB
                if(dbcursor!=null && dbcursor.getCount()>0){
                    dbcursor.moveToFirst();
                    if(intent){
                    Intent i = new Intent(context,Chat_Activity.class);
                    i.putExtra("UserID",dbcursor.getInt(dbcursor.getColumnIndexOrThrow(BackendLessHelper.pUserID)));
                    context.startActivity(i);}//move to chatActivity
                    b[0]=true;
                return;}//break thread

                //query for search user in server
                String whereClause = "email = '"+email+"'";
                DataQueryBuilder dataQueryBuilder = DataQueryBuilder.create();
                dataQueryBuilder.setWhereClause(whereClause);

                //sync search for the user in backendless server

                List<BackendlessUser> result = Backendless.Data.of(BackendlessUser.class).find(dataQueryBuilder);
                if(result.isEmpty()){return;}
                final BackendlessUser backendlessUser = result.get(0);//get the user from backendless server

                //save the user in local db
                SQLiteStatement stmt=db.compileStatement("INSERT INTO "+DBHelper.ChatMembers+" (status,email,name,UserID,UserImg)" +
                        " VALUES("+backendlessUser.getProperty("status")+",?,?,"+backendlessUser.getProperty(pUserID)+",?)");
                stmt.bindString(1,email);
                stmt.bindString(2,(String) backendlessUser.getProperty(pname));
                stmt.bindString(3, (String) backendlessUser.getProperty(pImg));
                stmt.execute();//Insert to db
                // create user chat table
                db.execSQL("CREATE TABLE IF NOT EXISTS "+DBHelper.ChatWindow+"_"+backendlessUser.getProperty(pUserID)+" ('_id' INTEGER PRIMARY KEY AUTOINCREMENT , `name` VARCHAR," +
                        " `UserID` INTEGER NOT NULL,`message` VARCHAR,`image` VARCHAR, `Time` TIMESTAMP DEFAULT CURRENT_TIMESTAM)");
                //upload picture from server
                new Thread(){
                    @Override
                    public void run() {
                       String imgfullpath =  AddPhotoFromBackendless((String)backendlessUser.getProperty(pImg),email,true);//Create new file and update db
                        if(imgfullpath!=null){
                        String str="UPDATE " + DBHelper.ChatMembers+" SET UserImg ='"+imgfullpath+"' WHERE email= '"+email+"'";
                        db.execSQL(str);}
                    }}.start();
                if(intent){
                Intent i = new Intent(context,Chat_Activity.class);
                i.putExtra("UserID",String.valueOf(backendlessUser.getProperty(BackendLessHelper.pUserID)));
                context.startActivity(i);}//move to chatActivity
                b[0]=true;
            }};

        AddFR.start();
        try {
            AddFR.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return b;
    }

    public static String AddPhotoFromBackendless(String img, String email, boolean b){
        if (img==null)return null;
        String url="Root folder Path On Server"+img;
        InputStream in = null;
        try {
            in = new URL(url).openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(in);//get bitmap from input stream

            String Directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/Whatsup/"+email; //create directory for images
            File directory = new File(Directory);
            if(!directory.exists())directory.mkdirs();

            File imgfile;
            if(b==true){imgfile = new File(Directory,"Profile.jpg");}//if need to download profile picture
            else{
                String[] imgname = img.split("/");//if not profile img take image name
                imgfile = new File(Directory,imgname[imgname.length-1]);//create img file container
            }

            if(!imgfile.exists()){imgfile.createNewFile();}
            else{return imgfile.getAbsolutePath();}


            FileOutputStream fos = new FileOutputStream(imgfile);//download image from backendless to container
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
            fos.flush();
            fos.close();
            in.close();
            return imgfile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();return null;
        }}

}
