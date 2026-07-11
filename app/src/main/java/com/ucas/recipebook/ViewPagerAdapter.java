package com.ucas.recipebook;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final String name;
    private final String email;
    private final String country;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                            String name,
                            String email,
                            String country) {
        super(fragmentActivity);
        this.name = name;
        this.email = email;
        this.country = country;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return HomeFragment.newInstance(name, email, country);
            case 1:
                return MyRecipesFragment.newInstance(name, email, country);
            case 2:
                return MyFavoriteFragment.newInstance(name, email, country);
            case 3:
                return ProfileFragment.newInstance(name, email, country);
            default:
                Log.e("ViewPagerAdapter", "position out of bounds position = " + position);
                return HomeFragment.newInstance(name, email, country);
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}