package com.github.filemanager;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.List;


class branchAndCommit{
    public String branchname;
    public RevCommit commitname;
    public branchAndCommit(String name, RevCommit tempCommit){
        branchname =name;
        commitname = tempCommit;
    }
}

class textgraphAndBranchAndCommit{
    public String textgraph;
    public String branchname;
    public RevCommit commitname;
    public textgraphAndBranchAndCommit(String text, String name, RevCommit tempCommit){
        textgraph = text;
        branchname =name;
        commitname = tempCommit;
    }
}


public class GitCommitHistory {
    ///////////////////////

    public static void showCommitHistory(Git git) throws GitAPIException {

        try {
            //현재 브랜치 찾기.
            Repository repository = git.getRepository();
            String branchName = repository.getBranch();
            System.out.println("Current Branch: " + branchName);

            // 모든 브랜치 가져오기
            List<Ref> branches = git.branchList().call();

            // 현재 브랜치와 이름이 일치하는 커밋 가져오기
            List<branchAndCommit> commits = new ArrayList<>();
            for (Ref branch : branches) {
                Iterable<RevCommit> branchCommits = git.log().add(branch.getObjectId()).call();
                //같은 브랜치인지 비교
                if(branchName.equals(branch.getName().substring(branch.getName().lastIndexOf('/') + 1))){
                    System.out.println(branch.getName());
                    for (RevCommit commit : branchCommits) {
                        commits.add(new branchAndCommit(branch.getName(), commit));
                    }
                }
            }
            // git log 출력
            for (branchAndCommit commit : commits) {
                System.out.println(
                        "-------------------------------------------" +
                        "  Checksum: " + commit.commitname.getId().getName() +
                        "  Author: " + commit.commitname.getAuthorIdent().getName() +
                        "  message: " + commit.commitname.getShortMessage());
                //"Author: " + commit.getAuthorIdent().getName() + " <" + commit.getAuthorIdent().getEmailAddress() + ">"
            }

            //dfs방식 구현
            Stack<branchAndCommit> commitStack = new Stack<>();
            commitStack.push(commits.get(0));
            while(!commitStack.empty()){
                branchAndCommit bac = commitStack.pop();
                System.out.println(bac.commitname.getShortMessage());
                for(RevCommit node: bac.commitname.getParents()){//부모 노드 호출
                    //스택에 넣기
                    commitStack.push(new branchAndCommit(branchName, node));
                }
            }

            /*
            // 그래프 출력
            int parentCount = 0;
            //int j=0;
            for (branchAndCommit commit : commits) {
                String commitId = commit.commitname.getName();
                String shortMessage = commit.commitname.getShortMessage();
                String graph = createGraph(parentCount);
                System.out.println(graph + "* " + commitId + " " + shortMessage);
                parentCount = commit.commitname.getParentCount();
            }
            */

            /////////////////////////////////////////////////////////
            JFrame framed = new JFrame("깃 커밋 히스토리");
            framed.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);//EXIT_ON_CLOSE

            // Create column names
            String[] columnNames = {"graph path", "name", "short message"};


            // Create data for the table
            Object[][] data = new Object[commits.size()][columnNames.length];
            int i=0;
            int j=0;
            for (branchAndCommit commit : commits) {
                Object[] newRow = {
                        commit.commitname.getId().getName(),
                        commit.commitname.getAuthorIdent().getName(),
                        commit.commitname.getShortMessage()
                };
                for(j=0;j<columnNames.length;j++){
                    data[i][j] = newRow[j];
                }
                i++;
            }


            // Create a JTable
            JTable table = new JTable(data, columnNames);

            // Add the table to a scroll pane
            JScrollPane scrollPane = new JScrollPane(table);
            framed.add(scrollPane);

            // Set the size and visibility of the frame
            framed.setSize(400, 300);
            framed.setVisible(true);

            framed.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // 상위 JFrame 닫기
                    framed.dispose();
                }
            });
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }


    }

    private static String createGraph(int parentCount) {
        StringBuilder graphBuilder = new StringBuilder();
        for (int i = 0; i < parentCount - 1; i++) {
            graphBuilder.append("|  ");
        }
        if (parentCount > 0) {
            graphBuilder.append("|--");
        }
        return graphBuilder.toString();
    }



    //////////
}
