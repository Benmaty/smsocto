package com.benmaty.smsocto;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service obligatoire pour être appli SMS par défaut.
 * Gère les réponses SMS depuis l'écran de verrouillage / appels.
 */
public class HeadlessSmsSendService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
