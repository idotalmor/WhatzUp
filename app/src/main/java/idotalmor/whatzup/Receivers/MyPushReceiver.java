package idotalmor.whatzup.Receivers;

import com.backendless.push.BackendlessBroadcastReceiver;
import com.backendless.push.BackendlessPushService;

import idotalmor.whatzup.Services.MyPushService;

public class MyPushReceiver extends BackendlessBroadcastReceiver
{
    @Override
    public Class<? extends BackendlessPushService> getServiceClass()
{
    return MyPushService.class;
}
}