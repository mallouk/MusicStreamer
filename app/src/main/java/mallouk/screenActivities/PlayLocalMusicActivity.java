package mallouk.screenActivities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mallouk.musicstreamer.AmazonAccountKeys;
import mallouk.musicstreamer.R;

public class PlayLocalMusicActivity extends Activity implements View.OnTouchListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnBufferingUpdateListener, AdapterView.OnItemClickListener,
        View.OnClickListener {

    private ListView playMusicView = null;
    private String playPause = "Play";
    private ImageButton repeatButton = null;
    private ImageButton playPauseButton = null;

    private TextView currentDirectoryView = null;
    private TextView mode = null;
    private TextView songTime = null;
    private RelativeLayout loadingPanel = null;
    private SeekBar seekBarProgress;
    private MediaPlayer player;
    private int numItemsInBucket = 0;
    private final Handler handler = new Handler();
    final int[] currPos = new int[1];
    final AdapterView<?>[] view = new AdapterView<?>[1];
    private boolean repeatOn = false;
    private int selectedIndex = -1;
    private int prevViewIndex = -1;

    private ArrayList<String> formatedFilesInBucket = new ArrayList<String>();
    private boolean downloadActive = false;
    private ArrayList<String> selectedDownloadedSong = new ArrayList<String>();
    private int downloadLevel = 1;
    private boolean[] itemToggle;
    private ProgressDialog downloadProgress = null;
    private String root = Environment.getExternalStorageDirectory() + AmazonAccountKeys.getAppFolder();

    private String globalBucketName = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localmusic);
        playMusicView = (ListView)findViewById(R.id.musicView);
        playPauseButton = (ImageButton)findViewById(R.id.playImageButton);
        playPauseButton.setImageResource(R.drawable.play_icon);

        mode = (TextView)findViewById(R.id.mode);
        mode.setText("Local Music Mode");
        songTime = (TextView)findViewById(R.id.songTime);
        loadingPanel = (RelativeLayout)findViewById(R.id.loadingPanel);

        repeatButton = (ImageButton)findViewById(R.id.repeatButton);
        repeatButton.setImageResource(R.drawable.offrepeat_icon);
        ImageButton backButton = (ImageButton)findViewById(R.id.backButton);
        backButton.setImageResource(R.drawable.back_icon);
        ImageButton forwardButton = (ImageButton)findViewById(R.id.forwardButton);
        forwardButton.setImageResource(R.drawable.forward_icon);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        currentDirectoryView = (TextView)findViewById(R.id.songName);
        currentDirectoryView.setText("  /");
        currPos[0] = -1;

        //Define listeners
        repeatButton.setOnClickListener(this);
        forwardButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
        playPauseButton.setOnClickListener(this);

        String path = Environment.getExternalStorageDirectory() +
                AmazonAccountKeys.getAppFolder();
        File f = new File(path);
        File[] files = f.listFiles();
        String[] token = files[0].toString().split("/");

        new SpillBucketTask("").execute();
        playMusicView.setOnItemClickListener(this);

        seekBarProgress = (SeekBar)findViewById(R.id.seekBar);
        seekBarProgress.setMax(99); // It means 100% .0-99
        seekBarProgress.setOnTouchListener(this);

        player = new MediaPlayer();
        player.setOnBufferingUpdateListener(this);
    }

//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
//            //Toast.makeText(getApplicationContext(), "Vol Decreased.", Toast.LENGTH_LONG).show();
//            if (audioManager != null){
//                int currVol = audioManager.getStreamVolume(audioManager.STREAM_MUSIC);
//                currVol-=2;
//                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currVol, 0);
//            }
//        }else if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
//            //Toast.makeText(getApplicationContext(), "Vol Increased.", Toast.LENGTH_LONG).show();
//            if (audioManager != null){
//                int currVol = audioManager.getStreamVolume(audioManager.STREAM_MUSIC);
//                currVol+=2;
//                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currVol, 0);
//            }
//        }
//            return true;
//    }
	
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.playImageButton) {
            try {
                if (currPos[0] == -1 || downloadLevel == 2){
                    Toast.makeText(getApplicationContext(), "You must have a song selected to play from.", Toast.LENGTH_LONG).show();
                }else {
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
                }
                primarySeekBarProgressUpdater();
            } catch (Exception e) {
                //Do Nothing
            }
        }else if(v.getId() == R.id.forwardButton){
            if (currPos[0] == -1 || downloadLevel == 2){
                Toast.makeText(getApplicationContext(), "You must have a song selected to switch from.", Toast.LENGTH_LONG).show();
            }else{
                player.release();
                if (currPos[0] == (numItemsInBucket)) {
                    currPos[0] = 0;
                    String fileName = String.valueOf(view[0].getItemAtPosition(currPos[0]));
                    while (!fileName.endsWith("mp3")){
                        currPos[0]++;
                        fileName = String.valueOf(view[0].getItemAtPosition(currPos[0]));
                    }
                } else {
                    currPos[0]++;
                }
                player = new MediaPlayer();
                player.setOnBufferingUpdateListener(this);
                try {
                    getViewByPosition(selectedIndex, playMusicView).setBackgroundColor(Color.BLACK);
                    getViewByPosition(currPos[0], playMusicView).setBackgroundColor(Color.GRAY);
                    selectedIndex=currPos[0];

                    processMusic();
                }catch(Exception e){
                    e.printStackTrace();
                }
                playMusicView.setItemChecked(currPos[0], true);
            }
        }else if(v.getId() == R.id.backButton){
            if (currPos[0] == -1 || downloadLevel == 2){
                Toast.makeText(getApplicationContext(), "You must have a song selected to switch from.", Toast.LENGTH_LONG).show();
            }else{
                player.release();
                if (currPos[0] > 0){
                    String fileName = String.valueOf(view[0].getItemAtPosition(currPos[0] - 1));
                    if (!fileName.endsWith("mp3")){
                        currPos[0] = numItemsInBucket;
                    }else{
                        currPos[0]--;
                    }
                }
                getViewByPosition(selectedIndex, playMusicView).setBackgroundColor(Color.BLACK);
                getViewByPosition(currPos[0], playMusicView).setBackgroundColor(Color.GRAY);
                selectedIndex=currPos[0];

                player = new MediaPlayer();
                player.setOnBufferingUpdateListener(this);
                try {
                    processMusic();
                }catch(Exception e){
                    e.printStackTrace();
                }
                playMusicView.setItemChecked(currPos[0], true);
            }
        }else if (v.getId() == R.id.repeatButton){
            if (!repeatOn) {
                repeatButton.setImageResource(R.drawable.onrepeat_icon);
                repeatOn = true;
            } else if (repeatOn) {
                repeatButton.setImageResource(R.drawable.offrepeat_icon);
                repeatOn = false;
            } else {
                repeatButton.setImageResource(R.drawable.offrepeat_icon);
            }
        }
    }

    private boolean songDone = false;
    /** Method which updates the SeekBar primary progress by current song playing position*/
    private void primarySeekBarProgressUpdater() {
        String songEnd = "";
        String songBegin = "";
        if (!songDone) {
            double songMin = (player.getDuration() / 1000) / 60;
            double songSec = (player.getDuration() / 1000) - (songMin * 60);

            if (songSec < 10) {
                songEnd = (int) songMin + ":0" + (int) songSec;
            } else {
                songEnd = (int) songMin + ":" + (int) songSec;
            }

            songMin = (player.getCurrentPosition() / 1000) / 60;
            songSec = (player.getCurrentPosition() / 1000) - (songMin * 60);
            if (songSec < 10) {
                songBegin = (int) songMin + ":0" + (int) songSec;
            } else {
                songBegin = (int) songMin + ":" + (int) songSec;
            }

            seekBarProgress.setProgress((int) (((float) player.getCurrentPosition() / player.getDuration()) * 100));
            songTime.setText(songBegin + "/" + songEnd);
        }

        if (songBegin.equals(songEnd)){
            songDone = true;
        }

        // This math construction give a percentage of "was playing"/"song length"
        if (playPause.equals("Pause")) {
            Runnable notification = new Runnable() {
                public void run() {
                    primarySeekBarProgressUpdater();
                }
            };
            handler.postDelayed(notification, 1000);
        }
    }

    private AudioManager audioManager = null;
    AudioManager.OnAudioFocusChangeListener audioListener = new AudioManager.OnAudioFocusChangeListener(){
        @Override
        public void onAudioFocusChange(int focusChange) {
            int currVol = audioManager.getStreamVolume(audioManager.STREAM_MUSIC);
            if (focusChange == audioManager.AUDIOFOCUS_GAIN) {
                player.start();
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currVol, 0);
            }else{
                player.pause();
            }
        }
    };

       public void processMusic() throws Exception{
        String fileName = String.valueOf(view[0].getItemAtPosition(currPos[0]));
		
        String originFile = "";
        if (fileName.contains("../")){
            String[] parse = currentDirectoryView.getText().toString().trim().split("/");
            String newDir = "";
            for (int i = 1; i < parse.length-1; i++){
                newDir+=parse[i] + "/";
            }
            originFile = newDir;
            currentDirectoryView.setText("  " + newDir);
        }else {
            if (currentDirectoryView.getText().toString().trim().equals("/")) {
                originFile = fileName + "";
            } else {
                String currDir = currentDirectoryView.getText().toString().trim();
                String dir = currDir.substring(1, currDir.length());
                originFile = dir + "/" + fileName;
            }
        }

        String url = root + "/" + originFile;
        //Toast.makeText(getApplicationContext(), url, Toast.LENGTH_SHORT).show();

        if (url.endsWith("mp3")){
            songDone = false;
            //audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int reqAudioChannel = audioManager.requestAudioFocus(audioListener,
                    audioManager.STREAM_MUSIC, audioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
			//Toast.makeText(getApplicationContext(), audioManager.getStreamVolume(audioManager.STREAM_MUSIC) + " GEt VOL1", Toast.LENGTH_SHORT).show();
            //Toast.makeText(getApplicationContext(), audioManager.getStreamVolume(audioManager.STREAM_NOTIFICATION) + " GEt VOL2", Toast.LENGTH_SHORT).show();


            if (reqAudioChannel == audioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.setDataSource(url);
                playPause = "Pause";
                playPauseButton.setImageResource(R.drawable.pause_icon);
                player.setOnCompletionListener(this);
                Runnable r = new Runnable() {
                    public void run() {
                        try {
                            player.prepare();
                        } catch (Exception e) {
                            //Do Nothing
                        }
                    }
                };

                ExecutorService executeT1 = Executors.newFixedThreadPool(1);
                executeT1.execute(r);
                executeT1.shutdownNow();
                while (!executeT1.isTerminated()) {
                }
                player.start();

                primarySeekBarProgressUpdater();
            }
        }else{
            playMusicView.setAdapter(null);
            currentDirectoryView.setText("  /" + originFile);
            new SpillBucketTask(currentDirectoryView.getText().toString().trim()).execute();
        }
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        seekBarProgress.setSecondaryProgress(percent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        player.release();
        if (currPos[0] == (numItemsInBucket)) {
            currPos[0] = 0;
            String fileName = String.valueOf(view[0].getItemAtPosition(currPos[0]));
            while (!fileName.endsWith("mp3")){
                currPos[0]++;
                fileName = String.valueOf(view[0].getItemAtPosition(currPos[0]));
            }
        } else {
            currPos[0]++;
        }
        getViewByPosition(selectedIndex, playMusicView).setBackgroundColor(Color.BLACK);
        getViewByPosition(currPos[0], playMusicView).setBackgroundColor(Color.GRAY);
        selectedIndex=currPos[0];

        player = new MediaPlayer();
        player.setOnBufferingUpdateListener(this);
        try {
            processMusic();
        }catch(Exception e){
            e.printStackTrace();
        }
        playMusicView.setItemChecked(currPos[0], true);

}

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v.getId() == R.id.seekBar){
            /** Seekbar onTouch event handler. Method which seeks MediaPlayer to seekBar primary progress position*/
            SeekBar sb = (SeekBar)v;
            int playPositionInMillisecconds = (player.getDuration() / 100) * sb.getProgress();
            player.seekTo(playPositionInMillisecconds);
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parentView, View view1, int position, long id) {
        String fileName = String.valueOf(parentView.getItemAtPosition(position));
        if (!downloadActive) {
            player.stop();
            player = new MediaPlayer();
            playPause = "Play";
            playPauseButton.setImageResource(R.drawable.play_icon);

            //Needed for code to work.
            currPos[0] = position;
            view[0] = parentView;
            try {
                if (fileName.endsWith("mp3")) {
                    selectedIndex = position;
                    songDone = false;
                } else {
                    selectedIndex = -1;
                    loadingPanel.setVisibility(View.VISIBLE);
                    songTime.setText("0:00/0:00");
                    songDone = true;
                }
                processMusic();

                if (prevViewIndex != -1) {
                    getViewByPosition(prevViewIndex, playMusicView).setBackgroundColor(Color.BLACK);
                }
                prevViewIndex = position;
                view1.setBackgroundColor(Color.GRAY);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            if (!fileName.endsWith("mp3") && !fileName.endsWith("jpg")){
                Toast.makeText(getApplicationContext(), "Sorry, but you cannot change folders while in download mode. " +
                        "To exit download mode, either download what you need or hit the back button." , Toast.LENGTH_SHORT).show();
            }else{
                ImageView imageView = (ImageView)view1.findViewById(R.id.musicIcon);
                if (itemToggle[position]){
                    //We unselect it
                    imageView.setImageResource(R.drawable.unchecked);
                    view1.setBackgroundColor(Color.BLACK);
                    selectedDownloadedSong.remove(fileName);
                    itemToggle[position] = false;
                }else{
                    //We make it selected
                    imageView.setImageResource(R.drawable.checked);
                    view1.setBackgroundColor(Color.GRAY);
                    selectedDownloadedSong.add(fileName);
                    itemToggle[position] = true;
                }
            }
        }
    }


    class CustomPlayView extends ArrayAdapter<String> {
        public CustomPlayView(Context context, ArrayList<String> songNames){
            super(context, R.layout.custom_row, songNames);
        }

        /** We overwrite the getView method to draw our custom list
         *
         * @param position          position of list we are creating
         * @param customView        custom view object
         * @param parent            parent object of view.
         * @return                  return view created.
         */
        public View getView(int position, View customView, ViewGroup parent) {
            if (customView == null) {
                LayoutInflater layoutInflator = LayoutInflater.from(getContext());
                customView = layoutInflator.inflate(R.layout.custom_row, null, true);
            }

            if (!downloadActive){
                //Gets song name
                String song = getItem(position);
                TextView songName = (TextView) customView.findViewById(R.id.songName);
                ImageView image = (ImageView) customView.findViewById(R.id.musicIcon);

                songName.setText(song);
                if (song.endsWith("mp3")) {
                    image.setImageResource(R.drawable.music);
                } else {
                    image.setImageResource(R.drawable.folder);
                }

                customView.setBackgroundColor(Color.BLACK);

                if (selectedIndex != -1) {
                    if (selectedIndex == position && getItem(selectedIndex).endsWith("mp3")) {
                        customView.setBackgroundColor(Color.GRAY);
                    }
                }

                return customView;
            }else{
                //Gets song name
                String song = getItem(position);
                TextView songName = (TextView) customView.findViewById(R.id.songName);
                ImageView image = (ImageView) customView.findViewById(R.id.musicIcon);

                songName.setText(song);
                if (song.endsWith("mp3")) {
                    if (itemToggle[position]) {
                        customView.setBackgroundColor(Color.GRAY);
                        image.setImageResource(R.drawable.checked);
                    }else{
                        customView.setBackgroundColor(Color.BLACK);
                        image.setImageResource(R.drawable.unchecked);
                    }
                } else {
                    image.setImageResource(R.drawable.folder);
                    customView.setBackgroundColor(Color.BLACK);
                }

                if (selectedIndex != -1) {
                    if (selectedIndex == position && getItem(selectedIndex).endsWith("mp3")) {
                        customView.setBackgroundColor(Color.GRAY);
                    }
                }
                return customView;
            }
        }
    }


    /** Inner class that acts as a way to spill the contents of a particular bucket onto the listView
     *  screen.
     *
     */
    public class SpillBucketTask extends AsyncTask<Void, Void, ArrayList<String>> {
        //Define instance variables
        private String subDir;

        public SpillBucketTask(String subDir){
            this.subDir = subDir;
        }

        /** Method that runs when this task is executed. It lists the takes the objects of the
         *  bucket and lists the file names to have them placed in an Array.
         *
         * @param voids                         something...
         * @return                              return the list of items to be placed on the
         *                                      screen.
         */
        public ArrayList<String> doInBackground(Void... voids) {
            ArrayList<String> filesInBucket = new ArrayList<String>();

            String path = root + subDir;
            File f = new File(path);
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                String[] token = files[i].toString().split("/");
                filesInBucket.add(token[token.length - 1]);
            }

            return filesInBucket;
        }

        /** After the execution of the method above, we wll then update our adapter to list the
         *  files on the screen and add that adapter onto the ListView to then show.
         *
         * @param filesInBucket                 files in bucket
         */
        public void onPostExecute(ArrayList<String> filesInBucket) {
            formatedFilesInBucket = new ArrayList<String>();
            if (!currentDirectoryView.getText().equals("  /")){
                formatedFilesInBucket.add("../ (go up one level)");
            }

            for (int i = 0; i < filesInBucket.size(); i++){
                String[] tok = filesInBucket.get(i).split("/");

                if (!filesInBucket.get(i).endsWith(".mp3") && tok.length == 1){
                    formatedFilesInBucket.add(filesInBucket.get(i));
                }
            }

            for (int i = 0; i < filesInBucket.size(); i++){
                if (filesInBucket.get(i).endsWith(".mp3")){
                    formatedFilesInBucket.add(filesInBucket.get(i));
                }
            }

            ListAdapter list = new CustomPlayView(getApplicationContext(), formatedFilesInBucket);
            playMusicView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            playMusicView.setAdapter(list);
            numItemsInBucket = filesInBucket.size();
            itemToggle = new boolean[formatedFilesInBucket.size()];
            Arrays.fill(itemToggle, false);
            loadingPanel.setVisibility(View.INVISIBLE);
        }
    }
}
