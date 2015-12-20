package mallouk.musicstreamer;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.amazonaws.services.s3.model.S3Object;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

/** This class is used by the listeners to download the files to the target device. It uses
 *  the AsyncTask to do this.
 *
 */
public class DownloadFile extends AsyncTask<String, Integer, Void> {
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
        downloadProgress.setCancelable(true);
        downloadProgress.setMessage("Downloading... (" + (i+1) + "/" + checkedItems.size() + ")");
        downloadProgress.show();
    }

    /** Progress bar method that gets updated on each run.
     *
     * @param progress
     */
    public void onProgressUpdate(Integer... progress) {
        downloadProgress.setMessage("Downloading... (" + (i+1) + "/" + checkedItems.size() + ")");
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
                String downloadedFilePath = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS) + "/" + downloadedFileName;

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