package idotalmor.whatzup;

import android.*;
import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import idotalmor.whatzup.Adapters.ChatlistAdapter;
import pub.devrel.easypermissions.EasyPermissions;


/**
 * Created by Ido Talmor on 01/07/2017.
 */

public class ChatListAc extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    BackendlessUser currentUser;
    CircleImageView profileImg;
    String imgpath,UserId;
    String [] profilePhotoParams;
    final int profilePIC = 1;
    CoordinatorLayout coordinatorLayout;
    TextView name;
    SharedPreferences prefs;
    BackendLessHelper BC;
    SQLiteDatabase db;
    ListView LV;
    Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatlist_layout);
        coordinatorLayout = (CoordinatorLayout)findViewById(R.id.coordinatorLayoutchatList);

        currentUser = Backendless.UserService.CurrentUser();
        BC=BackendLessHelper.GetBackEndLessHelper(this);
        db=DBHelper.GetDBHelper(this).getWritableDatabase();
        prefs = getSharedPreferences(DBHelper.SHPRF,MODE_PRIVATE);

        profileImg = (CircleImageView) findViewById(R.id.ProfileImg);

        name=(TextView)findViewById(R.id.TextName);
        name.setText((String) currentUser.getProperty(BackendLessHelper.pname));//on server name can not be null

        LV=(ListView) findViewById(R.id.memberlist);
        handler = new Handler(Looper.getMainLooper());

        UserId = String.valueOf(currentUser.getProperty(BackendLessHelper.pUserID));
        imgpath = prefs.getString(UserId,null);//get imgpath frm shared preferences

        if(imgpath!=null){//if imgpath stored on shared preferences
            profileImg.setImageURI(Uri.fromFile(new File(imgpath)));
        }

        else{
            if (currentUser.getProperty(BackendLessHelper.pImg)!=null){
                profilePhotoParams = new String []{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
                if(EasyPermissions.hasPermissions(this,profilePhotoParams)){
                    ProfileImgBC();
                }
                else{
                    requestPermissions(profilePhotoParams,profilePIC);
                }
            }

        }
    }

    public void ProfileImgBC(){
        new Thread(){
            @Override
            public void run() {
                String img=(String) currentUser.getProperty(BackendLessHelper.pImg);//img path on server
                imgpath = BackendLessHelper.AddPhotoFromBackendless(img,currentUser.getEmail(),true);
                if (imgpath==null)return;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        profileImg.setImageURI(Uri.fromFile(new File(imgpath)));
                    }
                });
                prefs.edit().putString(UserId,imgpath).apply();
                        super.run();
            }
        }.start();

    }

    @Override
    protected void onStart() {
        getcontacts();
        super.onStart();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,ChatListAc.this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if(!EasyPermissions.hasPermissions(ChatListAc.this,profilePhotoParams)){return;}//if not all permissions granted
        switch (requestCode){
            case profilePIC :{ProfileImgBC();break;}
        }
    }

    @Override
    public void onPermissionsDenied(final int requestCode, final List<String> perms) {
        Snackbar snackbar = Snackbar//snack bar for re-requesting missing permissions
                .make(coordinatorLayout, "Need Permission In Order To Use Application ", Snackbar.LENGTH_LONG)
                .setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        requestPermissions(perms.toArray(new String[perms.size()]),requestCode);
                    }
                });

// Changing message text color
        snackbar.setActionTextColor(Color.RED);

// Changing action button text color
        View sbView = snackbar.getView();

        TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);//taking
        textView.setTextColor(Color.GREEN);
        snackbar.show();
    }

    public void AddDialog(View view) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.add_friend_dialog);
        final EditText editText = (EditText)dialog.findViewById(R.id.AddFriendEdTxt);
        Button button = (Button)dialog.findViewById(R.id.addFriendBtn);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                String email = editText.getText().toString();
                if(BC.AddFriend(email,true)[0]!=true){
                    LoginAc.ToastMSG(ChatListAc.this,"Couldn't find user "+email);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void getcontacts(){
        String query = "SELECT * FROM "+DBHelper.ChatMembers;
        Cursor cursor = db.rawQuery(query, null);
        LV.setAdapter(new ChatlistAdapter(this,cursor));

    }


    public void SignOut(View view) {
        Backendless.UserService.logout(new AsyncCallback<Void>() {
            @Override
            public void handleResponse(Void aVoid) {
                //terminate single tone object
                BackendLessHelper.Terminate();
                DBHelper.Terminate();

                Intent i = new Intent(ChatListAc.this,LoginAc.class);
                startActivity(i);
                Backendless.Messaging.unregisterDevice();//unregister device
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {

            }
        });
    }

    @Override
    public void onBackPressed() {//exit app when pressing back in chatlist window
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}


