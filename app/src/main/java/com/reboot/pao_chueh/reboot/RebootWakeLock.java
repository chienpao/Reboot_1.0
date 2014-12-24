package com.reboot.pao_chueh.reboot;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class RebootWakeLock {
    private static PowerManager.WakeLock cpuWakeLock;
    private static final String TAG = "REBOOT";

    static void acquireCpuWakeLock(Context context) {
        Log.d(TAG, "Acquiring cpu wake lock");
        if (cpuWakeLock != null) {
            return;
        }

        PowerManager pm =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        //cpuWakeLock = pm.newWakeLock(
        //        PowerManager.FULL_WAKE_LOCK |
       //         PowerManager.ACQUIRE_CAUSES_WAKEUP |
        //        PowerManager.ON_AFTER_RELEASE, TAG);
             cpuWakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP , TAG);
        cpuWakeLock.acquire();
    }

    static void releaseCpuLock() {
        Log.v(TAG, "Releasing cpu wake lock");
        if (cpuWakeLock != null) {
            cpuWakeLock.release();
            cpuWakeLock = null;
        }
    }

}
