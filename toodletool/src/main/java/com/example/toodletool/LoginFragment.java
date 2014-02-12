package com.example.toodletool;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class LoginFragment extends Fragment {
    public interface ViewReadyListener {
        public void onViewReadyListener();
    }

    private ViewReadyListener listener;

    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        try {
            listener = (ViewReadyListener) a;
        } catch (ClassCastException e) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listener.onViewReadyListener();
    }
}
