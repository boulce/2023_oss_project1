package com.github.filemanager;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;

import javax.swing.*;
import java.io.IOException;



public class BranchMerge {

    /*
        branchMerge 함수는 merge가 정상적으로 수행되면 true를 반환하고, 수행안되면 false를 반환한다.
     */
    public static boolean branchMerge(Git git, String target_branch_name) throws IOException, GitAPIException {
        // merge하기 전에 checkout 되어 있어야 한다. 해솔이가 구현해야 하는 부분
        CheckoutCommand coCmd = git.checkout();
        coCmd.setName("master");
        coCmd.setCreateBranch(false); // probably not needed, just to make sure
        coCmd.call(); // switch to "master" branch
        ///////////////////////////////////////////////////////////////////////////////////

        String current_branch_name = git.getRepository().getFullBranch(); // 현재 checkout 된 브랜치 이름 저장
        if(target_branch_name != current_branch_name){ // 선택한 target_branch가 current_branch와 달라야 merge 수행한다.
            // 현재 checkout 된 브랜치에 target_branch를 merge한다.
            MergeCommand mgCmd = git.merge();
            mgCmd.include(git.getRepository().findRef(target_branch_name));
            MergeResult mergeResult = mgCmd.call();


            // merge가 성공적으로 되었는지 안되었는지 판단해서 창 띄우는 부분, 아직 제대로 작동 안함. 구현중
            if (mergeResult.getMergeStatus().isSuccessful() ) {
                JOptionPane.showMessageDialog(null, "Merging \"" + target_branch_name + "\" to \"" + current_branch_name + "\" was done successfully!");
                return true;
            } else {
                if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)){
                    System.out.println(mergeResult.getConflicts().toString());
                }
                else{
                    JOptionPane.showMessageDialog(null, mergeResult.getMergeStatus());
                }
                return false;
            }
        }
        else{ // 만약 target_branch와 current_branch가 같다면 merge 수행 안되도록 한다.
            JOptionPane.showMessageDialog(null, "You can't choose the current branch");
            return false;
        }
    }
}
