package com.github.filemanager;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.*;
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

                if(existkey.equals(gitID)) { //새로운 ID가 setting에 존재하는 경우

                    int choice=JOptionPane.showConfirmDialog(null,"해당 ID가 존재합니다. 이전 token을 사용하시겠습니까?"," ", JOptionPane.YES_NO_OPTION);

                    if(choice==JOptionPane.NO_OPTION) { //새로운 토큰을 사용
                        sb.append(gitID).append(" ").append(accessToken).append(System.lineSeparator());
                        idExists = true;
                    } else { //setting.txt에 있는 토큰 사용
                        this.accessToken=existvalue;
                        sb.append(line).append(System.lineSeparator());
                        idExists=true;
                    }
                } else { //새로운 ID가 setting에 존재하지 않는 경우
                    sb.append(line).append(System.lineSeparator());
                }

            } else {
                System.out.println("Invalid entry: "+line);
            }
        }
        br.close();

        if(!idExists) { //새로운 ID가 setting.txt에 존재하지 않으면 setting에 추가.
            sb.append(gitID).append(" ").append(accessToken).append(System.lineSeparator());
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(settingpath));
        bw.write(sb.toString());
        bw.close();

    }

    public void cloneRepoPrivate(String repoUrl, String gitID, String accessToken,String localPath) throws GitAPIException, IOException {
        String settingpath=OsUtils.getAbsolutePathByOs(System.getProperty("user.dir")+"\\src\\main\\resources\\settings.txt");


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
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitID, accessToken))
                    .call();


        }
    }




}
