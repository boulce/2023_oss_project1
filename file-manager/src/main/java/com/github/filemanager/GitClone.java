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



    public GitClone(String repoUrl,String gitID, String accessToken,String localPath) throws GitAPIException, IOException {
        this.repoUrl=repoUrl;
        this.gitID=gitID;
        this.accessToken=accessToken;
        this.localPath=localPath;


        cloneRepoPrivate(repoUrl,gitID,accessToken,localPath);
    }




    public void ReadWriteSetting(String gitID, String accessToken, String settingpath) throws IOException {

        BufferedReader br=new BufferedReader(new FileReader(settingpath));
        StringBuilder sb=new StringBuilder();
        boolean idExists=false;

        String line;

        while((line=br.readLine())!=null) {
            String[] parts=line.split(" ");
            if(parts.length==2) {
                String existkey=parts[0];
                String existvalue = parts[1];

                if(existkey.equals(gitID)) {
                    sb.append(gitID).append(" ").append(accessToken).append(System.lineSeparator());
                    idExists=true;
                } else {
                    sb.append(line).append(System.lineSeparator());
                }

            } else {
                System.out.println("Invalid entry: "+line);
            }
        }
        br.close();

        if(!idExists) {
            sb.append(gitID).append(" ").append(accessToken).append(System.lineSeparator());
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(settingpath));
        bw.write(sb.toString());
        bw.close();

    }

    public void cloneRepoPrivate(String repoUrl, String gitID, String accessToken,String localPath) throws GitAPIException, IOException {
        String settingpath=System.getProperty("user.dir")+"\\src\\main\\resources\\settings.txt";


        try {

            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))

                    .call();
        } catch (GitAPIException e) {

            ReadWriteSetting(gitID,accessToken,settingpath);
            //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


            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitID, accessToken)) //email도 되고, username도 가능
                    .call();


        }
    }




}
