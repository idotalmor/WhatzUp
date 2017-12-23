package idotalmor.whatzup.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.backendless.Backendless;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import idotalmor.whatzup.BackendLessHelper;
import idotalmor.whatzup.DBHelper;
import idotalmor.whatzup.R;


/**
 * Created by Ido Talmor on 15/07/2017.
 */

public class MsglistAdapter extends CursorAdapter {
    final Context context;
    int currentid;
    SQLiteDatabase db;

    public MsglistAdapter(Context context, Cursor c){
        super(context,c,FLAG_REGISTER_CONTENT_OBSERVER);
        this.context=context;
        db = DBHelper.GetDBHelper(context).getWritableDatabase();
        currentid= (int)Backendless.UserService.CurrentUser().getProperty(BackendLessHelper.pUserID);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.msg_layout, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        RelativeLayout relativelayout=(RelativeLayout)view.findViewById(R.id.relative_msg);
        RelativeLayout relativemsglayout=(RelativeLayout)view.findViewById(R.id.relativemsglayout);
        TextView username = (TextView)view.findViewById(R.id.userttlmsgtextview);
        TextView lastmsg = (TextView)view.findViewById(R.id.msgtextview);
        TextView msgtime = (TextView) view.findViewById(R.id.msgtimewindow);
        ImageView img = (ImageView)view.findViewById(R.id.msgimg);

        final int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
        String name=cursor.getString(cursor.getColumnIndexOrThrow("name"));
        String msg=cursor.getString(cursor.getColumnIndexOrThrow("message"));
        String image=cursor.getString(cursor.getColumnIndexOrThrow("image"));
        String utime=cursor.getString(cursor.getColumnIndexOrThrow("Time"));
        final int UserID = cursor.getInt(cursor.getColumnIndexOrThrow(BackendLessHelper.pUserID));

        if(UserID!=currentid){relativemsglayout.setBackgroundResource(R.drawable.msgbackuser);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)relativemsglayout.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            relativemsglayout.setLayoutParams(params);}
        else{relativemsglayout.setBackgroundResource(R.drawable.msg);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)relativemsglayout.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            relativemsglayout.setLayoutParams(params);
        }

        username.setText(name);
        lastmsg.setText(msg);

        Date date = new Date(Long.valueOf(utime)); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy"); // the format of your date
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+3")); // give a timezone reference for formating (see comment at the bottom
        String formattedDate = sdf.format(date);
        msgtime.setText(formattedDate);


        if(!"".equals(image)){//if has image add her to relative view
            img.setImageBitmap(BitmapFactory.decodeFile(image));
            img.setVisibility(View.VISIBLE);
        }else{
            img.setVisibility(View.GONE);
        }

        relativelayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder alrt = new AlertDialog.Builder(context);
                alrt.setTitle("Do You Want To Delete This Message?");
                alrt.setNegativeButton("No",null);
                alrt.setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String query = "UPDATE "+DBHelper.ChatWindow+"_"+UserID+" SET message = null WHERE _id = '"+id+"'";
                        db.execSQL(query);
                        query = "UPDATE "+DBHelper.ChatWindow+"_"+UserID+" SET image = null WHERE _id = '"+id+"'";
                        db.execSQL(query);
                       // ((Chat_Activity)context).getmsg();
                    }
                });
                alrt.show();

                return false;
            }
        });

    }
}
