package mallouk.musicstreamer;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mallouk.bucketTypes.AmazonS3Manager;

/**
 * Created by Matthew Jallouk on 2/2/2015.
 */

public class BucketManager implements Serializable {

    //Instance variables to be used by the BucketManager class.
    private AmazonS3Manager amazonS3Manager;
    private String bucketName;

    /** Constructor that defines the initial properties of this class object.
     *
     * @param bucketName            name of the bucket that is created and managed.
     */
    public BucketManager(String bucketName){
        this.bucketName = bucketName;
        this.amazonS3Manager = new AmazonS3Manager();
    }

    /** Method that uses the AWS API to create a bucket. We do this by calling methods from the
     *  AmazonS3Manager class.
     *
     */
    public void createBucket() {
        this.amazonS3Manager.createBucket(bucketName);
    }

    /** Method that uses the AWS API to delete a bucket (provided that it is empty). We do this by
     *  calling methods from the AmazonS3Manager class.
     *
     */
    public void destroyBucket() {
        this.amazonS3Manager.deleteBucket(bucketName);
    }

    /** Method that deletes a specific file from a particular bucket. We do this by calling methods
     *  from the AmazonS3Manager class.
     *
     * @param file              name of the file that will be deleted from a specific bucket.
     */
    public void deleteObjectInBucket(String file){
        this.amazonS3Manager.deleteObjectInBucket(this.bucketName, file);
    }

    /** Method that checks if a bucket exists on the server side. We do this by using the AWS API
     *  and calling methods from the AmazonS3Manager class.
     *
     * @return                  returns true/false whether or not the bucket exists.
     */
    public boolean doesBucketExist(){
        return amazonS3Manager.bucketExist(bucketName);
    }

    /** Method that places an object into a particular bucket (based upon the param). This is
     *  done by interfacing ith the AmazonS3Manager class.
     *
     * @param file              file that gets placed into a particular bucket.
     */
    public void fillBucket(File file) {
        this.amazonS3Manager.putObjectInBucket(this.bucketName, file);
    }

    /** Method that allows a user to obtain the particular object file of an object stored
     *  in a certain bucket. We do this by calling methods from the AmazonS3Manager class.
     *
     * @param file              name of the file that will be used to obtain the S3Object
     * @return                  returns the S3Object instance of the param filename.
     */
    public S3Object spillBucket(String currentFolder, String file){
        return this.amazonS3Manager.getObjectInBucket(this.bucketName + currentFolder, file);
    }

    /** Method that lists the objects in a particular bucket.
     *
     * @param bucketName        name of the bucket that will dump all of its contents.
     * @return                  returns all of the objects from the bucket name param.
     */
    public ArrayList<String> listObjectsInBucket(String bucketName){
        return this.amazonS3Manager.listObjectsInBucket(bucketName);
    }

    public ArrayList<String> listObjectsInBucketWithDelim(String bucketName, String delim){
        return this.amazonS3Manager.listObjectsInBucketWithDelim(bucketName, delim);
    }

    /** Method that lists the names of the buckets that are tied to the specific AWS
     *  account associated with the keys hardcoded above. We do this by calling methods
     *  from the AmazonS3Manager class.
     *
     * @return                  returns a list of bucketNames
     */
    public List<Bucket> listBuckets(){
        return amazonS3Manager.listBuckets();
    }

    /** Method that checks to see if a bucket is empty. We do this by calling methods from
     *  the AmazonS3Manager class.
     *
     * @return                  returns true/false of whether or not the bucket is empty.
     */
    public boolean isBucketEmpty(){
        return amazonS3Manager.isBucketEmpty(this.bucketName);
    }

    /** Method that sets the name/rename of the bucket to be managed
     *
     * @param bucketName            new name of the bucket
     */
    public void setBucketName(String bucketName){
        this.bucketName = bucketName;
    }

    /** Method that obtains the current name of the bucket from the BucketManager object.
     *
     * @return                  returns a string of the bucket name.
     */
    public String getBucketName(){
        return bucketName;
    }

    public URL getFileURL(String file){
        return amazonS3Manager.getFileURL(bucketName, file);
    }

}