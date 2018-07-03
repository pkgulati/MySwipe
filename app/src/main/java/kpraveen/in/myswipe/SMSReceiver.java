package kpraveen.in.myswipe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {

    private static String TAG = "SMSReceiver";

    private MessageManager messageManager ;

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "broadcaster ");
        if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs = null;
            if (bundle != null){
                try{
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];

                    SwipeMessage message = new SwipeMessage();
                    String body = "";
                    for(int i=0; i<msgs.length; i++){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            String format = bundle.getString("format");
                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                        }
                        else {
                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        }
                        SmsMessage sms = msgs[i];
                        body = body + sms.getDisplayMessageBody();
                        message.smsSentTime = sms.getTimestampMillis();
                        message.smsTime = System.currentTimeMillis();
                        message.serviceCenter = sms.getServiceCenterAddress();
                        message.address = sms.getDisplayOriginatingAddress();
                        message.source = "Broadcast";
                    }
                    message.text = body;
                    Log.d(TAG, "time " + message.smsTime + " final body " + body);
                    MessageManager.postSwipeMessage(context, message);
                    MessageManager.processMessage(context, message);
                } catch(Exception e) {
                    //Log.e(TeamApplication.TAG, e);
                }
            }
        }
        Log.d(TAG, "SMS Received");
    }

}
