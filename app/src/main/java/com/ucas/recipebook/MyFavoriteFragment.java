package com.ucas.recipebook;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class MyFavoriteFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ArrayList<Recipe> allRecipes;
    private ArrayList<Recipe> shownRecipes;
    private RecipeAdapter adapter;

    private ListenerRegistration recipesListener;
    private String currentUid;

    public static MyFavoriteFragment newInstance(String name, String email, String country) {
        MyFavoriteFragment fragment = new MyFavoriteFragment();
        Bundle args = new Bundle();
        args.putString("NAME", name);
        args.putString("EMAIL", email);
        args.putString("COUNTRY", country);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_favorite, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        allRecipes = new ArrayList<>();
        shownRecipes = new ArrayList<>();

        ListView listView = view.findViewById(R.id.lv_my_favorite);
        SearchView searchView = view.findViewById(R.id.sv_my_favorite);

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Login first", Toast.LENGTH_SHORT).show();
            return view;
        }

        currentUid = mAuth.getCurrentUser().getUid();

        adapter = new RecipeAdapter(requireContext(), shownRecipes, currentUid);
        listView.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterRecipes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterRecipes(newText);
                return true;
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (currentUid != null) {
            loadFavoriteRecipes(currentUid);
        }
    }

    private void loadFavoriteRecipes(String currentUid) {
        if (recipesListener != null) {
            recipesListener.remove();
        }

        recipesListener = db.collection("recipes")
                .whereArrayContains("likedBy", currentUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot == null) return;

                    allRecipes.clear();
                    shownRecipes.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Recipe recipe = doc.toObject(Recipe.class);

                        if (recipe != null) {
                            recipe.setId(doc.getId());
                            allRecipes.add(recipe);
                            shownRecipes.add(recipe);
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void filterRecipes(String text) {
        shownRecipes.clear();

        String query = text == null ? "" : text.toLowerCase().trim();

        for (Recipe recipe : allRecipes) {
            String name = recipe.getName() == null ? "" : recipe.getName().toLowerCase();
            String description = recipe.getDescription() == null ? "" : recipe.getDescription().toLowerCase();
            String category = recipe.getCategory() == null ? "" : recipe.getCategory().toLowerCase();

            if (name.contains(query) || description.contains(query) || category.contains(query)) {
                shownRecipes.add(recipe);
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (recipesListener != null) {
            recipesListener.remove();
            recipesListener = null;
        }
    }
}