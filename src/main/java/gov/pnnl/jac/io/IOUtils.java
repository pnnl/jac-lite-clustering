package gov.pnnl.jac.io;

import gov.pnnl.jac.util.MemUtils;

import java.awt.Color;
import java.io.*;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

public final class IOUtils {

	private static Logger logger = Logger.getLogger(IOUtils.class);
	
	private IOUtils() {
	}

	/**
	 * Counts the number of line in a text file, optionally excluding those of length zero
	 * or composed entirely of whitespace.
	 * 
	 * @param textFile
	 * @param excludeBlank
	 * 
	 * @return number of lines.
	 * 
	 * @throws IOException
	 */
	public static int countLines(File textFile, boolean excludeBlank) throws IOException {
		
		int lineCount = 0;
		
		EncodingDetector ed = new EncodingDetector(textFile, true);
		String charset = ed.getEncoding();
		
		Reader br = null;
		
		try {
		
			br = new InputStreamReader(new FileInputStream(textFile), charset);
			char[] buffer = new char[65536];
			
			// Lines are terminated by "\n", "\r", or "\r\n".
			//
			int numRead;
			int charsOnLine = 0;
			boolean lastWasCR = false;
			
			while((numRead = br.read(buffer)) != -1) {
				for (int i=0; i<numRead; i++) {
					if (buffer[i] == '\r') {
						if (!excludeBlank || charsOnLine > 0) {
							lineCount++;
						}
						charsOnLine = 0;
						lastWasCR = true;
					} else if (buffer[i] == '\n') {
						if (!lastWasCR) {
							if (!excludeBlank || charsOnLine > 0) {
								lineCount++;
							}
						}
						charsOnLine = 0;
						lastWasCR = false;
					} else {
						if (!excludeBlank || !Character.isWhitespace(buffer[i])) {
							charsOnLine++;
						}
						lastWasCR = false;
					}
				}
			}
			
			if (charsOnLine > 0) {
				lineCount++;
			}
			
		} finally {
			
			if (br != null) {
				try {
					br.close();
				} catch (IOException ioe) {
					logger.error("error closing file: " + textFile.getAbsolutePath(), ioe);
				}
			}
			
		}
		
		return lineCount;
	}
	
	/**
	 * Counts the number of line in a file quickly, reading the file as a byte stream.
	 * Lines are delimited by newlines, carriage returns, or carriage returns immediately followed
	 * by newlines. Since the file contents are not decoded into characters, this method is much
	 * faster than <code>countLines(File textFile, boolean excludeBlank)</code>. However, it does not
	 * allow exclusion of lines consisting solely of white space.
	 * 
	 * @param f
	 * 
	 * @return number of lines.
	 * 
	 * @throws IOException
	 */
	public static int countLinesFast(File f) throws IOException {
		
		int lineCount = 0;
		
		InputStream in = null;
		
		try {
		
			in = new FileInputStream(f);
			byte[] buffer = new byte[65536];
			byte newline = '\n';
			byte carriageReturn = '\r';
			
			// Lines are terminated by "\n", "\r", or "\r\n".
			//
			int numRead;
			boolean hasTrailingChars = false;
			boolean lastWasCR = false;
			
			while((numRead = in.read(buffer)) != -1) {
				for (int i=0; i<numRead; i++) {
					byte b = buffer[i];
					if (b == carriageReturn || b == newline) {
						if (b == carriageReturn || !lastWasCR) {
							lineCount++;
						}
						lastWasCR = (b == carriageReturn);
					} else {
						lastWasCR = false;
					}
				}
				hasTrailingChars = (numRead > 0) && buffer[numRead - 1] != newline && buffer[numRead - 1] != carriageReturn;
			}
			
			if (hasTrailingChars) {
				lineCount++;
			}
			
		} finally {
			
			if (in != null) {
				try {
					in.close();
				} catch (IOException ioe) {
					logger.error("error closing file: " + f.getAbsolutePath(), ioe);
				}
			}
			
		}
		
		return lineCount;
	}

	/**
	 * Convenience method for loading text from a file.
	 * 
	 * @param f
	 * @param maxLength
	 * @return
	 * @throws IOException
	 */
	public static String loadTextFile(File f, int maxLength) throws IOException {
		
		if (maxLength <= 0) {
			maxLength = Integer.MAX_VALUE;
		}
		
		EncodingDetector ed = new EncodingDetector(f, true);
		String charset = ed.getEncoding();
		Reader fr = null;
		
		try {
		
			fr = new InputStreamReader(new FileInputStream(f), charset);
			
			char[] buffer = new char[Math.min(8192, maxLength)];
			
			StringBuilder sb = new StringBuilder();
		
			boolean eof = false;
			
			int leftToRead = maxLength;
			
			while(!eof && leftToRead > 0) {
				
				int readThisTime = Math.min(buffer.length, leftToRead);
				
				int read = fr.read(buffer, 0, readThisTime);
				
				if (read > 0) {
					leftToRead -= read;
					sb.append(buffer, 0, read);
				} else if (read == -1) {
					eof = true;
				}
				
			}
					
			return sb.toString();

		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
					logger.error("error closing InputStreamReader for file: " + f.getAbsolutePath(), e);
				}
			}
		}
	}
	
	public static void close(java.io.Closeable ... closeables) {
		for (int i=0; i<closeables.length; i++) {
			if (closeables[i] == null) continue;
			try {
				closeables[i].close();
			} catch (IOException ioe) {
				logger.error("error closing " + closeables[i], ioe);
			}
		}
	}
	
	public static void writeIntArray(DataOutput out, int[] arr)
			throws IOException {
		int n = arr != null ? arr.length : -1;
		out.writeInt(n);
		for (int i = 0; i < n; i++)
			out.writeInt(arr[i]);
	}

	public static int[] readIntArray(DataInput in) throws IOException {
		int[] rtn = null;
		int n = in.readInt();
		if (n >= 0) {
			rtn = new int[n];
			for (int i = 0; i < n; i++)
				rtn[i] = in.readInt();
		}
		return rtn;
	}

	public static void writeDoubleArray(DataOutput out, double[] arr)
			throws IOException {
		int n = arr != null ? arr.length : -1;
		out.writeInt(n);
		for (int i = 0; i < n; i++)
			out.writeDouble(arr[i]);
	}

	public static double[] readDoubleArray(DataInput in) throws IOException {
		double[] rtn = null;
		int n = in.readInt();
		if (n >= 0) {
			rtn = new double[n];
			for (int i = 0; i < n; i++)
				rtn[i] = in.readDouble();
		}
		return rtn;
	}

	public static void writeString(DataOutput out, String s) throws IOException {
		int len = s != null ? s.length() : -1;
		out.writeInt(len);
		if (len > 0) {
			for (int i = 0; i < len; i++) {
				out.writeChar(s.charAt(i));
			}
		}
	}

	public static String readString(DataInput in) throws IOException {
		int len = in.readInt();
		if (len >= 0) {
			StringBuffer sb = new StringBuffer(len);
			for (int i = 0; i < len; i++) {
				sb.append(in.readChar());
			}
			return sb.toString();
		}
		return null;
	}

	public static void skipString(DataInput in) throws IOException {
		int len = in.readInt();
		if (len >= 0) {
			in.skipBytes(2 * len);
		}
	}

	public static void writeColor(DataOutput out, Color c) throws IOException {
		// note: -1 is the color white
		int rgb = c != null ? c.getRGB() : -1;
		out.writeInt(rgb);
	}

	public static Color readColor(DataInput in) throws IOException {
		int rgb = in.readInt();
		// note: -1 is the color white
		return new Color(rgb);
	}

	public static void copy(File source, File dest) throws IOException {

		long len = source.length();
		
		// Limit the number of bytes transfered at a time.  If the file is very large
		// transferring it all at once might cause an OutOfMemoryError.
		//
		// Make nBytes the smaller of: the file length, half the maximum free heap size, 100 MBytes.
		//
		// (The 100MB limit is to keep the copying of one inordinately large file from causing the jvm to 
		//  balloon the memory usage of the program.  100MB should be enough for a quick copy.)
		//
		long nBytes = len;
		
		// But only worry about checking the heap if len is greater than 16mbyte, since
		// getting the heap memory is a fairly expensive operation.
		if (len > 16777216L) {
			nBytes = Math.min(len, Math.min(104857600L, MemUtils.maximumFreeHeap()/2L));
		}

		FileInputStream in = null;
		FileChannel inChannel = null;
		FileOutputStream out = null;
		FileChannel outChannel = null;

		IOException ioe = null;

		try {

			in = new FileInputStream(source);
			out = new FileOutputStream(dest);
			inChannel = in.getChannel();
			outChannel = out.getChannel();
			
			long  position = 0L;
			
			while( position < len ) {
				position += inChannel.transferTo(position, nBytes, outChannel);
			}

		} catch (IOException e) {

			ioe = e;

		} finally {

			if (inChannel != null) {
				try {
					inChannel.close();
				} catch (IOException e2) {
					if (ioe == null) {
						ioe = e2;
					}
				}
			} else if (in != null) {
				try {
					in.close();
				} catch (IOException e2) {
					if (ioe == null) {
						ioe = e2;
					}
				}
			}

			if (outChannel != null) {
				try {
					outChannel.close();
				} catch (IOException e2) {
					if (ioe == null) {
						ioe = e2;
					}
				}
			} else if (out != null) {
				try {
					out.close();
				} catch (IOException e2) {
					if (ioe == null) {
						ioe = e2;
					}
				}
			}

		}

		if (ioe != null)
			throw ioe;
	}

}
