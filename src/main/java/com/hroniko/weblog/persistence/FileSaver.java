package com.hroniko.weblog.persistence;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileSaver {

    public static Boolean writeToFS(String fullPath, String message) {
        try {
            File targetFile = new File(fullPath);
            if (!targetFile.exists()) {
                FileUtils.touch(targetFile);
            }
            FileUtils.writeStringToFile(targetFile, message + "\n", StandardCharsets.UTF_8, true);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
