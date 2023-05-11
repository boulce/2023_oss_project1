package com.github.filemanager;

public class OsUtils {
    public static String getOsType() { // 운영체제 종류 판단 : Windows, Mac
        String os = System.getProperty("os.name").toLowerCase();
        String result = "";
        if (os.contains("win")) {
            result = "Windows";

        } else if (os.contains("mac")) {
            result = "Mac";
        }
        return result;
    }

    public static String getAbsolutePathByOs(String path){
        String result = "";
        if(getOsType() == "Windows"){
            result = path.replace("/", "\\");
        }
        else if(getOsType() == "Mac"){
            result = path.replace("\\", "/");
        }
        return result;
    }
}