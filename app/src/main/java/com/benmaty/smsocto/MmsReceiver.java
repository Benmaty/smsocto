package com.benmaty.smsocto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receiver MMS obligatoire pour être appli SMS par défaut.
 */
public class MmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // MMS reçu – non géré
    }
}
