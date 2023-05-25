package com.github.filemanager;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.util.HashMap;
import java.io.BufferedReader;

public class GitClone {
        String repoUrl;
        String gitID;
        String accessToken;
        String localPath;

        HashMap<String,String> dict;

    public GitClone(String repoUrl,String gitID, String accessToken,String localPath) throws GitAPIException {
        this.repoUrl=repoUrl;
        this.gitID=gitID;
        this.accessToken=accessToken;
        this.localPath=localPath;

        dict=new HashMap<>();
        cloneRepoPrivate(repoUrl,gitID,accessToken,localPath);
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



    public void cloneRepoPrivate(String repoUrl, String gitID, String accessToken,String localPath) throws GitAPIException {
        try {
            accessToken="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"; //임시로 발급받은 코드
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))

                    .call();
        } catch (GitAPIException e) {

            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("yhyh5275@naver.com", accessToken)) //email도 되고, username도 가능
                    .call();

           // e.printStackTrace();
        }
    }




}
