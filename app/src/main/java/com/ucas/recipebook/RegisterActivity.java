package com.ucas.recipebook;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private ImageView iv_profile_image_register;
    private EditText et_name_register, et_email_register, et_password_register;
    private Spinner sp_country_register;
    private Button btn_register;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    @Override
    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ImageView iv_new_image = findViewById(R.id.iv_new_image);
        TextView tv_back_to_login = findViewById(R.id.tv_back_to_login);
        iv_profile_image_register = findViewById(R.id.iv_profile_image_register);
        et_name_register = findViewById(R.id.et_name_register);
        et_email_register = findViewById(R.id.et_email_register);
        et_password_register = findViewById(R.id.et_password_register);
        sp_country_register = findViewById(R.id.sp_country_register);
        btn_register = findViewById(R.id.btn_register);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        //FirebaseAuth mAuth = FirebaseAuth.getInstance();

        pickMedia = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    // Callback when the user selects an image or cancels
                    if (uri != null) {
                        // Set the selected image URI to the ImageView
                        iv_profile_image_register.setImageURI(uri);
                    } else {
                        // User closed the picker without selecting anything
                        System.out.println("No media selected");
                    }
                }
        );

        iv_new_image.setOnClickListener( e -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        tv_back_to_login.setOnClickListener( e -> {
            finish();
        });

        btn_register.setOnClickListener( e -> {
            handleRegistration();
        });
    }
    private void handleRegistration() {
        String name = et_name_register.getText().toString().trim();
        String email = et_email_register.getText().toString().trim();
        String password = et_password_register.getText().toString().trim();
        String country = sp_country_register.getSelectedItem().toString();

        // 1. Basic Validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || country.equals("Select Country")) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Create User account in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration success! Get the unique user ID
                        String userId = mAuth.getCurrentUser().getUid();

                        // 3. Process the image and save to Firestore
                        saveUserDataToFirestore(userId, name, email, country);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserDataToFirestore(String userId, String name, String email, String country) {
        String base64Image = convertImageViewToBase64(iv_profile_image_register);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", userId);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("country", country);
        userMap.put("profileImage", base64Image);

        // Try saving to firestore
        mFirestore.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registration Complete!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    //intent.putExtra("USER_NAME", name);
                    //intent.putExtra("USER_EMAIL", email);
                    //intent.putExtra("USER_COUNTRY", country);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // !!! THIS WILL TELL YOU EXACTLY WHAT IS WRONG !!!
                    android.util.Log.e("FIRESTORE_ERROR", "Error writing document", e);
                    Toast.makeText(RegisterActivity.this, "Firestore Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /*
    private void saveUserDataToFirestore(String userId, String name, String email, String country) {
        // Convert your ImageView image to a Base64 string safely
        String base64Image = convertImageViewToBase64(iv_profile_image_register);

        // Create a Map object representing user profile structure
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", userId);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("country", country);
        userMap.put("profileImage", base64Image);

        // Save into a collection named "users" using the userId as the document name
        mFirestore.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registration Complete!", Toast.LENGTH_SHORT).show();
                    // Finish activity or intent to HomeActivity here
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Failed to save profile: " +
                            e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }*/

    // Helper method to convert the current image to text format
    private String convertImageViewToBase64(ImageView imageView) {
        if (imageView.getDrawable() == null) return "";

        try {
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Compress heavily (quality 50) to ensure it stays well under the 1MB firestore limit
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
            byte[] byteArray = outputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}