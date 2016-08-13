package mallouk.musicstreamer;

/**
 * Created by Matthew Jallouk on 2/26/2015.
 */
public class AmazonAccountKeys {

    //Defined instance variables
    private static String publicKey = "";
    private static String privateKey = "";
    private static String appFolder = "/MusicStreamer";

    /** Constructor that defines initial properties
     *
     */
    public AmazonAccountKeys(){}

    /** Method that returns hardcoded public key to the AWS account.
     *
     * @return                      returns public key.
     */
    public static String getPublicKey(){
        return publicKey;
    }

    /** Method that returns the hardcoded private key to the AWS account.
     *
     * @return                      returns private key.
     */
    public static String getPrivateKey(){
        return privateKey;
    }

    /** Method that returns the hardcoded app folder name on the device
     *
     * @return                      returns the app folder name.
     */
    public static String getAppFolder(){
        return appFolder;
    }
}
