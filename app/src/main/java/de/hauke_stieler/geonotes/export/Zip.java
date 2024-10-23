package de.hauke_stieler.geonotes.export;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Zip {
    private static final int BUFFER_SIZE = 4096;

    public static void zip(List<File> inputFiles, File outputZipFile) {
        try {
            FileOutputStream fileOutput = new FileOutputStream(outputZipFile);
            ZipOutputStream zipOutput = new ZipOutputStream(new BufferedOutputStream(fileOutput));
            zipOutput.setLevel(Deflater.HUFFMAN_ONLY);
            byte[] buffer = new byte[BUFFER_SIZE];

            for (File file : inputFiles) {
                FileInputStream fileInput = new FileInputStream(file);
                BufferedInputStream bufferedFileInput = new BufferedInputStream(fileInput, BUFFER_SIZE);

                ZipEntry entry = new ZipEntry(file.getName());
                zipOutput.putNextEntry(entry);

                int count;
                while ((count = bufferedFileInput.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    zipOutput.write(buffer, 0, count);
                }
                bufferedFileInput.close();
            }

            zipOutput.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This file unzips the given file and assumes that there are <i>no</i> folder within this ZIP file.
     */
    public static void unzipFlatZip(InputStream zipFileInputStream, File outputFolder) throws IOException {
        if (!outputFolder.isDirectory()) {
            throw new IOException("Output directory " + outputFolder.getAbsolutePath() + " must be an existing directory.");
        }

        try {
            ZipInputStream zipInput = new ZipInputStream(zipFileInputStream);
            ZipEntry entry;
            byte[] buffer = new byte[BUFFER_SIZE];

            while ((entry = zipInput.getNextEntry()) != null) {
                BufferedOutputStream fileOutput = new BufferedOutputStream(new FileOutputStream(outputFolder.getAbsolutePath() + "/" + entry.getName()));

                int count;
                while ((count = zipInput.read(buffer, 0, BUFFER_SIZE)) >= 0) {
                    fileOutput.write(buffer, 0, count);
                }

                zipInput.closeEntry();
                fileOutput.close();
            }

            zipInput.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
