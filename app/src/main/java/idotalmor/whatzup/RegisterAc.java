package idotalmor.whatzup;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.files.BackendlessFile;
import java.io.File;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by Ido Talmor on 01/07/2017.
 */

public class RegisterAc extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{

    public static final int CAMERA = 1,GALLERY=2;
    CoordinatorLayout coordinatorLayout;
    ImageView delimg;
    CircleImageView img;
    Dialog dialog;
    private EditText nameFieldR,emailFieldR, passwordFieldR;
    String [] photoPermissionParams;
    String imgpath;
    File ImgFile;
    Bitmap bm;
    BackendlessUser user;
    SharedPreferences prefs;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);
        Backendless.initApp(this,LoginAc.AppId, LoginAc.AndroidKey);

        coordinatorLayout = (CoordinatorLayout)findViewById(R.id.coordinatorLayout);

        nameFieldR = (EditText) findViewById(R.id.nameField);
        emailFieldR = (EditText) findViewById(R.id.emailFieldReg);
        passwordFieldR = (EditText) findViewById(R.id.textPasswordReg);

        passwordFieldR.setOnEditorActionListener(new TextView.OnEditorActionListener() {//set up done key to register
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    RegisterBtn(textView);
                    return true;
                }
                else {
                    return false;
                }
            }
        });

        img = (CircleImageView) findViewById(R.id.btnImgRegister);
        delimg = (ImageView)findViewById(R.id.deleteimg);
        prefs = getSharedPreferences(DBHelper.SHPRF,MODE_PRIVATE);
    }


    public void selectProfileImg(View view) {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.add_photo);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));//transparent background for xml corner radius

        TextView camera = (TextView)dialog.findViewById(R.id.cameraPickerBtn);
        TextView gallery = (TextView)dialog.findViewById(R.id.galleryPickerBtn);
        TextView cancel = (TextView)dialog.findViewById(R.id.cancelPickPhotoBtn);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoPermissionParams = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA};//permission needed for camera use
                if(EasyPermissions.hasPermissions(RegisterAc.this,photoPermissionParams)){camera();}//if the app already had the permission go to camera
                else{//if the app doesn't have camera permission
                    EasyPermissions.requestPermissions(RegisterAc.this,"Please Enable Camera Permission",CAMERA,photoPermissionParams);
                }
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoPermissionParams = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};//permission needed for gallery use
                if(EasyPermissions.hasPermissions(RegisterAc.this,photoPermissionParams)){gallery();}//if the app already had the permission go to gallery
                else{//if the app doesn't have gallery permission
                    EasyPermissions.requestPermissions(RegisterAc.this,"Please Enable Gallery Permission",GALLERY,photoPermissionParams);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,RegisterAc.this);
    }


        @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
       if(!EasyPermissions.hasPermissions(RegisterAc.this,photoPermissionParams)){return;}//if not all permissions granted
            switch (requestCode){
            case CAMERA :{camera();break;}
            case GALLERY:{gallery();break;}
        }
    }

    @Override
    public void onPermissionsDenied(final int requestCode, final List<String> perms) {
        dialog.dismiss();
        String str = "";
        switch (requestCode){
            case CAMERA:{str = "Camera";break;}
            case GALLERY:{str = "Gallery";break;}
        }
        Snackbar snackbar = Snackbar//snack bar for re-requesting missing permissions
                .make(coordinatorLayout, "Need Permission In Order To Use "+str, Snackbar.LENGTH_LONG)
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

    private void camera() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//implicit Intent - for image picker from camera
        generateImageFile();//create File object
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            // only for Marshmallow and older versions
            i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(ImgFile));//pass Uri with File object - for storage(with extra output key)
        }else {
            Uri photoURI = FileProvider.getUriForFile(RegisterAc.this,
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i) {

        if(resultCode==RESULT_OK){
            switch (requestCode){
                case GALLERY:{
                    imgpath=getRealPathFromUrl(i.getData());
                    break;}
                case CAMERA:
                    imgpath = ImgFile.getAbsolutePath();
                    break;}
            dialog.dismiss();
            if(imgpath==null)return;
            bm = BitmapFactory.decodeFile(imgpath);
            img.setImageBitmap(bm);
            delimg.setVisibility(View.VISIBLE);
        }else{dialog.dismiss();}
    }

    public String getRealPathFromUrl(Uri contentUri){//get full string path from uri
        Cursor cursor = getContentResolver().query(contentUri,null,null,null,null);
        if(cursor == null){
            return contentUri.getPath();
        }else {
            cursor.moveToNext();
            int idImg = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idImg);
        }}

    public void DelImgBtn(View view) {//delete img btn
        imgpath=null;//reset img store parameters
        ImgFile=null;
        img.setImageResource(R.drawable.profilepicplaceholder);//set default img
        delimg.setVisibility(View.GONE);//dissmiss del btn
    }


    public void RegisterBtn(View v){
        final String name= nameFieldR.getText().toString();
        final String email=emailFieldR.getText().toString();
        final String password=passwordFieldR.getText().toString();

        if(name.isEmpty()||email.isEmpty()||password.isEmpty()){
            Snackbar snackbar = Snackbar.make(coordinatorLayout,"You Must Fill All Fields",Snackbar.LENGTH_LONG);
            View view = snackbar.getView();
            TextView textView = (TextView)view.findViewById(R.id.snackbar_text) ;
            textView.setTextColor(Color.RED);
            snackbar.show();
            return;
        }

        final Long[] counterValue = new Long[1];
        Thread t=new Thread(){
            @Override
            public void run() {
                counterValue[0] = Backendless.Counters.incrementAndGet( "my counter" );
            }
        };t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            LoginAc.ToastMSG(RegisterAc.this,"Please Try Again Later");
        }

        final int userid = Math.round(counterValue[0]);
        user = new BackendlessUser();
        user.setProperty(BackendLessHelper.pname,name);
        user.setProperty(BackendLessHelper.pmail, email );
        user.setProperty(BackendLessHelper.pUserID,userid);
        user.setPassword( password );

        Backendless.UserService.register( user, new AsyncCallback<BackendlessUser>()
        {
            public void handleResponse(final BackendlessUser registeredUser )
            {
                LoginAc.Login(RegisterAc.this,email,password);//login after register
                if(imgpath!=null){//if register successfully and has img - upload and save photo

                    String[] str = imgpath.split("/");
                    final String ostr = str[str.length - 1];//profile img name
                    //upload profile img
                    Backendless.Files.Android.upload(bm, Bitmap.CompressFormat.PNG, 100, ostr, "IMG/"+registeredUser.getEmail(), false, new AsyncCallback<BackendlessFile>() {
                        @Override
                        public void handleResponse(BackendlessFile backendlessFile) {
                            registeredUser.setProperty(BackendLessHelper.pImg,"IMG/"+registeredUser.getEmail()+"/"+ostr);//add profile img path (server path) to user property
                            Backendless.Persistence.of(BackendlessUser.class).save(registeredUser, new AsyncCallback<BackendlessUser>() {//update user object on server
                                @Override
                                public void handleResponse(BackendlessUser backendlessUser) {
                                    prefs.edit().putString(String.valueOf(userid),imgpath).apply();//add local img path value by userId key in shared preferences
                                }

                                @Override
                                public void handleFault(BackendlessFault backendlessFault) {
                                    //couldn't update user object with img property
                                }
                            });
                        }

                        @Override
                        public void handleFault(BackendlessFault backendlessFault) {
                            //couldn't upload img to server
                        }
                    });
                }

            }
            public void handleFault( BackendlessFault fault )
            {
                //couldn't register to server
                LoginAc.ToastMSG(RegisterAc.this,fault.getMessage());
            }
        } );
    }


}
