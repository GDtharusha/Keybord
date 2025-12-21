package com.keybord.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.getcapacitor.BridgeActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends BridgeActivity {
    
    private static final String TAG = "MainActivity";
    private static final int PICK_IMAGE_REQUEST = 1001;
    private KeyboardSettings settings;
    private KeyboardAPI keyboardAPI;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        settings = new KeyboardSettings(this);
        keyboardAPI = new KeyboardAPI(this);
        
        // Add the new modular API bridge
        getBridge().getWebView().addJavascriptInterface(keyboardAPI, "Android");
        
        // Keep old bridge for backward compatibility (optional - can remove later)
        getBridge().getWebView().addJavascriptInterface(keyboardAPI, "NativeSettings");
        
        Log.d(TAG, "KeyboardAPI bridge added");
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    String savedPath = saveImageToInternal(imageUri);
                    if (savedPath != null) {
                        settings.setBackgroundImage(savedPath);
                        notifyKeyboard();
                        
                        runOnUiThread(() -> {
                            getBridge().getWebView().evaluateJavascript(
                                "if(typeof showToast === 'function') showToast('✓ Background image set!');", 
                                null
                            );
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing image", e);
            }
        }
    }
    
    private String saveImageToInternal(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            
            File outputDir = new File(getFilesDir(), "keyboard_bg");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            File outputFile = new File(outputDir, "background.jpg");
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            inputStream.close();
            outputStream.close();
            
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }
    
    private void notifyKeyboard() {
        try {
            Intent intent = new Intent(KeyboardSettings.ACTION_SETTINGS_CHANGED);
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error notifying keyboard", e);
        }
    }
    
    /**
     * Called from KeyboardAPI for image selection
     */
    public void selectBackgroundImage() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "Error opening image picker", e);
        }
    }
}