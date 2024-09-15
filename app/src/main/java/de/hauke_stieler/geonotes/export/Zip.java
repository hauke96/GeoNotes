package de.hauke_stieler.geonotes.export;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Zip {
    private static final int BUFFER = 4096;

    public static void zip(List<File> inputFiles, File outputZipFile) {
        try {
            FileOutputStream fileOutput = new FileOutputStream(outputZipFile);
            ZipOutputStream zipOutput = new ZipOutputStream(new BufferedOutputStream(fileOutput));
            byte data[] = new byte[BUFFER];

            for (File file : inputFiles) {
                FileInputStream fileInput = new FileInputStream(file);
                BufferedInputStream bufferedFileInput = new BufferedInputStream(fileInput, BUFFER);

                ZipEntry entry = new ZipEntry(file.getName());
                zipOutput.putNextEntry(entry);
                int count;

                while ((count = bufferedFileInput.read(data, 0, BUFFER)) != -1) {
                    zipOutput.write(data, 0, count);
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
    public void unzipFlatZip(File zipFile, File outputFolder) throws IOException {
        if (!outputFolder.isDirectory()) {
            throw new IOException("Output directory " + outputFolder.getAbsolutePath() + " must be an existing directory.");
        }

        try {
            ZipInputStream zipInput = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry entry;

            while ((entry = zipInput.getNextEntry()) != null) {
                FileOutputStream fileOutput = new FileOutputStream(outputFolder.getAbsolutePath() + "/" + entry.getName());
                for (int c = zipInput.read(); c != -1; c = zipInput.read()) {
                    fileOutput.write(c);
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
