package idotalmor.whatzup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.UserService;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.local.UserIdStorageFactory;
import com.backendless.persistence.local.UserTokenStorageFactory;


public class LoginAc extends AppCompatActivity {

    EditText LoginUsername,LoginUserpassword;
    private String Username,Password;
    public static String AppId="AppID",AndroidKey="AndroidBackendlessKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        Backendless.initApp(this,AppId,AndroidKey);//handshake with backendless

        LoginUsername=(EditText)findViewById(R.id.LoginUserName);
        LoginUserpassword=(EditText)findViewById(R.id.LoginPassword);
        LoginUserpassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {//set up done key to login
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    LoginBtnFunc(textView);
                    return true;
                }
                else {
                    return false;
                }
            }
        });
    }



    @Override
    protected void onStart() { //Check if uer is login already
        super.onStart();
        //get storage login user token
        String userToken = UserTokenStorageFactory.instance().getStorage().get();

        if( userToken != null && !userToken.equals( "" ) )//check if user is logged in already
        {   String currentUserObjectId =UserIdStorageFactory.instance().getStorage().get();//get login user id
            Backendless.Data.of( BackendlessUser.class ).findById(currentUserObjectId, new AsyncCallback<BackendlessUser>() {//get user object from server
                @Override
                public void handleResponse(BackendlessUser backendlessUser) {
                    Backendless.UserService.setCurrentUser(backendlessUser);//set login user object as current user
                    startActivity(new Intent("ChatListActivity"));//intent to chatlist activity
                }
                @Override
                public void handleFault(BackendlessFault backendlessFault) {
                    ToastMSG(LoginAc.this,"Failed To Auto Login");
                }
            });
        }}

    public void LoginBtnFunc(View v){ //login btn
        Username=LoginUsername.getText().toString();
        Password=LoginUserpassword.getText().toString();
        Login(this,Username,Password);//static login method
    }

    public void RegisterBtn(View v){// register btn intent
        startActivity(new Intent("RegisterActivity"));
    }

    public static void Login(final Context c, final String username, final String pass){ //static login method
        Backendless.UserService.login( username, pass, new AsyncCallback<BackendlessUser>()
        {
            public void handleResponse( BackendlessUser user ) //if login successfully
            {
                String channel = "CHN"+user.getProperty(BackendLessHelper.pUserID);//Channel name
                Backendless.Messaging.registerDevice("GCMSenderID", channel, new AsyncCallback<Void>() {//Register Device
                    @Override
                    public void handleResponse(Void aVoid) {
                        //When Device Register Successfully
                        Intent i = new Intent("ChatListActivity");
                        c.startActivity(i);
                    }
                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        ToastMSG(c,"Failed to Register Device "+backendlessFault.getMessage());
                        Backendless.UserService.logout();//logout user if device has not register successfully
                    }
                });
            }
            public void handleFault( BackendlessFault fault ) {ToastMSG(c,fault.getMessage());} // if login unsuccessfully
        },true);//true - stay login until logout
    }

    public static void ToastMSG(Context c, String msg){
        Toast.makeText(c,msg, Toast.LENGTH_LONG).show();} //Static Toast Method


}
