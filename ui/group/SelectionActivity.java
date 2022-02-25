package com.travity.ui.group;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;
import androidx.fragment.app.Fragment;

import com.travity.R;

public class SelectionActivity extends AppCompatActivity {
//    private static final int ACTIVITY_REQUEST_CODE = 1;
    Bundle extras;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_second);
/*
        extras = null;

        String newString;
        if (savedInstanceState == null) {
            extras = getIntent().getExtras();
            if(extras == null) {
                newString= null;
            } else {
                newString = extras.getString("group_name");
                Log.v("test", "getString: " + newString);
            }
        } else {
            newString= (String) savedInstanceState.getSerializable("STRING_I_NEED");
        }
*/
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        String[] drama={"Blindspot","The Player","Silicon Valley","Hannibal","XIII"};

        ListView lv= (ListView) findViewById(R.id.dramaListView);
        ArrayAdapter adapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1, drama);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(getBaseContext(), drama[position], Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), CreateGroupActivity.class);
//                intent.putExtra("name", extras.getString("name"));
                intent.putExtra("activity", drama[position]);
//                intent.putExtra("description", extras.getString("description"));
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                setResult(Activity.RESULT_OK, intent);
//                startActivity(intent);
                finish();
            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
//                Intent intent = new Intent(this, SettingsActivity.class);
//                startActivity(intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}