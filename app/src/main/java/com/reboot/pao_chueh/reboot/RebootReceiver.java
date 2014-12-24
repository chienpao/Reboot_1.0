package com.reboot.pao_chueh.reboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebootReceiver extends BroadcastReceiver {
	public static final String TAG = "REBOOT_RECEIVER";

	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Utils.disableKeyLock(context);
			Intent rebootIntent = new Intent (context, MainActivity.class);
			rebootIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(rebootIntent);
		}
		else {
			Log.e(TAG, "Received unexpected intent " + intent.toString());
		}
	}

}
