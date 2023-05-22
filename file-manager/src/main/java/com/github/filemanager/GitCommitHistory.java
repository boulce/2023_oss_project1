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

class textgraphAndCommit{
    public String textgraph;
    public RevCommit commitname;
    public textgraphAndCommit(String text, RevCommit tempCommit){
        textgraph = text;
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
            int numOfCommits=0;
            List<branchAndCommit> commits = new ArrayList<>();
            for (Ref branch : branches) {
                Iterable<RevCommit> branchCommits = git.log().add(branch.getObjectId()).call();
                //같은 브랜치인지 비교
                if(branchName.equals(branch.getName().substring(branch.getName().lastIndexOf('/') + 1))){
                    System.out.println(branch.getName());
                    for (RevCommit commit : branchCommits) {
                        numOfCommits++;
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
            ArrayList<textgraphAndCommit> forPrint = new ArrayList<>();

            int lineCount =0;
            Stack<branchAndCommit> commitStack = new Stack<>();
            ArrayList<String> visitedCheckSum = new ArrayList<>();

            commitStack.push(commits.get(0));
            while(!commitStack.empty()){
                branchAndCommit bac = commitStack.pop();
                //System.out.println(bac.commitname.getShortMessage());
                addCommitForPrint(forPrint,bac.commitname,lineCount);
                visitedCheckSum.add(bac.commitname.getId().getName());//checkSum 가져오기
                //if((bac.commitname.getParents().length == 0)){ //갈 수 있는 부모 노드 수를 반환하는 함수.
                if(getCangoCount(visitedCheckSum, bac.commitname.getParents()) == 0){ //갈 수 있는 부모 노드 수를 반환하는 함수.
                    //부모노드가 0이거나 있더라도 이미 방문한 노드면
                    //줄 수를 늘릴 것이다.
                    lineCount++;
                    //System.out.println(lineCount);
                }
                for(RevCommit node: bac.commitname.getParents()){//부모 노드 호출
                    //visitedCheckSum에 같은게 있다면 이미 방문한 거고 아니면 방문한 적 없는 것.
                    if(visitedCheckSum.contains(node.getId().getName())){
                        //이미 방문함.
                    }
                    else{
                        //스택에 넣기
                        commitStack.push(new branchAndCommit(branchName, node));
                    }
                }
            }

            for(textgraphAndCommit forprint_ : forPrint){
                System.out.println(forprint_.textgraph + forprint_.commitname.getShortMessage());
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

    private static int getCangoCount(ArrayList<String> visitedCS, RevCommit[] nodes){
        int result = nodes.length;
        for(RevCommit node : nodes){
            if(visitedCS.contains(node.getId().getName())){
                result--;
            }
        }
        return result;
    }
    private static void addCommitForPrint(ArrayList<textgraphAndCommit> forPrintList,RevCommit node,int lineNum) {
        //forPrintList.add(new textgraphAndCommit("123",node));

        int insertindex = calIndex(forPrintList, node, lineNum);
        String tempStr = "";
        for(int i=0;i<lineNum;i++){
            tempStr += "|____";
        }
        tempStr += "*____";
        forPrintList.add(insertindex, new textgraphAndCommit(tempStr ,node));//해당 줄
/*
        int count=0;
        for(textgraphAndCommit forPrint : forPrintList){
            if(count < insertindex){//insertindex 이전
                forPrint.textgraph += "_____";
            }
            else if(count == insertindex){//insertindex 해당줄
            }
            else{//insertindex 이후
                forPrint.textgraph += "|____";
            }
            count++;
        }*/
    }

    private static int calIndex(ArrayList<textgraphAndCommit> forPrintList,RevCommit node,int lineNum){

        int num=0;
        for(textgraphAndCommit forPrint : forPrintList){
            if(forPrint.commitname.getCommitTime() < node.getCommitTime()){ //node가 더 나중에 커밋된 경우.
                break;
            }
            num++;
        }
        return num;
    }
}
