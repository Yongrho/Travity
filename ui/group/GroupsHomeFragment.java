package com.travity.ui.group;

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

public class GroupsHomeFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        ViewPager homeViewPager = (ViewPager) root.findViewById(R.id.viewpager);

        TabLayout tabLayout = (TabLayout) root.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(homeViewPager);

        setupViewPager(homeViewPager);
        return root;
    }

    private void setupViewPager(ViewPager viewPager) {
        TabViewPagerAdapter adapter = new TabViewPagerAdapter(getChildFragmentManager());
        adapter.addFragment(new GroupsEventsFragment(), "Events");
        adapter.addFragment(new GroupsMembersFragment(), "Members");
        adapter.addFragment(new GroupsDetailsFragment(), "Details");

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

