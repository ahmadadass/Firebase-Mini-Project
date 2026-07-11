package com.ucas.recipebook;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class HomeFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ArrayList<Recipe> allRecipes;
    private ArrayList<Recipe> shownRecipes;
    private RecipeAdapter adapter;

    private ListenerRegistration recipesListener;

    private TabLayout categoriesTab;
    private String selectedCategory = "All";
    private String currentSearchText = "";

    public static HomeFragment newInstance(String name, String email, String country) {
        HomeFragment fragment = new HomeFragment();
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

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        allRecipes = new ArrayList<>();
        shownRecipes = new ArrayList<>();

        String currentUid = null;

        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
        }

        ListView listView = view.findViewById(R.id.lv_for_you);
        SearchView searchView = view.findViewById(R.id.searchView);
        FloatingActionButton fabAddRecipe = view.findViewById(R.id.fab_add_recipe);
        categoriesTab = view.findViewById(R.id.tab_categories);

        categoriesTab.setVisibility(View.VISIBLE);

        adapter = new RecipeAdapter(requireContext(), shownRecipes, currentUid);
        listView.setAdapter(adapter);

        fabAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddRecipeActivity.class);
            startActivity(intent);
        });

        categoriesTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedCategory = tab.getText() == null ? "All" : tab.getText().toString();
                applyFilters();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchText = query == null ? "" : query.trim();
                applyFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchText = newText == null ? "" : newText.trim();
                applyFilters();
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            currentSearchText = "";
            applyFilters();
            return false;
        });

        loadPublicRecipes();

        return view;
    }

    private void loadPublicRecipes() {
        recipesListener = db.collection("recipes")
                .whereEqualTo("publicRecipe", true)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot == null) return;

                    allRecipes.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Recipe recipe = doc.toObject(Recipe.class);

                        if (recipe != null) {
                            recipe.setId(doc.getId());
                            allRecipes.add(recipe);
                        }
                    }

                    rebuildCategoryTabs();
                    applyFilters();
                });
    }

    private void rebuildCategoryTabs() {
        if (categoriesTab == null) return;

        String oldCategory = selectedCategory;

        LinkedHashSet<String> categories = new LinkedHashSet<>();
        categories.add("All");

        for (Recipe recipe : allRecipes) {
            String category = recipe.getCategory();

            if (category != null && !category.trim().isEmpty()) {
                categories.add(category.trim());
            }
        }

        categoriesTab.removeAllTabs();

        boolean oldCategoryStillExists = false;

        for (String category : categories) {
            TabLayout.Tab tab = categoriesTab.newTab().setText(category);
            categoriesTab.addTab(tab, false);

            if (category.equalsIgnoreCase(oldCategory)) {
                oldCategoryStillExists = true;
            }
        }

        if (!oldCategoryStillExists) {
            selectedCategory = "All";
        }

        for (int i = 0; i < categoriesTab.getTabCount(); i++) {
            TabLayout.Tab tab = categoriesTab.getTabAt(i);

            if (tab != null && tab.getText() != null &&
                    tab.getText().toString().equalsIgnoreCase(selectedCategory)) {
                tab.select();
                break;
            }
        }
    }

    private void applyFilters() {
        shownRecipes.clear();

        String query = currentSearchText == null ? "" : currentSearchText.toLowerCase().trim();
        String categoryFilter = selectedCategory == null ? "All" : selectedCategory.trim();

        for (Recipe recipe : allRecipes) {
            String name = recipe.getName() == null ? "" : recipe.getName().toLowerCase();
            String description = recipe.getDescription() == null ? "" : recipe.getDescription().toLowerCase();
            String category = recipe.getCategory() == null ? "" : recipe.getCategory();

            boolean matchesSearch =
                    query.isEmpty()
                            || name.contains(query)
                            || description.contains(query)
                            || category.toLowerCase().contains(query);

            boolean matchesCategory =
                    categoryFilter.equalsIgnoreCase("All")
                            || category.equalsIgnoreCase(categoryFilter);

            if (matchesSearch && matchesCategory) {
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