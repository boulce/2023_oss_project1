package com.github.filemanager;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Ref;

public class BranchManagement  extends JFrame{
    private JTextField branchName;

    //1. 현재 브랜치 이름 띄우기 -현재 경로가 깃으로 관리 되고 있는지 확인, 관리되고 있으면 파일매니저 우측 하단 패널에 브랜치명 띄우기
    public void BranchName (Git git, File currentPath){


    }


    //2. New Branch Create
    public void BranchCreate(Git git, String name) throws GitAPIException {
        //버튼 입력 이벤트 받음. 실행. Jgit에서 브랜치 생성

        // valiation
        for (Ref ref : git.branchList().call()) {
            if (ref.getName().equals("refs/heads/" + name)) {
                System.out.println("Branch already exists");
                return;
            }
        }

        git.branchCreate()
                .setName(name)
                .call();

    }




    //3. Error Popup

    //3. Branch Delete

    //4. Branch Rename

    //5. Branch Checkout


}
