package idotalmor.whatzup;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.files.BackendlessFile;
import com.backendless.messaging.MessageStatus;
import com.backendless.messaging.PublishOptions;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;
import idotalmor.whatzup.Adapters.MsglistAdapter;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by Ido Talmor on 13/07/2017.
 */

public class Chat_Activity extends AppCompatActivity {

    public static final int CAMERA = 1,GALLERY=2,WRITE_STORAGE=2;
    int fUserID;
    CircleImageView fUserImg;
    TextView fUserName;
    ListView lv;
    EditText msgtxt;
    String fUName,msgtxtstr,imgpath, fUEmail;
    String [] photoPermissionParams;
    Dialog dialog;
    File ImgFile;
    Bitmap bm;
    PublishOptions publishOptions;
    SQLiteDatabase db;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_window);

        db=DBHelper.GetDBHelper(this).getReadableDatabase();

        fUserImg =(CircleImageView) findViewById(R.id.chatwindowimg);
        fUserName =(TextView)findViewById(R.id.chatusername);
        lv=(ListView)findViewById(R.id.msglistview);
        msgtxt=(EditText)findViewById(R.id.goingmsgtextview);


        fUserID = getIntent().getIntExtra("UserID",-1);
        if(fUserID<0){this.finish();}//if user id is smaller than 0
        String query = "SELECT * FROM "+DBHelper.ChatMembers+" WHERE UserID = '"+ fUserID +"'"; //get user to chat
        Cursor cursor = db.rawQuery(query, null);
        if(cursor==null||cursor.getCount()<=0){this.finish();}
        cursor.moveToFirst();
        String uimg=cursor.getString(cursor.getColumnIndexOrThrow(BackendLessHelper.pImg));
        fUName =cursor.getString(cursor.getColumnIndexOrThrow(BackendLessHelper.pname));
        fUEmail =cursor.getString(cursor.getColumnIndexOrThrow(BackendLessHelper.pmail));

        fUserName.setText(fUName);
        if(uimg!=null){Bitmap bm = BitmapFactory.decodeFile(uimg);
        if(bm!=null){
            fUserImg.setImageBitmap(bm);}}
        getmsg();

    }


    public void getmsg(){
        String query = "SELECT * FROM "+DBHelper.ChatWindow+"_"+ fUserID +" WHERE message IS NOT NULL"; //get table query - each user have msg table
        Cursor cursor = db.rawQuery(query, null);//get table cursor
        lv.setAdapter(new MsglistAdapter(this,cursor));

    }


    public void send(View view) {
        msgtxtstr=msgtxt.getText().toString();
        publishOptions = new PublishOptions();
        publishOptions.setPublisherId(String.valueOf(Backendless.UserService.CurrentUser().getProperty(BackendLessHelper.pUserID)));
        if(imgpath!=null){
            String[] imgarray = imgpath.split("/");
            String imgname = imgarray[imgarray.length-1];
            bm= BitmapFactory.decodeFile(imgpath);
            Backendless.Files.Android.upload(bm, Bitmap.CompressFormat.PNG, 10, imgname, "IMG/"+ fUEmail, false, new AsyncCallback<BackendlessFile>() {
                @Override
                public void handleResponse(BackendlessFile backendlessFile) {

                }

                @Override
                public void handleFault(BackendlessFault backendlessFault) {

                }
            });

            publishOptions.putHeader("IMG","IMG/"+ fUEmail +"/"+imgname);}
        publishOptions.putHeader("NAME", fUName);
        publishOptions.putHeader("EMAIL", fUEmail);
        publishOptions.putHeader( "android-ticker-text", "You just got a push notification!" );
        publishOptions.putHeader( "android-content-title", fUName +" Send You A Message!");
        publishOptions.putHeader( "android-content-text", msgtxtstr );
        Backendless.Messaging.publish("CHN"+fUserID,(Object)msgtxtstr , publishOptions, new AsyncCallback<MessageStatus>() {
            @Override
            public void handleResponse(MessageStatus messageStatus) {
                String str = "INSERT INTO "+DBHelper.ChatWindow+"_"+fUserID+" (name,UserID,message,image,Time) VALUES (?,"+fUserID+",?,?,"+System.currentTimeMillis()+")";
                SQLiteStatement stmt = db.compileStatement(str);

                stmt.bindString(1,fUName);
                stmt.bindString(2,msgtxtstr);
                if(imgpath==null)imgpath="";
                stmt.bindString(3,imgpath);
                stmt.execute();
                imgpath="";
                ImgFile=null;
                getmsg();
                msgtxt.setText("");
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {

            }
        });
    }


    public void addimg(View view) {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.add_photo);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));//transparent background for xml corner radius

        TextView camera = (TextView)dialog.findViewById(R.id.cameraPickerBtn);
        TextView gallery = (TextView)dialog.findViewById(R.id.galleryPickerBtn);
        TextView cancel = (TextView)dialog.findViewById(R.id.cancelPickPhotoBtn);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoPermissionParams = new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA};//permission needed for camera use
                if(EasyPermissions.hasPermissions(Chat_Activity.this,photoPermissionParams)){camera();}//if the app already had the permission go to camera
                else{//if the app doesn't have camera permission
                    EasyPermissions.requestPermissions(Chat_Activity.this,"Please Enable Camera Permission",CAMERA,photoPermissionParams);
                }
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoPermissionParams = new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE};//permission needed for gallery use
                if(EasyPermissions.hasPermissions(Chat_Activity.this,photoPermissionParams)){gallery();}//if the app already had the permission go to gallery
                else{//if the app doesn't have gallery permission
                    EasyPermissions.requestPermissions(Chat_Activity.this,"Please Enable Gallery Permission",GALLERY,photoPermissionParams);
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();


    }


    private void camera() {

        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//implicit Intent - for image picker from camera
        generateImageFile();//create File object
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            // only for Marshmallow and older versions
            i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(ImgFile));//pass Uri with File object - for storage(with extra output key)
        }else {
            Uri photoURI = FileProvider.getUriForFile(Chat_Activity.this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    ImgFile);
            i.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);//pass Uri with File object - for storage(with extra output key)
        }
        startActivityForResult(i, CAMERA);//go to camera
    }

    private void gallery(){
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//implicit Intent - for image picker from gallery
        startActivityForResult(i,GALLERY);//open gallery to pick image
    }

    private void generateImageFile(){
        String fileName="IMG_"+ System.currentTimeMillis()+".jpg";
        ImgFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)+"/"+fileName);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent i) {

        if(resultCode==RESULT_OK){
            switch (requestCode){
                case GALLERY:{
                    imgpath=getRealPathFromUrl(i.getData());break;}
                case CAMERA:
                    imgpath = ImgFile.getAbsolutePath();break;
            }
            bm = BitmapFactory.decodeFile(imgpath);
            dialog.dismiss();
        }
    }

    public void BackPress(View v){
        this.finish();
    }
    public String getRealPathFromUrl(Uri contentUri){//get full string path from uri
        Cursor cursor = getContentResolver().query(contentUri,null,null,null,null);
        if(cursor == null){
            return contentUri.getPath();
        }else {
            cursor.moveToNext();
            int idImg = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idImg);
        }
}

}

