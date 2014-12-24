package com.reboot.pao_chueh.reboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
	public void onReceive(Context context, Intent intent) {
		Log.d("REBOOT", "onReceive");
		RebootWakeLock.acquireCpuWakeLock(context);
		Utils.disableKeyLock(context);
		
		Intent i = new Intent (context, MainActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}
}