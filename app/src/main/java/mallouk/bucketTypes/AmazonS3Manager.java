package mallouk.bucketTypes;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mallouk.musicstreamer.AmazonAccountKeys;

/**
 * Created by Matthew Jallouk on 2/1/2015.
 */
public class AmazonS3Manager implements Serializable {

    /* Hard coded public/private keys from Amazon. Can be obtained by logging into
    *  amazon account and going to "Security Credentials".
    *  Rest of private variables defined here.
    */
    private String amazonAccessKeyID = AmazonAccountKeys.getPublicKey();
    private String amazonPrivateKey = AmazonAccountKeys.getPrivateKey();
    private AmazonS3Client amazonS3Client;

    /** Constructor that defines the initial properties of the AmazonS3Manager object.
     *
     */
    public AmazonS3Manager(){
        amazonS3Client = new AmazonS3Client(new BasicAWSCredentials(amazonAccessKeyID, amazonPrivateKey));
        amazonS3Client.setRegion(Region.getRegion(Regions.US_EAST_1));
    }

    /** Method that creates a bucket on the AWS account associated with the hardcoded keys above.
     *
     * @param bucketName        name of the bucket that will be created.
     */
    public void createBucket(String bucketName){
        amazonS3Client.createBucket(bucketName);
    }

    /** Method that deletes a bucket on the AWS account associated with the hardcoded keys above.
     *
     * @param bucketName        name of the bucket that will be destroyed.
     */
    public void deleteBucket(String bucketName){
        amazonS3Client.deleteBucket(bucketName);
    }

    /** Method that checks if a bucket exists on the AWS account or not, it returns true/false
     *  based upon if the bucket does exist.
     *
     * @param bucketName        name of the bucket that is checked whether or not it exists.
     * @return                  returns true/false on if the bucket exists.
     */
    public boolean bucketExist(String bucketName){
        return amazonS3Client.doesBucketExist(bucketName);
    }

    /** Method that places a file into a particular bucket associated with the AWS keys above.
     *
     * @param bucketName        name of the bucket that will hold the uploaded file.
     * @param file              file that will uploaded to the AWS bucket.
     */
    public void putObjectInBucket(String bucketName, File file) {
        PutObjectRequest object = new PutObjectRequest(bucketName,
                file.getName(),
                file);
        amazonS3Client.putObject(object);
    }

    /** Method that obtains a particular file object from a specific bucket and downloads
     *  this object to the user's device.
     *
     * @param bucketName        name of the bucket that contains the file to be downloaded
     * @param fileName          name of the file to be downloaded to the user's device
     * @return                  returns the s3Object key of the specific file from the bucket
     */
    public S3Object getObjectInBucket(String bucketName, String fileName){
        return amazonS3Client.getObject(new GetObjectRequest(bucketName, fileName));
    }

    /** Method that deletes an object from a specific bucket.
     *
     * @param bucketName        name of the bucket that contains the file to be deleted
     * @param fileName          name of the file to be deleted
     */
    public void deleteObjectInBucket(String bucketName, String fileName){
        amazonS3Client.deleteObject(bucketName, fileName);
    }

    /** Method that lists the objects in a particular bucket.
     *
     * @param bucketName        name of the bucket from where all the objects are contained
     * @return                  returns the name of objects from the bucketName param.
     */
    public ArrayList<String> listObjectsInBucket(String bucketName){
        ArrayList<String> fileNamesList = new ArrayList<String>();
        for(S3ObjectSummary file : amazonS3Client.listObjects(bucketName).getObjectSummaries()) {
            fileNamesList.add(file.getKey());
        }
        return fileNamesList;

        /*ArrayList<String> filesInBucket = new ArrayList<String>();
        for (int i = 0; i < amazonS3Client.listObjects(bucketName).getObjectSummaries().size(); i++){
            S3ObjectSummary fileObject = amazonS3Client.listObjects(bucketName).getObjectSummaries().get(i);
            filesInBucket.add(fileObject.getKey());
        }
        return filesInBucket;*/
    }

    /** Method that lists the buckets that are associated with the various AWS keys hardcoded above
     *
     * @return                  returns the list of bucket names
     */
    public List<Bucket> listBuckets(){
        return amazonS3Client.listBuckets();
    }

    /** Method that determines if a bucket is empty. We do this by getting the number of objects
     *  from that bucket, if that arraylist of bucketName objects is empyty, then we know in fact
     *  if a bucket is empty or not.
     *
     * @param bucketName        name of the bucket to be checked
     * @return                  returns true/false if the bucket is empty.
     */
    public boolean isBucketEmpty(String bucketName){
        ArrayList<String> fileNamesList = new ArrayList<String>();
        for (int i = 0; i < amazonS3Client.listObjects(bucketName).getObjectSummaries().size(); i++){
        //for(S3ObjectSummary file : amazonS3Client.listObjects(bucketName).getObjectSummaries()) {
            S3ObjectSummary file = amazonS3Client.listObjects(bucketName).getObjectSummaries().get(i);
            fileNamesList.add(file.getKey());
        }
        if (fileNamesList.isEmpty()){
            return true;
        }else{
            return false;
        }
    }

    /** Method that gets the URL for the music file
     *
     * @param bucketName            bucketName where the file resides
     * @param fileName              file that we're fetching
     * @return                      get url object of the URL of the song.
     */
    public URL getFileURL(String bucketName, String fileName){
        return amazonS3Client.getUrl(bucketName, fileName);
        //test
    }
}
