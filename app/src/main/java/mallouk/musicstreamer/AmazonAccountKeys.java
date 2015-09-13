package mallouk.musicstreamer;

/**
 * Created by Matthew Jallouk on 2/26/2015.
 */
public class AmazonAccountKeys {

    //Defined instance variables
    private static String publicKey = "";
    private static String privateKey = "";

    private static String keyFile = "/.keys";

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

    /** Method that returns the hardcoded key file to access the accounts
     *  of the users on the device.
     *
     * @return                      returns the key file name.
     */
    public static String getKeyFileName(){
        return keyFile;
    }
}
