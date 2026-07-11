package com.ucas.recipebook;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ImageView iv_profile_image;
    private TextView tv_name, tv_email;
    private Button btn_logout;

    private ArrayList<Recipe> myRecipes;
    private ProfileRecipeAdapter adapter;
    private ListenerRegistration myRecipesListener;

    public static ProfileFragment newInstance(String name, String email, String country) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("NAME", name);
        args.putString("EMAIL", email);
        args.putString("COUNTRY", country);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        iv_profile_image = view.findViewById(R.id.iv_profile_image);
        tv_name = view.findViewById(R.id.tv_name);
        tv_email = view.findViewById(R.id.tv_email);
        btn_logout = view.findViewById(R.id.btn_logout);

        ListView lvProfileMyRecipes = view.findViewById(R.id.lv_profile_my_recipes);

        myRecipes = new ArrayList<>();

        if (mAuth.getCurrentUser() != null) {
            String currentUid = mAuth.getCurrentUser().getUid();

            adapter = new ProfileRecipeAdapter(requireContext(), myRecipes, currentUid);
            lvProfileMyRecipes.setAdapter(adapter);

            loadProfileFromFirestore();
            loadMyRecipes(currentUid);
        } else {
            Toast.makeText(getContext(), "Login first", Toast.LENGTH_SHORT).show();
        }

        btn_logout.setOnClickListener(e -> {
            mAuth.signOut();

            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        return view;
    }

    private void loadProfileFromFirestore() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(getContext(), "Profile not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String name = document.getString("name");
                    String email = document.getString("email");
                    String base64Image = document.getString("profileImage");

                    tv_name.setText(name == null ? "Name" : name);
                    tv_email.setText(email == null ? "email@gmail.com" : email);

                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            iv_profile_image.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            iv_profile_image.setImageResource(R.drawable.baseline_account_circle_24);
                        }
                    } else {
                        iv_profile_image.setImageResource(R.drawable.baseline_account_circle_24);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadMyRecipes(String currentUid) {
        myRecipesListener = db.collection("recipes")
                .whereEqualTo("authorId", currentUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot == null) return;

                    myRecipes.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Recipe recipe = doc.toObject(Recipe.class);

                        if (recipe != null) {
                            recipe.setId(doc.getId());
                            myRecipes.add(recipe);
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (myRecipesListener != null) {
            myRecipesListener.remove();
            myRecipesListener = null;
        }
    }
}