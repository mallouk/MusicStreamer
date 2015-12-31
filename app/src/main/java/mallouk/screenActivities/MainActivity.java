package mallouk.screenActivities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

import mallouk.musicstreamer.AmazonAccountKeys;
import mallouk.musicstreamer.BucketManager;
import mallouk.musicstreamer.R;


public class MainActivity extends ActionBarActivity {

    private Button streamMusic = null;
    private Button playLocal = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        streamMusic = (Button)findViewById(R.id.streamMusic);
        playLocal = (Button)findViewById(R.id.playLocalMusic);

        runButtonListeners();
    }

    public void runButtonListeners(){
        streamMusic.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (isNetworkAvailable()) {
                    BucketManager bucket = new BucketManager("musictestapp");

                    File folder = new File(Environment.getExternalStorageDirectory() +
                            AmazonAccountKeys.getAppFolder());
                    boolean folderExists = true;
                    if (!folder.exists()) {
                        folderExists = folder.mkdir();
                    }

                    Intent i = new Intent();
                    i.putExtra("BucketName", bucket.getBucketName());

                    i.setClass(MainActivity.this, PlayMusicActivity.class);
                    //Launch the next activity.
                    startActivity(i);
                }else{
                    Toast.makeText(getApplicationContext(), "Your Wifi or 3G/4G data doesn't seem to be active. You can't run this " +
                                    "streaming music function without access to the internet.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        playLocal.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                BucketManager bucket = new BucketManager("musictestapp");

                File folder = new File(Environment.getExternalStorageDirectory() +
                        AmazonAccountKeys.getAppFolder());
                boolean folderExists = true;
                if (!folder.exists()) {
                    folderExists = folder.mkdir();
                }

                String path = Environment.getExternalStorageDirectory() +
                        AmazonAccountKeys.getAppFolder();
                File f = new File(path);
                File[] files = f.listFiles();

                if (files.length != 0) {
                    Intent i = new Intent();
                    i.putExtra("BucketName", bucket.getBucketName());

                    i.setClass(MainActivity.this, PlayLocalMusicActivity.class);
                    //Launch the next activity.
                    startActivity(i);
                }else{
                    Toast.makeText(getApplicationContext(), "You have no local music to play.", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
