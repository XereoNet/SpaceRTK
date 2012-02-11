package chunkster;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author Xeon
 */
public class Chunkster {

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            exitUsage();
        }

        File worldDir = new File(args[1]);

        if (!worldDir.exists() || !worldDir.isDirectory()) {
            exit("error: " + worldDir.getPath() + " is not a directory");
        }

        scan(worldDir);
    }

    public static void scan(File worldDir) {
        File regionDir = new File(worldDir, "region");

        if (!regionDir.exists()) {
            exit("error: region directory not found");
        }

        debug("Starting scan...");

        Pattern regionFilePattern = Pattern
                .compile("r\\.(-?[0-9]+)\\.(-?[0-9]+).mcr");
        Matcher match;
        for (File file : regionDir.listFiles()) {
            if (!file.isFile()) {
                continue;
            }

            match = regionFilePattern.matcher(file.getName());
            if (match.matches()) {
                scanRegionPointers(file, match);
            }
        }
    }

    private static void scanRegionPointers(File file, Matcher match) {
        RegionFile region = new RegionFile(file);
        String name = file.getName();

        int regionX = Integer.parseInt(match.group(1));
        int regionZ = Integer.parseInt(match.group(2));

        debug("Scanning region file x=" + regionX + " z=" + regionZ + " file="
                + name);

        for (int x = 0; x < 32; ++x) {
            for (int z = 0; z < 32; ++z) {
                int chunkX = x + (regionX << 5);
                int chunkZ = z + (regionZ << 5);

                if (region.getOffset(x, z) == 0)
                    continue;

                DataInputStream chunkInputStream = region
                        .getChunkDataInputStream(x, z);
                if (chunkInputStream == null) {
                    continue;
                }

                NBTTagCompound rootTag;
                if (chunkInputStream != null) {
                    try {
                        rootTag = NBTCompressionUtility
                                .readRootTagCompound(chunkInputStream);
                        if (!rootTag.hasKey("Level")) {
                            debug((new StringBuilder())
                                    .append("Chunk file at ").append(chunkX)
                                    .append(",").append(chunkZ)
                                    .append(" is missing level data, skipping")
                                    .toString());
                        } else {
                            NBTTagCompound levelTag = rootTag
                                    .getCompoundTag("Level");
                            int xPos = levelTag.getInteger("xPos");
                            int zPos = levelTag.getInteger("zPos");
                            if (xPos != chunkX || zPos != chunkZ) {
                                debug("Bad chunk pointer found! expected chunk at [x="
                                        + chunkX
                                        + " z="
                                        + chunkZ
                                        + "] got [x="
                                        + xPos + " z=" + zPos + "]");

                                // lets see if the correct pointer points to
                                // this chunk and fix that
                                int currentOffset = region
                                        .getOffset(xPos - (regionX << 5), zPos
                                                - (regionZ << 5));
                                if (currentOffset != region.getOffset(x, z)) {
                                    debug("Chunk pointer for [x=" + xPos
                                            + " z=" + zPos
                                            + "] was incorrect, fixing.");
                                    region.setOffset(xPos - (regionX << 5),
                                            zPos - (regionZ << 5),
                                            region.getOffset(x, z));
                                }

                                debug("Setting bad chunk pointer to null (it should regenerate a new chunk in its place).");
                                region.setOffset(x, z, 0);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        try {
            region.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void debug(String in) {
        System.out.println("[Chunkster] " + in);
    }

    private static void exitUsage() {
        exit("chunkster: region pointer repair utility v0.1\n"
                + "usage: java -jar Chunkster.jar <world directory>\n");

    }

    private static void exit(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
