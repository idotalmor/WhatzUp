package idotalmor.whatzup.Adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.backendless.Backendless;
import com.backendless.Subscription;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.messaging.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import de.hdodenhof.circleimageview.CircleImageView;
import idotalmor.whatzup.BackendLessHelper;
import idotalmor.whatzup.Chat_Activity;
import idotalmor.whatzup.R;

/**
 * Created by Ido Talmor on 15/07/2017.
 */

public class ChatlistAdapter extends CursorAdapter {
    final Context context;
    Bitmap bm;

    public ChatlistAdapter(Context context, Cursor c){
        super(context,c);
        this.context=context;
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.member_object, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        RelativeLayout relativelayout=(RelativeLayout)view.findViewById(R.id.relative_layout);
        View status = (View)view.findViewById(R.id.status);
        CircleImageView userImg = (CircleImageView) view.findViewById(R.id.userImg);
        TextView userName = (TextView)view.findViewById(R.id.userName);
        TextView lastmsg = (TextView)view.findViewById(R.id.lastmsg);
        TextView msgtime = (TextView) view.findViewById(R.id.msgtime);

        int s = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
        String uimg=cursor.getString(cursor.getColumnIndexOrThrow(BackendLessHelper.pImg));
        String uname=cursor.getString(cursor.getColumnIndexOrThrow(BackendLessHelper.pname));
        String umsg=cursor.getString(cursor.getColumnIndexOrThrow("lastmsg"));
        String utime=cursor.getString(cursor.getColumnIndexOrThrow("Time"));
        final int UserID = cursor.getInt(cursor.getColumnIndexOrThrow(BackendLessHelper.pUserID));

        if(s==1){status.setBackgroundColor(Color.argb(100,66, 244, 203));}
        String channelName = "CHN"+String.valueOf(UserID)+"S";
        Backendless.Messaging.subscribe(channelName, new AsyncCallback<List<Message>>() {
            @Override
            public void handleResponse(List<Message> response) {

                for (Message message:response){

                    String publisherId = message.getPublisherId();
                    Object data = message.getData();

                }
            }

            @Override
            public void handleFault(BackendlessFault fault) {

            }
        }, new AsyncCallback<Subscription>() {
            @Override
            public void handleResponse(Subscription response) {
                //managed to subscribe
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                //didn't subscribe to the channel
            }
        });
        if(uimg!=null){bm= BitmapFactory.decodeFile(uimg);
            if(bm!=null)userImg.setImageBitmap(bm);}
        userName.setText(uname);
        if(umsg!=null&&!umsg.isEmpty())
        {lastmsg.setText(umsg);}

        try{
        Date date = new Date(Long.valueOf(utime)); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy"); // the format of your date
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+2")); // give a timezone reference for formating (see comment at the bottom
        String formattedDate = sdf.format(date);
        msgtime.setText(formattedDate);}catch (NumberFormatException e){}

        relativelayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(context,Chat_Activity.class);
                i.putExtra("UserID",UserID);
                context.startActivity(i);
            }
        });

    }
}
