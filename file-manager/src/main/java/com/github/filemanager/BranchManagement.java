package com.github.filemanager;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Ref;


public class BranchManagement extends JFrame {


    //1. 현재 브랜치 이름 띄우기 -현재 경로가 깃으로 관리 되고 있는지 확인, 관리되고 있으면 파일매니저 우측 하단 패널에 브랜치명 띄우기
    public String BranchName(Git git, File currentPath, Repository repository) {
        if (GitUtilsForTrack.isGitRepository(currentPath)) {
            try {
                git.status().call(); //git으로 관리되고 있으면
                Ref head = repository.exactRef("HEAD");
                String branchName = head.getTarget().getName();
                return Repository.shortenRefName(branchName);
            } catch (GitAPIException | IOException e) {
                throw new RuntimeException(e);
            }

        }
        return null;
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
//Caused by: org.eclipse.jgit.api.errors.NotMergedException: Branch was not deleted as it has not been merged yet; use the force option to delete it anyway
        //이 예외 처리
    }

    //4. Branch Rename
    public void BranchRename(Git git, String oldName, String newName) throws GitAPIException {


        git.branchRename().setOldName(oldName).setNewName(newName).call();
//Caused by: org.eclipse.jgit.api.errors.RefAlreadyExistsException: Ref refs/heads/eee already exists
        //이 예외 처리
    }

    //5. Branch Checkout
    public void BranchCheckout(Git git, String name) throws GitAPIException {


        git.checkout().setName(name).call();

    }

}