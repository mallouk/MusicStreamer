package mallouk.screenActivities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.services.s3.model.S3Object;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mallouk.musicstreamer.AmazonAccountKeys;
import mallouk.musicstreamer.BucketManager;
import mallouk.musicstreamer.R;

public class PlayMusicActivity extends Activity implements View.OnTouchListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnBufferingUpdateListener, AdapterView.OnItemClickListener,
        View.OnClickListener {

    private ListView playMusicView = null;
    private BucketManager bucketManager = null;
    private String playPause = "Play";
    private ImageButton repeatButton = null;
    private ImageButton playPauseButton = null;
    private ImageButton downloadButton = null;

    private TextView currentDirectoryView = null;
    private TextView mode = null;
    private TextView songTime = null;
    private TextView pullData = null;
    private SeekBar seekBarProgress;
    private MediaPlayer player;
    private int numItemsInBucket = 0;
    private int mediaFileLengthInMilliseconds;
    private final Handler handler = new Handler();
    final int[] currPos = new int[1];
    final AdapterView<?>[] view = new AdapterView<?>[1];
    int repeatSwiticher = 1;
    private String globalBucketName;
    private int selectedIndex = -1;
    private int prevViewIndex = -1;

    private ArrayList<String> formatedFilesInBucket = new ArrayList<String>();
    private boolean downloadActive = false;
    private ArrayList<String> selectedDownloadedSong = new ArrayList<String>();
    private int downloadLevel = 1;
    private boolean[] itemToggle;
    private ProgressDialog downloadProgress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playmusic);
        playMusicView = (ListView)findViewById(R.id.musicView);
        String bucketName = (String)getIntent().getSerializableExtra("BucketName");
        globalBucketName = bucketName;
        bucketManager = new BucketManager(bucketName);
        playPauseButton = (ImageButton)findViewById(R.id.playImageButton);
        playPauseButton.setImageResource(R.drawable.play_icon);
        downloadButton = (ImageButton)findViewById(R.id.downloadButton);
        downloadButton.setImageResource(R.drawable.download_icon);
        mode = (TextView)findViewById(R.id.mode);
        songTime = (TextView)findViewById(R.id.songTime);
        pullData = (TextView)findViewById(R.id.pullData);

        repeatButton = (ImageButton)findViewById(R.id.repeatButton);
        repeatButton.setImageResource(R.drawable.offrepeat_icon);
        ImageButton backButton = (ImageButton)findViewById(R.id.backButton);
        backButton.setImageResource(R.drawable.back_icon);
        ImageButton forwardButton = (ImageButton)findViewById(R.id.forwardButton);
        forwardButton.setImageResource(R.drawable.forward_icon);

        currentDirectoryView = (TextView)findViewById(R.id.songName);
        currentDirectoryView.setText("  /");
        currPos[0] = -1;

        //Define listeners
        repeatButton.setOnClickListener(this);
        forwardButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
        playPauseButton.setOnClickListener(this);
        downloadButton.setOnClickListener(this);

        new SpillBucketTask(bucketName, "").execute();
        playMusicView.setOnItemClickListener(this);

        seekBarProgress = (SeekBar)findViewById(R.id.seekBar);
        seekBarProgress.setMax(99); // It means 100% .0-99
        seekBarProgress.setOnTouchListener(this);

        player = new MediaPlayer();
        player.setOnBufferingUpdateListener(this);
    }

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
                if (currPos[0] == (numItemsInBucket - 1)) {
                    currPos[0] = 0;
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

                if (currPos[0] == 0) {
                    currPos[0] = (numItemsInBucket - 1);
                } else {
                    currPos[0]--;
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
            if (repeatSwiticher == 1) {
                repeatButton.setImageResource(R.drawable.onrepeat_icon);
                repeatSwiticher = 2;
            } else if (repeatSwiticher == 2) {
                repeatButton.setImageResource(R.drawable.offrepeat_icon);
                repeatSwiticher = 1;
            } else {
                repeatButton.setImageResource(R.drawable.offrepeat_icon);
            }
        }else if(v.getId() == R.id.downloadButton) {
            mode.setText("Download Mode");
            if (downloadLevel == 1){
                downloadActive = true;
                ListAdapter list = new CustomPlayView(getApplicationContext(), formatedFilesInBucket);
                playMusicView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                playMusicView.setAdapter(list);

                selectedDownloadedSong = new ArrayList<String>();
                downloadLevel = 2;
            }else{
                downloadProgress = new ProgressDialog(PlayMusicActivity.this);
                ProgressDialog progressDialog = new ProgressDialog(PlayMusicActivity.this);

                for (int i = 0; i < selectedDownloadedSong.size(); i++){
                    String currentFolder = currentDirectoryView.getText().toString().trim();
                    currentFolder = currentFolder.substring(0, currentFolder.length() - 1);

                    DownloadFile downloadFile = new DownloadFile(i, selectedDownloadedSong, getApplicationContext(),
                            progressDialog, bucketManager, currentFolder);

                    downloadFile.execute();
                }
            }
        }
    }

    /** Method which updates the SeekBar primary progress by current song playing position*/
    private void primarySeekBarProgressUpdater() {
        seekBarProgress.setProgress((int)(((float)player.getCurrentPosition()/mediaFileLengthInMilliseconds)*100));
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
                originFile = dir + fileName + "";
            }
        }
        String url = bucketManager.getFileURL(originFile) + "";

        if (url.endsWith("mp3")){
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
                        //Do Nothing
                    }
                }
            };

            ExecutorService executeT1 = Executors.newFixedThreadPool(1);
            executeT1.execute(r);
            executeT1.shutdownNow();
            while (!executeT1.isTerminated()){}
            player.start();

            mediaFileLengthInMilliseconds = player.getDuration();
            primarySeekBarProgressUpdater();
        }else{
            playMusicView.setAdapter(null);
            String[] directories = url.split(globalBucketName + ".s3.amazonaws.com");
            String curDirectory = directories[1];
            currentDirectoryView.setText("  /" + originFile);
            String delim = curDirectory.substring(1, curDirectory.length());
            //Toast.makeText(getApplicationContext(), url + " " + delim, Toast.LENGTH_LONG).show();
            new SpillBucketTask(globalBucketName, delim).execute();
        }
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        seekBarProgress.setSecondaryProgress(percent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        player.release();
        if (repeatSwiticher == 2){
            if (currPos[0] == (numItemsInBucket - 1)) {
                currPos[0] = 0;
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
        }else if (repeatSwiticher == 1){
            if (currPos[0] == (numItemsInBucket - 1)) {
                playPauseButton.setImageResource(R.drawable.play_icon);
                player = new MediaPlayer();
                player.setOnBufferingUpdateListener(this);

                playMusicView.clearChoices();
                playMusicView.requestLayout();
                playPause = "Play";
                currPos[0] = -1;
            } else {
                currPos[0]++;
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

        }
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
            int playPositionInMillisecconds = (mediaFileLengthInMilliseconds / 100) * sb.getProgress();
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
                } else {
                    selectedIndex = -1;
                    pullData.setVisibility(View.VISIBLE);
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



    class DownloadFile extends AsyncTask<String, Integer, Void> {
        //Define instance variables
        private int i;
        private ArrayList<String> checkedItems;
        private Context appContext;
        private ProgressDialog downloadProgress;
        private BucketManager bucketManager;
        private String currentFolder;

        /** Constructor defined to update the index of the loop we're on (depending on which
         * file download we're on).
         *
         * @param index
         */
        public DownloadFile(int index, ArrayList<String> checkedItems, Context context, ProgressDialog downloadProgress,
                            BucketManager bucketManager, String currentFolder){
            i = index;
            this.checkedItems = checkedItems;
            appContext = context;
            this.downloadProgress = downloadProgress;
            this.bucketManager = bucketManager;
            this.currentFolder = currentFolder;
        }

        /** Settings set before the download takes places, this sets up the properties for the
         *  download and the download updater.
         *
         */
        public void onPreExecute() {
            downloadProgress.setIndeterminate(true);
            downloadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            downloadProgress.setCancelable(false);
            downloadProgress.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            downloadProgress.setButton(DialogInterface.BUTTON_POSITIVE, "Okay!", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    downloadActive = false;
                    selectedDownloadedSong = new ArrayList<String>();
                    mode.setText("Music Mode");
                    downloadLevel = 0;

                    ListAdapter list = new CustomPlayView(appContext, formatedFilesInBucket);
                    playMusicView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    playMusicView.setAdapter(list);

                    dialog.dismiss();
                }
            });

            downloadProgress.setMessage("Downloading... (" + (i + 1) + "/" + checkedItems.size() + ")");
            downloadProgress.show();
        }

        /** Progress bar method that gets updated on each run.
         *
         * @param progress
         */
        public void onProgressUpdate(Integer... progress) {
            downloadProgress.setMessage("Downloading... (" + (i +1) + "/" + checkedItems.size() + ")");
            downloadProgress.setIndeterminate(false);
            downloadProgress.setMax(100);
            downloadProgress.setProgress(progress[0]);
        }

        /** Method to download the file.
         *
         * @param strings
         * @return
         */
        public Void doInBackground(String... strings) {
            if (checkedItems.size() != 0) {
                try {
                    //Toast.makeText(getApplicationContext(), "Download finished", Toast.LENGTH_LONG).show();
                    String file = checkedItems.get(i);

                    S3Object downloadedFile = bucketManager.spillBucket(currentFolder, file);
                    String downloadedFileName = file;
                    String downloadedFilePath = Environment.getExternalStorageDirectory() +
                            AmazonAccountKeys.getAppFolder() + "/" + downloadedFileName;

                    FileOutputStream downloadedFileStream = new FileOutputStream(downloadedFilePath);;
                    InputStream fileContentStream = downloadedFile.getObjectContent();;

                    //How much of file has been downloaded up to this point.
                    int receivedBytes = 0;
                    //The size of the file
                    long fileSize = downloadedFile.getObjectMetadata().getContentLength();
                    int chunkSize;
                    byte[] downloadBuffer = new byte[1024];
                    while ((chunkSize = fileContentStream.read(downloadBuffer)) > 0) {
                        downloadedFileStream.write(downloadBuffer, 0, chunkSize);
                        //More of the file has been downloaded so update the progressbar
                        receivedBytes += chunkSize;
                        //Convert to percent of file downloaded
                        publishProgress((int) (receivedBytes * 100 / fileSize));
                    }
                    downloadedFileStream.close();
                    fileContentStream.close();

                } catch (Exception e) {}
            }else{
                Toast.makeText(appContext, "No files selected.", Toast.LENGTH_LONG).show();
            }
            return null;
        }

        public void onPostExecute(ArrayList<String> filesInBucket) {

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
        private String bucketName;
        private String delim;

        public SpillBucketTask(String bucketName, String delim){
            this.bucketName = bucketName;
            this.delim = delim;
        }

        /** Method that runs when this task is executed. It lists the takes the objects of the
         *  bucket and lists the file names to have them placed in an Array.
         *
         * @param voids                         something...
         * @return                              return the list of items to be placed on the
         *                                      screen.
         */
        public ArrayList<String> doInBackground(Void... voids) {
            ArrayList<String> filesInBucket = null;
            try {
                if (delim.equals("")) {
                    filesInBucket = bucketManager.listObjectsInBucket(bucketName);
                }else{
                    filesInBucket = bucketManager.listObjectsInBucketWithDelim(bucketName, delim);
                }
            } catch (Exception e) {
                //Do nothing...
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
                String[] tok = filesInBucket.get(i).toString().split("/");

                if (!filesInBucket.get(i).toString().endsWith(".mp3") && tok.length == 1){
                    formatedFilesInBucket.add(filesInBucket.get(i));
                }
            }

            for (int i = 0; i < filesInBucket.size(); i++){
                if (filesInBucket.get(i).toString().endsWith(".mp3")){
                    formatedFilesInBucket.add(filesInBucket.get(i));
                }
            }

            ListAdapter list = new CustomPlayView(getApplicationContext(), formatedFilesInBucket);
            playMusicView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            playMusicView.setAdapter(list);
            numItemsInBucket = filesInBucket.size();
            itemToggle = new boolean[formatedFilesInBucket.size()];
            Arrays.fill(itemToggle, false);
            pullData.setVisibility(View.INVISIBLE);
        }
    }
}
