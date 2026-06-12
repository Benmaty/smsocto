package com.benmaty.smsocto;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.benmaty.smsocto.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_DEFAULT_SMS = 200;
    private static final int SMS_PERMISSION_REQUEST = 101;
    private static final int DELAY_MS = 2000;

    private ActivityMainBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isSending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Demande à devenir appli SMS par défaut au démarrage
        requestDefaultSmsApp();

        binding.btnSend.setOnClickListener(v -> {
            if (isSending) stopSending();
            else checkAndSend();
        });

        binding.btnClear.setOnClickListener(v -> {
            binding.etNumbers.setText("");
            binding.etMessage.setText("");
            binding.tvLog.setText("");
            binding.progressBar.setVisibility(View.GONE);
            binding.tvStatus.setText("");
        });
    }

    private void requestDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ : via RoleManager
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
                    && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                startActivityForResult(intent, REQUEST_DEFAULT_SMS);
            }
        } else {
            // Android < 10 : méthode classique
            String defaultSms = Telephony.Sms.getDefaultSmsPackage(this);
            if (!getPackageName().equals(defaultSms)) {
                Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                startActivityForResult(intent, REQUEST_DEFAULT_SMS);
            }
        }
    }

    private void checkAndSend() {
        // Vérifie si on est appli SMS par défaut
        boolean isDefault;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            isDefault = roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_SMS);
        } else {
            isDefault = getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
        }

        if (!isDefault) {
            // Redemande si l'utilisateur a refusé
            Toast.makeText(this,
                "Définissez smsocto comme appli SMS par défaut pour envoyer des SMS",
                Toast.LENGTH_LONG).show();
            requestDefaultSmsApp();
            return;
        }

        // On est appli par défaut, on a les permissions → on envoie
        startSending();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DEFAULT_SMS) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "✅ smsocto est maintenant l'appli SMS par défaut !", Toast.LENGTH_SHORT).show();
                binding.tvStatus.setText("✅ Prêt – vous pouvez envoyer des SMS");
            } else {
                Toast.makeText(this, "⚠️ Sans être appli SMS par défaut, l'envoi est bloqué par Android", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startSending() {
        String raw = binding.etNumbers.getText() != null
                ? binding.etNumbers.getText().toString().trim() : "";
        String msg = binding.etMessage.getText() != null
                ? binding.etMessage.getText().toString().trim() : "";

        if (raw.isEmpty()) { binding.etNumbers.setError("Entrez des numéros"); return; }
        if (msg.isEmpty())  { binding.etMessage.setError("Message vide"); return; }

        String[] split = raw.split(";");
        List<String> numbers = new ArrayList<>();
        for (String n : split) {
            String c = n.trim();
            if (!c.isEmpty()) numbers.add(c);
        }
        if (numbers.isEmpty()) {
            Toast.makeText(this, "Aucun numéro valide", Toast.LENGTH_SHORT).show();
            return;
        }

        isSending = true;
        binding.btnSend.setText("⏹ Arrêter");
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.progressBar.setMax(numbers.size());
        binding.progressBar.setProgress(0);
        binding.tvLog.setText("");
        binding.tvStatus.setText("Envoi… 0/" + numbers.size());
        sendNext(numbers, msg, 0);
    }

    private void sendNext(List<String> numbers, String msg, int i) {
        if (!isSending || i >= numbers.size()) {
            finishSending(i, numbers.size());
            return;
        }
        String number = numbers.get(i);
        boolean success = sendSms(number, msg);
        binding.tvLog.append((success ? "✅ " : "❌ ") + number + "\n");
        binding.progressBar.setProgress(i + 1);
        binding.tvStatus.setText("Envoi… " + (i + 1) + "/" + numbers.size());
        binding.scrollLog.post(() -> binding.scrollLog.fullScroll(View.FOCUS_DOWN));

        if (i + 1 < numbers.size()) {
            handler.postDelayed(() -> sendNext(numbers, msg, i + 1), DELAY_MS);
        } else {
            finishSending(numbers.size(), numbers.size());
        }
    }

    private boolean sendSms(String number, String msg) {
        try {
            SmsManager sms;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sms = getApplicationContext().getSystemService(SmsManager.class);
            } else {
                sms = SmsManager.getDefault();
            }
            if (sms == null) return false;
            ArrayList<String> parts = sms.divideMessage(msg);
            sms.sendMultipartTextMessage(number, null, parts, null, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void finishSending(int sent, int total) {
        isSending = false;
        binding.btnSend.setText("📤 Envoyer");
        binding.progressBar.setVisibility(View.GONE);
        binding.tvStatus.setText("Terminé : " + sent + "/" + total + " SMS envoyés");
    }

    private void stopSending() {
        isSending = false;
        handler.removeCallbacksAndMessages(null);
        binding.btnSend.setText("📤 Envoyer");
        binding.progressBar.setVisibility(View.GONE);
        binding.tvStatus.setText("Envoi interrompu");
    }
}
