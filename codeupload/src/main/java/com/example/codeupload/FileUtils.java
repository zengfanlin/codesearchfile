package com.example.codeupload;

import org.springframework.boot.system.ApplicationHome;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedList;

public class FileUtils {
    /**
     * 非递归遍历
     *
     * @param path
     * @return
     */
    public static LinkedList<File> GetDirectory(String path) {
        File file = new File(path);
        LinkedList<File> Dirlist = new LinkedList<File>(); // 保存待遍历文件夹的列表
        LinkedList<File> fileList = new LinkedList<File>();
        GetOneDir(file, Dirlist, fileList);// 调用遍历文件夹根目录文件的方法
        File tmp;
        while (!Dirlist.isEmpty()) {
            tmp = (File) Dirlist.removeFirst();// 从文件夹列表中删除第一个文件夹，并返回该文件夹赋给tmp变量
            // 遍历这个文件夹下的所有文件，并把
            GetOneDir(tmp, Dirlist, fileList);
        }
        return fileList;
    }

    // 遍历指定文件夹根目录下的文件
    private static void GetOneDir(File file,
                                  LinkedList<File> Dirlist,
                                  LinkedList<File> fileList) {
        // 每个文件夹遍历都会调用该方法
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                Dirlist.add(f);
            } else {
                // 这里列出当前文件夹根目录下的所有文件,并添加到fileList列表中
                fileList.add(f);
                // System.out.println("file==>" + f);
            }
        }


    }
}
