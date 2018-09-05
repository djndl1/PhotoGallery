package com.bignerdranch.android.photogallery;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by djn on 18-8-14.
 * An abstract class characterized by its property of containing a single fragment
 */

public abstract class SingleFragmentActivity extends AppCompatActivity {
    protected abstract Fragment createFragment();

    @LayoutRes
    protected int getLayoutResId() {
        return R.layout.activity_fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());//layout not hardcoded

        FragmentManager fm = getSupportFragmentManager(); // inherited from AppCompatActivity
        Fragment frag = fm.findFragmentById(R.id.fragment_container);
        //null returned if not in the list,
        // otherwise the fragment corresponding to the id// is returned

        //if the fragment is not in the list, i.d. , not created, then add one to the list of FragmentManager
        if (frag == null) {
            frag = createFragment();
            fm.beginTransaction().add(R.id.fragment_container, frag).commit(); // fluent method
        }
    }
}
