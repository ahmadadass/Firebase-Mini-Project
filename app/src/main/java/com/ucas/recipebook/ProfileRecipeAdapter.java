package com.ucas.recipebook;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ProfileRecipeAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Recipe> recipes;
    private String currentUserId;
    private FirebaseFirestore db;

    public ProfileRecipeAdapter(Context context, ArrayList<Recipe> recipes, String currentUserId) {
        this.context = context;
        this.recipes = recipes;
        this.currentUserId = currentUserId;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public int getCount() {
        return recipes.size();
    }

    @Override
    public Object getItem(int position) {
        return recipes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.profile_recipe_item, parent, false);
        }

        Recipe recipe = recipes.get(position);

        ImageView image = view.findViewById(R.id.iv_profile_recipe_item);
        TextView title = view.findViewById(R.id.tv_profile_recipe_title);
        TextView details = view.findViewById(R.id.tv_profile_recipe_details);
        ImageButton edit = view.findViewById(R.id.btn_profile_recipe_edit);
        ImageButton delete = view.findViewById(R.id.btn_profile_recipe_delete);

        title.setText(recipe.getName() == null ? "Recipe" : recipe.getName());
        details.setText(recipe.getDescription() == null ? "" : recipe.getDescription());

        showImage(image, recipe.getBase64Image());

        view.setOnClickListener(v -> openRecipeDetails(recipe));

        edit.setOnClickListener(v -> openEditRecipe(recipe));

        delete.setOnClickListener(v -> confirmDeleteRecipe(recipe));

        return view;
    }

    private void openRecipeDetails(Recipe recipe) {
        if (recipe.getId() == null || recipe.getId().isEmpty()) {
            Toast.makeText(context, "Recipe id missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, ShowRecipeActivity.class);
        intent.putExtra("RECIPE_ID", recipe.getId());
        context.startActivity(intent);
    }

    private void openEditRecipe(Recipe recipe) {
        if (!isOwner(recipe)) {
            Toast.makeText(context, "Only creator can edit this recipe", Toast.LENGTH_SHORT).show();
            return;
        }

        if (recipe.getId() == null || recipe.getId().isEmpty()) {
            Toast.makeText(context, "Recipe id missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, AddRecipeActivity.class);
        intent.putExtra("EDIT_MODE", true);
        intent.putExtra("RECIPE_ID", recipe.getId());
        context.startActivity(intent);
    }

    private void confirmDeleteRecipe(Recipe recipe) {
        if (!isOwner(recipe)) {
            Toast.makeText(context, "Only creator can delete this recipe", Toast.LENGTH_SHORT).show();
            return;
        }

        if (recipe.getId() == null || recipe.getId().isEmpty()) {
            Toast.makeText(context, "Recipe id missing", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to delete this recipe?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecipe(recipe))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecipe(Recipe recipe) {
        db.collection("recipes")
                .document(recipe.getId())
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(context, "Recipe deleted successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private boolean isOwner(Recipe recipe) {
        return currentUserId != null
                && recipe.getAuthorId() != null
                && currentUserId.equals(recipe.getAuthorId());
    }

    private void showImage(ImageView imageView, String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_recipe_book_logo_color);
            return;
        }

        try {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.ic_recipe_book_logo_color);
        }
    }
}