package com.example.codeupload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;

@SpringBootApplication
public class CodeuploadApplication {

    public static void main(String[] args) throws FileNotFoundException {
        SpringApplication.run(CodeuploadApplication.class, args);
        Print();
    }

    private static void Print() throws FileNotFoundException {
        //第五种
        ApplicationHome h = new ApplicationHome(CodeuploadApplication.class);
        File jarF = h.getSource();
        System.out.println(jarF.getParentFile().toString());
    }
}
