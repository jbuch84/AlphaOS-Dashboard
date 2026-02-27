package com.github.ma1co.pmcademo.app;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Draws a safe, simple menu on the camera screen
        String[] items = {"Start Alpha OS Dashboard (Wi-Fi)"};
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (position == 0) {
            // Hands off the heavy network lifting to WifiActivity ONLY after you click
            startActivity(new Intent(this, WifiActivity.class));
        }
    }
}