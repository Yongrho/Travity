package com.travity.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import java.util.ArrayList;
import java.util.List;

import com.travity.R;

public class HomeFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        ViewPager homeViewPager = (ViewPager) rootView.findViewById(R.id.viewpager);

        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(homeViewPager);

        setupViewPager(homeViewPager);
        return rootView;
    }

    private void setupViewPager(ViewPager viewPager) {
        TabViewPagerAdapter adapter = new TabViewPagerAdapter(getChildFragmentManager());
        adapter.addFragment(new HomeEventsFragment(), "Events");
        adapter.addFragment(new HomeHistoryFragment(), "History");
        adapter.addFragment(new HomeStampsFragment(), "Stamps");

        viewPager.setAdapter(adapter);
    }

    class TabViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragments = new ArrayList<>();
        private final List<String> fragmentTitle = new ArrayList<>();

        public TabViewPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        public void addFragment(Fragment fragment, String name) {
            fragments.add(fragment);
            fragmentTitle.add(name);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return fragments.size();
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitle.get(position);
        }
    }
}

