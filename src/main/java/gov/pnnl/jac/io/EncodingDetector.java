package gov.pnnl.jac.io;

import gov.pnnl.jac.util.DataConverter;

import java.io.*;
import java.nio.charset.Charset;

/**
 * <p>Used for quickly detecting the name of the character set for text
 * streams.  This is very similar to the class <tt>FileEncodingFinder</tt> from
 * the IN-SPIRE code base.</p>
 *
 * @author Nick Cramer, R. Scarberry (I stole from IN-SPIRE, then made some changes.)
 * @version 1.0
 */
public class EncodingDetector {

    public static final int ASCII       = 0;
    public static final int CP1252      = 0;
    public static final int UTF8        = 1;
    public static final int U16BE       = 2;
    public static final int U16LE       = 3;
    public static final int U32BE       = 4;
    public static final int U32LE       = 5;
    
    private static String[] mEncodingNames = {                             
        "Cp1252",               // Same as ASCII
        "UTF-8", 
        "UnicodeBig",
        "UnicodeLittle",
        "UTF-32BE",
        "UTF-32LE",
    };

    private String mDefaultEncoding;
    
    private String mEncoding;
    private int mBOMLen;
    
    public EncodingDetector(InputStream is) throws IOException {
        this(is, false);
    }
    
    public EncodingDetector(File f) throws IOException {
        this(f, false);
    }
    
    public EncodingDetector(InputStream is, boolean accountForEmptyBOM) throws IOException {
        this(is, "Cp1252", accountForEmptyBOM);
    }
    
    public EncodingDetector(File f, boolean accountForEmptyBOM) throws IOException {
        this(new FileInputStream(f), "Cp1252", accountForEmptyBOM);
    }

    public EncodingDetector(File f, String defaultEncoding, boolean accountForEmptyBOM) throws IOException {
        this(new FileInputStream(f), defaultEncoding, accountForEmptyBOM);
    }

    public EncodingDetector(InputStream is, String defaultEncoding, 
            boolean accountForEmptyBOM) throws IOException {
    	mDefaultEncoding = defaultEncoding;
    	analyze(is, accountForEmptyBOM);
    }
    
    public EncodingDetector(File f, String defaultEncoding) throws IOException {
        this(new FileInputStream(f), defaultEncoding, false);
    }

    public String getEncoding() {
        return mEncoding;
    }
    
    public int getEncodingIndex() {
        for (int i=0; i<mEncodingNames.length; i++) {
            if (mEncodingNames[i].equals(mEncoding)) {
                return i;
            }
        }
        return ASCII;
    }
    
    public static String encodingName(int encodingIndex) {
        return mEncodingNames[encodingIndex];
    }
    
    public int getBOMLength() {
        return mBOMLen;
    }
    
    public static int encodingIndex(String encoding) {
    	for (int i=0; i<mEncodingNames.length; i++) {
    		if (mEncodingNames[i].equalsIgnoreCase(encoding)) {
    			return i;
    		}
    	}
    	return -1;
    }
    
    public static byte[] bomFor(String encoding) throws UnsupportedEncodingException {
    	int enc = encodingIndex(encoding);
    	byte[] bom = null;
    	switch(enc) {
    	case UTF8:
    		bom = new byte[] {
    				(byte) 0xEF,
    				(byte) 0xBB,
    				(byte) 0xBF
    		};
    		break;
    	case U16BE:
    		bom = new byte[] {
    				(byte) 0xFE, (byte) 0xFF
    		};
    		break;
    	case U16LE:
    		bom = new byte[] {
    				(byte) 0xFF, (byte) 0xFE
    		};
    		break;
    	case U32BE:
    		bom = new byte[] {
    				(byte) 0x00,
	    			(byte) 0x00,
	    			(byte) 0xFE,
	    			(byte) 0xFF
    		};
    		break;
    	case U32LE:
    		bom = new byte[] {
    				(byte) 0xFF,
    				(byte) 0xFE,
    				(byte) 0x00,
    				(byte) 0x00
    		};
    		break;
    	default:
    		bom = new byte[0];
    	}
    	
    	return bom;
    }
    
    private void analyze(InputStream is, boolean accountForEmptyBOM) throws IOException {

        int encoding = ASCII; // ASCII
        int bomLen = 0;
        boolean found = false;
        
        try {

        	// Most we'll ever need.
        	byte[] bytes = new byte[4];
        	int bytesRead = is.read(bytes);

        	// Convert to ints, so the comparisons work.
        	int byte0 = 0x000000ff & ((int) bytes[0]);
        	int byte1 = 0x000000ff & ((int) bytes[1]);
        	int byte2 = 0x000000ff & ((int) bytes[2]);
        	int byte3 = 0x000000ff & ((int) bytes[3]);
        	
        	if (byte0 == 0xFE &&
                byte1 == 0xFF) {
                // UCS-2 Big Endian
                encoding = U16BE;
                bomLen = 2;
                found = true;
        	} else if (byte0 == 0xFF &&
            		 byte1 == 0xFE) {
                // UCS-2 Little Endian
            	 encoding = U16LE;
            	 bomLen = 2;
                 found = true;
             } else if (
                    byte0 == 0xEF &&
                    byte1 == 0xBB &&
                    byte2 == 0xBF) {
                    // UTF-8
                    encoding = UTF8;
                    bomLen = 3;
                    found = true;
             } else if (byte0 == 0x00 && byte1 == 0x00 &&
            		 byte2 == 0xFE && byte3 == 0xFF) {
            	 encoding = U32BE;
            	 bomLen = 4;
                 found = true;
             } else if (byte0 == 0xFF && byte1 == 0xFE && bytesRead == 4 &&
            		 byte2 == 0x00 && byte3 == 0x00) {
            
                 encoding = U32LE;
            	 bomLen = 4;
                 found = true;

             } else if (accountForEmptyBOM) { // No BOM or BOM unrecognized 

                 String[] encodingsToTry = new String[1 + mEncodingNames.length];
                 // Give the default first crack.
                 encodingsToTry[0] = mDefaultEncoding;
                 for (int i=0; i<mEncodingNames.length; i++) {
                     String enc = mEncodingNames[i];
                     if (!enc.equals(mDefaultEncoding)) {
                         encodingsToTry[i+1] = enc;
                     }
                 }
                 
                 for (int i=0; i<encodingsToTry.length; i++) {
                     
                     String enc = encodingsToTry[i];
                     if (enc == null) continue;
                     
                     ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                     InputStreamReader isr = new InputStreamReader(bais, enc);
                     
                     char[] buffer = new char[bytes.length];
                     int charsRead = isr.read(buffer);
                     
                     boolean charsOk = true;
                     for (int j=0; j<charsRead; j++) {
                         int c = (int) buffer[j];
                         if (c == 0 || c > 255) {
                             charsOk = false;
                             break;
                         }
                     }
                     
                     if (charsOk) {
                         mEncoding = enc;
                         mBOMLen = 0;
                         break;
                     }
                 }
                     
             }
            
        } finally {

        	if (found) {
        		mEncoding = mEncodingNames[encoding];
        		mBOMLen = bomLen;
        	} else if (mEncoding == null) {
        		mEncoding = mDefaultEncoding;
        	}
            
            if (is != null) {
                try {
                    is.close(); 
                } catch (IOException ioe) {   
                }
            }
        }
    }
}
