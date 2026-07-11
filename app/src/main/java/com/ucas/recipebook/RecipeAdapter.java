package com.ucas.recipebook;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Recipe> recipes;
    private String currentUserId;
    private FirebaseFirestore db;

    public RecipeAdapter(Context context, ArrayList<Recipe> recipes, String currentUserId) {
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

    private boolean userLiked(Recipe recipe) {
        List<String> likedBy = recipe.getLikedBy();
        return currentUserId != null && likedBy != null && likedBy.contains(currentUserId);
    }

    private int likesCount(Recipe recipe) {
        return recipe.getLikedBy() == null ? 0 : recipe.getLikedBy().size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.recipe_item, parent, false);
        }

        Recipe recipe = recipes.get(position);

        ImageView image = view.findViewById(R.id.iv_recipe_item);
        TextView title = view.findViewById(R.id.tv_title_recipe_item);
        TextView details = view.findViewById(R.id.tv_details_recipe_item);
        CheckBox favorite = view.findViewById(R.id.cb_favorite_recipe_item);

        title.setText(recipe.getName());
        details.setText(recipe.getDescription());

        if (recipe.getBase64Image() != null && !recipe.getBase64Image().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(recipe.getBase64Image(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                image.setImageBitmap(bitmap);
            } catch (Exception e) {
                image.setImageResource(R.drawable.ic_recipe_book_logo);
            }
        } else {
            image.setImageResource(R.drawable.ic_recipe_book_logo);
        }

        favorite.setOnClickListener(null);

        favorite.setChecked(userLiked(recipe));
        favorite.setText("Favorite (" + likesCount(recipe) + ")");

        favorite.setOnClickListener(v -> {
            boolean isChecked = favorite.isChecked();

            if (currentUserId == null) {
                Toast.makeText(context, "Login first", Toast.LENGTH_SHORT).show();
                favorite.setChecked(false);
                return;
            }

            if (recipe.getId() == null || recipe.getId().isEmpty()) {
                Toast.makeText(context, "Recipe id missing", Toast.LENGTH_SHORT).show();
                return;
            }

            favorite.setEnabled(false);

            if (isChecked) {
                db.collection("recipes")
                        .document(recipe.getId())
                        .update("likedBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            favorite.setChecked(false);
                            Toast.makeText(context, "Like failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        })
                        .addOnCompleteListener(task -> favorite.setEnabled(true));
            } else {
                db.collection("recipes")
                        .document(recipe.getId())
                        .update("likedBy", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            favorite.setChecked(true);
                            Toast.makeText(context, "Unlike failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        })
                        .addOnCompleteListener(task -> favorite.setEnabled(true));
            }
        });

        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShowRecipeActivity.class);
            intent.putExtra("RECIPE_ID", recipe.getId());
            context.startActivity(intent);
        });

        return view;
    }
}