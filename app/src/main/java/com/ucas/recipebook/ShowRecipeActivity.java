package com.ucas.recipebook;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ShowRecipeActivity extends AppCompatActivity {

    private ImageView imShowRecipe, tvBackShow, ivShareShow, ivFavoriteShow;
    private TextView tvRecipeNameShow, tvDescriptionShow, tvIngredientsShow, tvStepsShow;
    private Button btn_edit_rescipe, btn_delete_rescipe;
    private LinearLayout llWatchVideo;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String recipeId;
    private String currentUserId;

    private Recipe currentRecipe;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_recipe);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        recipeId = getIntent().getStringExtra("RECIPE_ID");

        imShowRecipe = findViewById(R.id.im_show_recipe);
        tvBackShow = findViewById(R.id.tv_back_show);
        ivShareShow = findViewById(R.id.iv_share_show);
        ivFavoriteShow = findViewById(R.id.iv_favorite_show);
        tvRecipeNameShow = findViewById(R.id.tv_recipe_name_show);
        tvDescriptionShow = findViewById(R.id.tv_description_show);
        tvIngredientsShow = findViewById(R.id.tv_ingredients_show);
        tvStepsShow = findViewById(R.id.tv_steps_show);
        btn_edit_rescipe = findViewById(R.id.btn_edit_rescipe);
        btn_delete_rescipe = findViewById(R.id.btn_delete_rescipe);
        llWatchVideo = findViewById(R.id.ll_whatch_video);

        tvIngredientsShow.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        tvStepsShow.setMovementMethod(new android.text.method.ScrollingMovementMethod());

        tvBackShow.setOnClickListener(v -> finish());

        ivShareShow.setOnClickListener(v -> shareRecipe());

        ivFavoriteShow.setOnClickListener(v -> toggleFavorite());

        llWatchVideo.setOnClickListener(v -> openVideo());

        btn_edit_rescipe.setOnClickListener(v -> openEditRecipe());
        btn_delete_rescipe.setOnClickListener(v -> confirmDeleteRecipe());

        if (recipeId == null || recipeId.isEmpty()) {
            Toast.makeText(this, "Recipe id missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadRecipe();
    }

    private void loadRecipe() {
        db.collection("recipes")
                .document(recipeId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentRecipe = documentSnapshot.toObject(Recipe.class);

                    if (currentRecipe == null) {
                        Toast.makeText(this, "Recipe data error", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentRecipe.setId(documentSnapshot.getId());
                    showRecipeData();
                });
    }

    private void showRecipeData() {
        setupOwnerButtons();

        tvRecipeNameShow.setText(nullToEmpty(currentRecipe.getName()));
        tvDescriptionShow.setText(nullToEmpty(currentRecipe.getDescription()));
        tvIngredientsShow.setText(formatCommaText(currentRecipe.getIngredients()));
        tvStepsShow.setText(formatCommaText(currentRecipe.getSteps()));

        showImage(currentRecipe.getBase64Image());
        checkFavoriteState();

        String videoUrl = currentRecipe.getVideoUrl();

        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            llWatchVideo.setVisibility(View.GONE);
        } else {
            llWatchVideo.setVisibility(View.VISIBLE);
        }
    }
    private String formatCommaText(String value) {
        if (value == null) return "";
        return value.replace(",", "\n").trim();
    }
    private void setupOwnerButtons() {
        if (currentRecipe == null || currentUserId == null) {
            btn_edit_rescipe.setVisibility(View.GONE);
            btn_delete_rescipe.setVisibility(View.GONE);
            return;
        }

        boolean isOwner = currentUserId.equals(currentRecipe.getAuthorId());

        if (isOwner) {
            btn_edit_rescipe.setVisibility(View.VISIBLE);
            btn_delete_rescipe.setVisibility(View.VISIBLE);
        } else {
            btn_edit_rescipe.setVisibility(View.GONE);
            btn_delete_rescipe.setVisibility(View.GONE);
        }
    }

    private void openEditRecipe() {
        if (currentRecipe == null || recipeId == null) {
            Toast.makeText(this, "Recipe not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!currentUserId.equals(currentRecipe.getAuthorId())) {
            Toast.makeText(this, "Only creator can edit this recipe", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, AddRecipeActivity.class);
        intent.putExtra("EDIT_MODE", true);
        intent.putExtra("RECIPE_ID", recipeId);
        startActivity(intent);
    }

    private void confirmDeleteRecipe() {
        if (currentRecipe == null || recipeId == null) {
            Toast.makeText(this, "Recipe not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!currentUserId.equals(currentRecipe.getAuthorId())) {
            Toast.makeText(this, "Only creator can delete this recipe", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to delete this recipe?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecipe())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecipe() {
        db.collection("recipes")
                .document(recipeId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Recipe deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
    private void showImage(String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            imShowRecipe.setImageResource(R.drawable.ic_recipe_book_logo_color);
            return;
        }

        try {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imShowRecipe.setImageBitmap(bitmap);
        } catch (Exception e) {
            imShowRecipe.setImageResource(R.drawable.ic_recipe_book_logo_color);
        }
    }

    private void checkFavoriteState() {
        if (currentUserId == null || currentRecipe == null) {
            isFavorite = false;
            ivFavoriteShow.setImageResource(R.drawable.baseline_star_border_24);
            return;
        }

        List<String> likedBy = currentRecipe.getLikedBy();

        isFavorite = likedBy != null && likedBy.contains(currentUserId);

        if (isFavorite) {
            ivFavoriteShow.setImageResource(R.drawable.baseline_star_24);
        } else {
            ivFavoriteShow.setImageResource(R.drawable.baseline_star_border_24);
        }
    }

    private void toggleFavorite() {
        if (currentUserId == null) {
            Toast.makeText(this, "Login first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentRecipe == null || recipeId == null) {
            Toast.makeText(this, "Recipe not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        ivFavoriteShow.setEnabled(false);

        if (isFavorite) {
            db.collection("recipes")
                    .document(recipeId)
                    .update("likedBy", FieldValue.arrayRemove(currentUserId))
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show())
                    .addOnCompleteListener(task ->
                            ivFavoriteShow.setEnabled(true));
        } else {
            db.collection("recipes")
                    .document(recipeId)
                    .update("likedBy", FieldValue.arrayUnion(currentUserId))
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show())
                    .addOnCompleteListener(task ->
                            ivFavoriteShow.setEnabled(true));
        }
    }

    private void openVideo() {
        if (currentRecipe == null) return;

        String videoUrl = currentRecipe.getVideoUrl();

        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            Toast.makeText(this, "No video URL", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open video", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareRecipe() {
        if (currentRecipe == null) {
            Toast.makeText(this, "Recipe not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String videoUrl = currentRecipe.getVideoUrl();

        String shareText =
                nullToEmpty(currentRecipe.getName()) + "\n\n" +
                        nullToEmpty(currentRecipe.getDescription()) + "\n\n" +
                        "Ingredients:\n" + nullToEmpty(currentRecipe.getIngredients()) + "\n\n" +
                        "Steps:\n" + nullToEmpty(currentRecipe.getSteps());

        if (videoUrl != null && !videoUrl.trim().isEmpty()) {
            shareText += "\n\nWatch video:\n" + videoUrl;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, currentRecipe.getName());
        intent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(intent, "Share Recipe"));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}