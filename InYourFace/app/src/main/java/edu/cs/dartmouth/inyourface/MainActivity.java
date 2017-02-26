package edu.cs.dartmouth.inyourface;

import java.util.*;

import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.app.Fragment;
import android.os.Bundle;


public class MainActivity extends AppCompatActivity
{
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ArrayList<Fragment> fragmentArray;
    private TabsViewPagerAdapter viewPageAdapter;   // self-defined adapter

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create the instance of the tablayout from the main layout
        tabLayout = (TabLayout) findViewById(R.id.tab);         // defined in main xml
        viewPager = (ViewPager) findViewById(R.id.viewpager);   // defined in main xml

        // create the array of fragments
        fragmentArray = new ArrayList<Fragment>();
        fragmentArray.add(new SettingFragment());
        fragmentArray.add(new AuthenticationFragment());
        fragmentArray.add(new EmotionsFragment());

        // bind the tab layout to the viewpager
        viewPageAdapter = new TabsViewPagerAdapter(getFragmentManager(), fragmentArray);
        viewPager.setAdapter(viewPageAdapter);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
    }

}

