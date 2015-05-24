package gov.pnnl.jac.projection;

import java.io.*;

/**
 * Contains utility methods for serialization and deserialization of
 * <code>ProjectionData</code> objects.
 * 
 * @author D3J923
 *
 */
public final class ProjectionDataSerializer {

    private static final int SERIAL_VERSION = 1;
    
    private ProjectionDataSerializer() {}
    
    /**
     * Saves the given projection information to a file
     * in binary format.
     * 
     * @param pd
     * @param file
     * @throws IOException
     */
    public static void save(ProjectionData pd, File file) throws IOException {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(file)));
            save(pd, dos);
            dos.flush();
            dos.close();
            dos = null;
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    // Ignore -- don't want to mask the other problem.
                }
            }
        }
    }
    
    /**
     * Saves the projection data to the given data output in binary
     * format.
     * 
     * @param pd
     * @param out
     * @throws IOException
     */
    public static void save(ProjectionData pd, DataOutput out) throws IOException {
        out.writeInt(SERIAL_VERSION);
        int projCount = pd.getProjectionCount();
        int dim = pd.getDimensionCount();
        out.writeInt(projCount);
        out.writeInt(dim);
        for (int i=0; i<dim; i++) {
            out.writeFloat(pd.getMinAllowed(i));
            out.writeFloat(pd.getMaxAllowed(i));
        }
        float[] buffer = new float[dim];
        for (int i=0; i<projCount; i++) {
            pd.getProjection(i, buffer);
            for (int j=0; j<dim; j++) {
                out.writeFloat(buffer[j]);
            }
        }
    }
    
    /**
     * The reverse of <code>load(ProjectionData pd, File file)</code>. 
     * Returns a projection data object instantiated from data in
     * the specified file.
     * 
     * @param file
     * @return
     * @throws IOException
     */
    public static ProjectionData load(File file) throws IOException {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(file)));
            ProjectionData pd = load(dis);
            dis.close();
            dis = null;
            return pd;
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Instantiates a projection data object from binary data
     * read from the given input.
     * 
     * @param in
     * @return
     * @throws IOException
     */
    public static ProjectionData load(DataInput in) throws IOException {
        int serialVersion = in.readInt();
        if (serialVersion == 1) {
            int projCount = in.readInt();
            int dim = in.readInt();
            float[] minAllowed = new float[dim];
            float[] maxAllowed = new float[dim];
            for (int i=0; i<dim; i++) {
                minAllowed[i] = in.readFloat();
                maxAllowed[i] = in.readFloat();
            }
            try {
                // Currently only supports SimpleProjectionData.
                // May need to threshold this and return a 
                // different class that doesn't store all its data in memory
                // if it exceeds the threshold.
                SimpleProjectionData pd = new SimpleProjectionData(
                    projCount, dim, minAllowed, maxAllowed);
                float[] buffer = new float[dim];
                for (int i=0; i<projCount; i++) {
                    for (int j=0; j<dim; j++) {
                        buffer[j] = in.readFloat();
                    }
                    pd.setProjection(i, buffer);
                }
                return pd;
            } catch (IllegalArgumentException e) {
                throw new IOException("invalid projection data: " + e.getMessage());
            }
        } else {
            throw new IOException("unrecognized serial version: " + serialVersion);
        }
    }
}
