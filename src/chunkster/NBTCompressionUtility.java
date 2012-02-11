package chunkster;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NBTCompressionUtility {

    public static NBTTagCompound readGzippedCompoundFromInputStream(
            InputStream inputstream) throws IOException {
        DataInputStream datainputstream = new DataInputStream(
                new GZIPInputStream(inputstream));
        try {
            NBTTagCompound nbttagcompound = readRootTagCompound(datainputstream);
            return nbttagcompound;
        } finally {
            datainputstream.close();
        }
    }

    public static void writeGzippedCompoundToOutputStream(
            NBTTagCompound nbttagcompound, OutputStream outputstream)
            throws IOException {
        DataOutputStream dataoutputstream = new DataOutputStream(
                new GZIPOutputStream(outputstream));
        try {
            writeRootTagCompound(nbttagcompound, dataoutputstream);
        } finally {
            dataoutputstream.close();
        }
    }

    public static NBTTagCompound readRootTagCompound(DataInput datainput)
            throws IOException {
        NBTBase nbtbase = NBTBase.readTag(datainput);
        if (nbtbase instanceof NBTTagCompound) {
            return (NBTTagCompound) nbtbase;
        } else {
            throw new IOException("Root tag must be a named compound tag");
        }
    }

    public static void writeRootTagCompound(NBTTagCompound nbttagcompound,
            DataOutput dataoutput) throws IOException {
        NBTBase.writeTag(nbttagcompound, dataoutput);
    }
}
