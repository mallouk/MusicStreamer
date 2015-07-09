package mallouk.screenActivities;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mallouk.musicstreamer.BucketManager;
import mallouk.musicstreamer.R;

/**
 * Created by Matthew Jallouk on 7/3/2015.
 */
public class PlayMusicActivity extends Activity implements View.OnTouchListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnBufferingUpdateListener, AdapterView.OnItemClickListener,
        View.OnClickListener {

    private ListView playMusicView = null;
    private BucketManager bucketManager = null;
    private String playPause = "Play";
    private ImageButton repeatButton = null;
    private ImageButton playPauseButton = null;
    private ImageButton backButton = null;
    private ImageButton forwardButton = null;

    private SeekBar seekBarProgress;
    private MediaPlayer player;
    private int numItemsInBucket = 0;
    private int mediaFileLengthInMilliseconds;
    private final Handler handler = new Handler();

    final int[] currPos = new int[1];
    final AdapterView<?>[] view = new AdapterView<?>[1];
    int repeatSwiticher = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playmusic);
        playMusicView = (ListView)findViewById(R.id.musicView);
        String bucketName = (String)getIntent().getSerializableExtra("BucketName");
        bucketManager = new BucketManager(bucketName);
        playPauseButton = (ImageButton)findViewById(R.id.playImageButton);
        playPauseButton.setImageResource(R.drawable.play_icon);

        repeatButton = (ImageButton)findViewById(R.id.repeatButton);
        repeatButton.setImageResource(R.drawable.offrepeat_icon);
        backButton = (ImageButton)findViewById(R.id.backButton);
        backButton.setImageResource(R.drawable.back_icon);
        forwardButton = (ImageButton)findViewById(R.id.forwardButton);
        forwardButton.setImageResource(R.drawable.forward_icon);
        currPos[0] = -1;

        repeatButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repeatSwiticher == 1) {
                    repeatButton.setImageResource(R.drawable.onrepeat_icon);
                    repeatSwiticher = 2;
                } else if (repeatSwiticher == 2) {
                    repeatButton.setImageResource(R.drawable.offrepeat_icon);
                    repeatSwiticher = 1;
                } else {
                    repeatButton.setImageResource(R.drawable.offrepeat_icon);
                }
            }
        });

        forwardButton.setOnClickListener(this);

        backButton.setOnClickListener(this);

        new SpillBucketTask(bucketName).execute();
        playMusicView.setOnItemClickListener(this);
        playPauseButton.setOnClickListener(this);

        seekBarProgress = (SeekBar)findViewById(R.id.seekBar);
        seekBarProgress.setMax(99); // It means 100% .0-99
        seekBarProgress.setOnTouchListener(this);

        player = new MediaPlayer();
        player.setOnBufferingUpdateListener(this);
    }


    /** Method which updates the SeekBar primary progress by current song playing position*/
    private void primarySeekBarProgressUpdater() {
        seekBarProgress.setProgress((int)(((float)player.getCurrentPosition()/mediaFileLengthInMilliseconds)*100)); // This math construction give a percentage of "was playing"/"song length"
        if (playPause.equals("Pause")) {
            Runnable notification = new Runnable() {
                public void run() {
                    primarySeekBarProgressUpdater();
                }
            };
            handler.postDelayed(notification,1000);
        }
    }


    public void processMusic() throws Exception{
        String fileName = String.valueOf(view[0].getItemAtPosition(currPos[0]));
        String url = bucketManager.getFileURL(fileName) + "";
        url = "http" + url.substring(4, url.length());

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setDataSource(url);
        playPause = "Pause";
        playPauseButton.setImageResource(R.drawable.pause_icon);
        player.setOnCompletionListener(this);
        Runnable r = new Runnable() {
            public void run(){
                try{
                    player.prepare();
                }catch (Exception e) {
                    // TODO: handle exception
                }
            }
        };

        ExecutorService executeT1 = Executors.newFixedThreadPool(1);
        executeT1.execute(r);
        executeT1.shutdownNow();
        while (!executeT1.isTerminated()){};
        player.start();

        mediaFileLengthInMilliseconds = player.getDuration();


        primarySeekBarProgressUpdater();
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        seekBarProgress.setSecondaryProgress(percent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //Toast.makeText(getApplicationContext(), "Hello!", Toast.LENGTH_LONG).show();
        player.release();
        if (repeatSwiticher == 2){
            if (currPos[0] == (numItemsInBucket - 1)) {
                currPos[0] = 0;
            } else {
                currPos[0]++;
            }
            player = new MediaPlayer();
            player.setOnBufferingUpdateListener(this);
            try {
                processMusic();
            }catch(Exception e){
                e.printStackTrace();
            }
            playMusicView.setItemChecked(currPos[0], true);
        }else{
            playPauseButton.setImageResource(R.drawable.play_icon);
            player = new MediaPlayer();
            player.setOnBufferingUpdateListener(this);

            playMusicView.clearChoices();
            playMusicView.requestLayout();
            playPause = "Play";
            currPos[0] = -1;
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v.getId() == R.id.seekBar){
            /** Seekbar onTouch event handler. Method which seeks MediaPlayer to seekBar primary progress position*/
            //if(playPauseButton.getText().toString().equals("Pause")){
                SeekBar sb = (SeekBar)v;
                int playPositionInMillisecconds = (mediaFileLengthInMilliseconds / 100) * sb.getProgress();
                player.seekTo(playPositionInMillisecconds);
            //}
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parentView, View view1, int position, long id) {
        Toast.makeText(getApplicationContext(), numItemsInBucket + "", Toast.LENGTH_LONG).show();
        player.stop();
        player = new MediaPlayer();
        playPause = "Play";
        playPauseButton.setImageResource(R.drawable.play_icon);

        currPos[0] = position;
        view[0] = parentView;
        try{
            processMusic();
        }catch(Exception e){
            e.printStackTrace();
        }


    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.playImageButton) {
            try {
                if (playPause.equals("Play")) {
                    processMusic();
                } else if (playPause.equals("UnPause")) {
                    playPause = "Pause";
                    playPauseButton.setImageResource(R.drawable.pause_icon);

                    player.start();
                } else if (playPause.equals("Pause")) {
                    playPause = "UnPause";
                    playPauseButton.setImageResource(R.drawable.play_icon);
                    player.pause();
                }

                primarySeekBarProgressUpdater();
            } catch (Exception e) {
                // TODO: handle exception
            }
        }else if(v.getId() == R.id.forwardButton){
            if (currPos[0] == -1){
                Toast.makeText(getApplicationContext(), "You must have a song selected to switch from.", Toast.LENGTH_LONG).show();
            }else{
                player.release();

                if (currPos[0] == (numItemsInBucket - 1)) {
                    currPos[0] = 0;
                } else {
                    currPos[0]++;
                }
                player = new MediaPlayer();
                player.setOnBufferingUpdateListener(this);
                try {
                    processMusic();
                }catch(Exception e){
                    e.printStackTrace();
                }
                playMusicView.setItemChecked(currPos[0], true);
            }
        }else if(v.getId() == R.id.backButton){
            if (currPos[0] == -1){
                Toast.makeText(getApplicationContext(), "You must have a song selected to switch from.", Toast.LENGTH_LONG).show();
            }else{
                player.release();

                if (currPos[0] == 0) {
                    currPos[0] = (numItemsInBucket - 1);
                } else {
                    currPos[0]--;
                }
                player = new MediaPlayer();
                player.setOnBufferingUpdateListener(this);
                try {
                    processMusic();
                }catch(Exception e){
                    e.printStackTrace();
                }
                playMusicView.setItemChecked(currPos[0], true);
            }
        }
    }



    /** Inner class that acts as a way to spill the contents of a particular bucket onto the listView
     *  screen.
     *
     */
    public class SpillBucketTask extends AsyncTask<Void, Void, ArrayList<String>> {
        //Define instance variables
        private String bucketName;

        /** Constructor that takes the bucketName that we are taking the contents of to spill.
         *
         * @param bucketName                    name of the bucket to spill.
         */
        public SpillBucketTask(String bucketName){
            this.bucketName = bucketName;
        }

        /** Method that runs when this task is executed. It lists the takes the objects of the
         *  bucket and lists the file names to have them placed in an Array.
         *
         * @param voids
         * @return                              return the list of items to be placed on the
         *                                      screen.
         */
        public ArrayList<String> doInBackground(Void... voids) {
            ArrayList<String> filesInBucket = null;
            try {
                filesInBucket = bucketManager.listObjectsInBucket(bucketName);
            } catch (Exception e) {}
            return filesInBucket;
        }

        /** After the execution of the method above, we wll then update our adapter to list the
         *  files on the screen and add that adapter onto the ListView to then show.
         *
         * @param filesInBucket
         */
        public void onPostExecute(ArrayList<String> filesInBucket) {
            ListAdapter list = new ArrayAdapter<String>(getApplicationContext(),
                    android.R.layout.simple_list_item_multiple_choice, filesInBucket);
            playMusicView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            playMusicView.setAdapter(list);
            numItemsInBucket = filesInBucket.size();
        }
    }
}
