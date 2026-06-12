package com.benmaty.smsocto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receiver obligatoire pour que Android accepte l'appli comme
 * gestionnaire SMS par défaut. Reçoit les SMS entrants.
 */
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // SMS reçu – on ne fait rien de spécial dans cette appli
        // (elle est focalisée sur l'envoi en masse)
    }
}
