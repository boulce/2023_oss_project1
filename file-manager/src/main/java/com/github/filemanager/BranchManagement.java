package com.github.filemanager;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import com.github.filemanager.GitUtilsForTrack.*;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;


public class BranchManagement  extends JFrame{
    private JTextField branchName;

    //1. 현재 브랜치 이름 띄우기 -현재 경로가 깃으로 관리 되고 있는지 확인, 관리되고 있으면 파일매니저 우측 하단 패널에 브랜치명 띄우기
    public void BranchName (Git git, File currentPath){
        if (GitUtilsForTrack.isGitRepository(currentPath)) {
            Status status = null;
            try {
                status = git.status().call();

            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }

        } else {
            try {

            } catch (Exception e) {

            }
        }
    }


    //2. New Branch Create
    public void BranchCreate() throws GitAPIException {
        //버튼 입력 이벤트 받음. 실행. Jgit에서 브랜치 생성

    }



    //3. Error Popup

    //3. Branch Delete

    //4. Branch Rename

    //5. Branch Checkout


}
