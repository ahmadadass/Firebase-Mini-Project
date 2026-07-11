package com.ucas.recipebook;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddRecipeActivity extends AppCompatActivity {

    private EditText etTitle, etDescription, etIngredients, etSteps, etVideoUrl;
    private Spinner spCategory;
    private Button btnAddRecipe, btnBack;
    private ImageView ivRecipeImage, ivChooseRecipeImage;
    private TextView tvScreenTitle;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isEditMode = false;
    private String editRecipeId;
    private String existingBase64Image = "";
    private Uri selectedImageUri = null;

    private ActivityResultLauncher<PickVisualMediaRequest> pickRecipeImage;

    @Override
    @SuppressLint("WrongViewCast")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        etTitle = findViewById(R.id.et_recipe_title);
        etDescription = findViewById(R.id.et_recipe_description);
        etIngredients = findViewById(R.id.et_recipe_ingredients);
        etSteps = findViewById(R.id.et_recipe_steps);
        spCategory = findViewById(R.id.sp_recipe_category);
        etVideoUrl = findViewById(R.id.et_recipe_video);

        btnAddRecipe = findViewById(R.id.btn_add_recipe);
        btnBack = findViewById(R.id.btn_back_add_rescipe);
        ivChooseRecipeImage = findViewById(R.id.iv_choose_recipe_image);
        ivRecipeImage = findViewById(R.id.iv_recipe_image_add);
        tvScreenTitle = findViewById(R.id.textView9);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        pickRecipeImage = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        ivRecipeImage.setImageURI(uri);
                    }
                }
        );

        ivChooseRecipeImage.setOnClickListener(v -> {
            pickRecipeImage.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnBack.setOnClickListener(v -> finish());

        isEditMode = getIntent().getBooleanExtra("EDIT_MODE", false);
        editRecipeId = getIntent().getStringExtra("RECIPE_ID");

        if (isEditMode && editRecipeId != null) {
            tvScreenTitle.setText("Edit Recipe");
            btnAddRecipe.setText("Update Recipe");
            loadRecipeForEdit();
        } else {
            tvScreenTitle.setText("Add Recipe");
            btnAddRecipe.setText("Add Recipe");
        }

        btnAddRecipe.setOnClickListener(v -> {
            if (isEditMode) {
                updateRecipe();
            } else {
                saveRecipe();
            }
        });
    }

    private void loadRecipeForEdit() {
        db.collection("recipes")
                .document(editRecipeId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    etTitle.setText(document.getString("name"));
                    etDescription.setText(document.getString("description"));
                    etIngredients.setText(document.getString("ingredients"));
                    etSteps.setText(document.getString("steps"));
                    setSpinnerValue(spCategory, document.getString("category"));
                    etVideoUrl.setText(document.getString("videoUrl"));

                    existingBase64Image = document.getString("base64Image");

                    if (existingBase64Image != null && !existingBase64Image.isEmpty()) {
                        showBase64Image(existingBase64Image);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;

        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void saveRecipe() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String ingredients = etIngredients.getText().toString().trim();
        String steps = etSteps.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        String videoUrl = etVideoUrl.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty()
                || steps.isEmpty() || category.equals("Select Category")) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You must login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String recipeId = db.collection("recipes").document().getId();
        String userId = currentUser.getUid();

        String base64Image = "";

        if (selectedImageUri != null) {
            base64Image = convertUriToBase64(selectedImageUri);
        }

        Map<String, Object> recipeMap = new HashMap<>();
        recipeMap.put("id", recipeId);
        recipeMap.put("name", title);
        recipeMap.put("description", description);
        recipeMap.put("ingredients", ingredients);
        recipeMap.put("steps", steps);
        recipeMap.put("category", category);
        recipeMap.put("videoUrl", videoUrl);
        recipeMap.put("base64Image", base64Image);

        recipeMap.put("authorId", userId);
        recipeMap.put("authorName", currentUser.getEmail());
        recipeMap.put("publicRecipe", true);
        recipeMap.put("likedBy", new ArrayList<String>());
        recipeMap.put("createdAt", FieldValue.serverTimestamp());

        db.collection("recipes")
                .document(recipeId)
                .set(recipeMap)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Recipe added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void updateRecipe() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String ingredients = etIngredients.getText().toString().trim();
        String steps = etSteps.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        String videoUrl = etVideoUrl.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty()
                || steps.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalBase64Image = existingBase64Image == null ? "" : existingBase64Image;

        if (selectedImageUri != null) {
            finalBase64Image = convertUriToBase64(selectedImageUri);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", title);
        updates.put("description", description);
        updates.put("ingredients", ingredients);
        updates.put("steps", steps);
        updates.put("category", category);
        updates.put("videoUrl", videoUrl);
        updates.put("base64Image", finalBase64Image);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("recipes")
                .document(editRecipeId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Recipe updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private String convertUriToBase64(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap == null) return "";

            Bitmap scaledBitmap = scaleBitmap(bitmap, 800);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 45, outputStream);

            byte[] byteArray = outputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Image error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return "";
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min(
                (float) maxSize / width,
                (float) maxSize / height
        );

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void showBase64Image(String base64Image) {
        try {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ivRecipeImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            ivRecipeImage.setImageResource(R.drawable.ic_recipe_book_logo_color);
        }
    }
}