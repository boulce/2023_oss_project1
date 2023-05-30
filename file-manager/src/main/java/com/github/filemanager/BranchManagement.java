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
    public void BranchCreate() throws GitAPIException {
        //버튼 입력 이벤트 받음. 실행. Jgit에서 브랜치 생성

    }

    public String[] getBranchNamesList(Git git, String current_branch) throws GitAPIException {
        String[] branchNames = new String[git.branchList().call().size() - 1];
        int i = 0;

        for (Ref ref : git.branchList().call()) {
            // refs/head/actual_name
            String name = ref.getName().split("/")[2];
            if (!current_branch.equals(name))
                branchNames[i++] = ref.getName().split("/")[2];
        }

        return branchNames;
    }

    //3. Branch Delete
    public void BranchDelete(Git git, String name) throws GitAPIException {

        git.branchDelete().setBranchNames(name).call();

    }


    //4. Branch Rename

    //5. Branch Checkout


}
