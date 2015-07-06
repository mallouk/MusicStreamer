package mallouk.screenActivities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import mallouk.musicstreamer.BucketManager;
import mallouk.musicstreamer.R;


public class MainActivity extends ActionBarActivity {

    private Button playMusic = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playMusic = (Button)findViewById(R.id.playMusic);
        runButtonListeners();
    }

    public void runButtonListeners(){
        playMusic.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v){
                BucketManager bucket = new BucketManager("musictestapp");
                Intent i = new Intent();
                i.putExtra("BucketName", bucket.getBucketName());

                i.setClass(MainActivity.this, PlayMusicActivity.class);
                //Launch the next activity.
                startActivity(i);
            }
        });
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
