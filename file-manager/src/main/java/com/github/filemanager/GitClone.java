package com.github.filemanager;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.util.HashMap;
import java.io.BufferedReader;

public class GitClone {
        String repoUrl;
        String localPath;
        String accessToken;
        HashMap<String,String> dict;

    public GitClone(String repoUrl,String localPath, String accessToken) {
        this.repoUrl=repoUrl;
        this.localPath=localPath;
        this.accessToken=accessToken;
        dict=new HashMap<>();
        cloneRepoPrivate(repoUrl,localPath,accessToken);
    }


    public  String readTokenFromFile() throws IOException { //access token이 둘이상의 줄이면 반복문으로 읽어야함, 현재는 token이 한줄로 있다고 가정
        BufferedReader reader = new BufferedReader(new FileReader("token.txt"));
        String token = reader.readLine();
        reader.close();
        return token;

    }

    public String readTokenFromFileHash(String ID) throws IOException {
        String key = null;
        try{
            BufferedReader br=new BufferedReader(new FileReader(localPath));
            String line;
            while((line=br.readLine())!=null) {
                String[] parts=line.split(" ");
                if(parts.length==2) {
                    key=parts[0];
                    String value = parts[1];
                    dict.put(key,value);
                } else {
                    System.out.println("Invalid entry: "+line);
                }
            }
        } catch (IOException e) {
                e.printStackTrace();
        }


        return dict.get(key);
    }



    public void cloneRepoPrivate(String repoUrl, String localPath, String accessToken) {
        try {
            accessToken="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"; //임시로 발급받은 코드
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("DESKTOP-65EJS6N\\yhyh5", accessToken))
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }




}
