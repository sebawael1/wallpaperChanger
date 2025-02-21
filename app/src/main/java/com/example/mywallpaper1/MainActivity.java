package com.example.mywallpaper1;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGES_CODE = 1;
    private Button btnPickImage, btnSetBackground;
    private ImageView imageView;
    private ArrayList<Uri> imageUris; // Stores selected images

    // Request permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    pickImageIntent(); // Permission granted, open gallery
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPickImage = findViewById(R.id.btnPickImage);
        btnSetBackground = findViewById(R.id.setbackground);
        imageView = findViewById(R.id.imageView);

        imageUris = new ArrayList<>();

        btnPickImage.setOnClickListener(v -> {
            if (isImagePermissionGranted()) {
                pickImageIntent();
            } else {
                requestImagePermission();
            }
        });

        btnSetBackground.setOnClickListener(v -> showWallpaperOptions());
    }

    // Check if permission is granted
    private boolean isImagePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // Request image permission
    private void requestImagePermission() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        requestPermissionLauncher.launch(permission);
    }

    // Open image picker
    private void pickImageIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_CODE && resultCode == Activity.RESULT_OK) {
            imageUris.clear();
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        imageUris.add(data.getClipData().getItemAt(i).getUri());
                    }
                } else if (data.getData() != null) {
                    imageUris.add(data.getData());
                }
            }
            displayFirstImage();
        }
    }

    // Display the first selected image
    private void displayFirstImage() {
        if (!imageUris.isEmpty()) {
            imageView.setImageURI(imageUris.get(0));
        }
    }

    // Show dialog for wallpaper options
    private void showWallpaperOptions() {
        if (imageUris.isEmpty()) {
            Toast.makeText(this, "Select an image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Home Screen", "Lock Screen", "Both"};
        new AlertDialog.Builder(this)
                .setTitle("Set as Background")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) setWallpaper(WallpaperManager.FLAG_SYSTEM);
                    else if (which == 1) setWallpaper(WallpaperManager.FLAG_LOCK);
                    else setWallpaperBoth();
                })
                .show();
    }

    // Set wallpaper for home or lock screen
    private void setWallpaper(int flag) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUris.get(0));
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(bitmap, null, true, flag);
            } else {
                wallpaperManager.setBitmap(bitmap);
            }

            String message = (flag == WallpaperManager.FLAG_SYSTEM) ? "Home Screen Set!" : "Lock Screen Set!";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "Failed to set wallpaper!", Toast.LENGTH_SHORT).show();
        }
    }

    // Set wallpaper for both home and lock screen
    private void setWallpaperBoth() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUris.get(0));
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM);
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
            } else {
                wallpaperManager.setBitmap(bitmap);
            }

            Toast.makeText(this, "Wallpaper Set for Both Screens!", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "Failed to set wallpaper!", Toast.LENGTH_SHORT).show();
        }
    }
}
