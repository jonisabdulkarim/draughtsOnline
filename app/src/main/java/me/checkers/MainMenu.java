package me.checkers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

public class MainMenu extends AppCompatActivity {
    public static final String EXTRA_BOOLEAN = "isOnline";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button button = (Button) findViewById(R.id.button5);


    }

    public void createOfflineActivity(View view){
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_BOOLEAN, false);
        startActivity(intent);
    }

    public void createOnlineActivity(View view){
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_BOOLEAN, true);
        startActivity(intent);
    }

}
