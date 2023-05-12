package com.github.filemanager;

import java.io.File;

class GitUtilsForTrack {
    public static boolean isGitRepository(File directory) {
        while (directory != null) {
            File gitDir = new File(directory, ".git");
            if (gitDir.exists()) {
                return true;
            }
            directory = directory.getParentFile();
        }
        return false;
    }
    public static File getGitRepository(File directory) {
        while (directory != null) {
            File gitDir = new File(directory, ".git");
            if (gitDir.exists()) {
                return gitDir;
            }
            directory = directory.getParentFile();
        }
        return null;
    }


}