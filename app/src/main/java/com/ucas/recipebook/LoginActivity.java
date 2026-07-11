package com.ucas.recipebook;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private EditText et_email_login, et_password_login;
    private Button btn_login;
    private CheckBox cb_remember_me;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "loginPrefs";
    private static final String KEY_REMEMBER = "rememberMe";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        et_email_login = findViewById(R.id.et_email_login);
        et_password_login = findViewById(R.id.et_password_login);
        cb_remember_me = findViewById(R.id.cb_remember_me);
        btn_login = findViewById(R.id.btn_login);
        TextView tv_register = findViewById(R.id.tv_register);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        tv_register.setOnClickListener( e -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btn_login.setOnClickListener( e -> {
            handleLogin();
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (rememberMe && currentUser != null) {
            getUserDataFromFirestore(currentUser.getUid());
        } else if (!rememberMe) {
            mAuth.signOut();
        }
    }

    private void handleLogin() {
        String email = et_email_login.getText().toString().trim();
        String password = et_password_login.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();

                        SharedPreferences.Editor editor = sharedPreferences.edit(); ////
                        if (cb_remember_me.isChecked())
                            editor.putBoolean(KEY_REMEMBER, true); //// <----- this code is for saveing on the phone if the user wants to auto login
                        else
                            editor.clear();
                        editor.apply();  ////

                        getUserDataFromFirestore(userId);
                    } else {
                        Toast.makeText(LoginActivity.this, "Login Failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void getUserDataFromFirestore(String userId) {
        // Go to the "users" collection and look for the document named after the userId
        mFirestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        if (document.exists()) {
                            // 3. Extract the text data fields strings
                            String name = document.getString("name");
                            String country = document.getString("country");
                            String email = document.getString("email");

                            Toast.makeText(LoginActivity.this, "Welcome back, " + name, Toast.LENGTH_SHORT).show();

                            // 4. Send this data over to your Home/Profile Activity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("USER_NAME", name);
                            intent.putExtra("USER_COUNTRY", country);
                            intent.putExtra("USER_EMAIL", email);

                            startActivity(intent);
                            finish(); // Close login activity
                        } else {
                            Toast.makeText(LoginActivity.this, "No profile data found in database.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Error fetching data: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}