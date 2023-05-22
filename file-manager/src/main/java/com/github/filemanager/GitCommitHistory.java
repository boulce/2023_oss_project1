package com.github.filemanager;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    public int line;
    public RevCommit commitname;
    public textgraphAndCommit(int inputline, RevCommit tempCommit){
        line = inputline;
        commitname = tempCommit;
    }
}


public class GitCommitHistory {
    private ImageIcon[] images;
    private static JLabel[] labels;

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
                System.out.println(forprint_.line + forprint_.commitname.getShortMessage());
            }
            //////////////
            JFrame framed = new JFrame("Image Click Example");
            framed.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);//EXIT_ON_CLOSE
            framed.setLayout(null);

            //ImageIcon[] images = new ImageIcon[3];
            JButton[] textFields = new JButton[forPrint.size()];

            //labels = new JLabel[3];

            // 이미지 로드
            int countfortextFields =0;
            for(textgraphAndCommit forprint_ : forPrint){
                textFields[countfortextFields] = new JButton(forprint_.commitname.getShortMessage());
                countfortextFields++;
            }

//            textFields[0] = new JButton("a");
  //          textFields[1] = new JButton("image2.png");
    //        textFields[2] = new JButton("image3.png");
            //ImageIcon icon_git = new ImageIcon(getClass().getClassLoader().getResource("unmodified,commited.png")); //git 상태 아이콘 준비

            // 이미지 레이블 생성 및 설정
            for (int i = 0; i < forPrint.size(); i++) {
                textFields[i].setBounds((forPrint.get(i).line+1)*100, (i+1)*70,150,50);
                //labels[i] = new JLabel(textFields[i]);
                //labels[i].setBounds(100, (i + 1) * 30, images[i].getIconWidth(), images[i].getIconHeight());

                final int index = i; // MouseAdapter에서 참조하기 위해 인덱스를 final 변수로 설정
                textFields[i].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        showInfoWindow(index);
                    }
                });

                framed.add(textFields[i]);
            }

            framed.setSize(500, 400);
            framed.setVisible(true);

            framed.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // 상위 JFrame 닫기
                    framed.dispose();
                }
            });

            //////////////

            /*


            /////////////////////////////////////////////////////////
            JFrame framed = new JFrame("깃 커밋 히스토리");
            framed.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);//EXIT_ON_CLOSE

            // Create column names
            String[] columnNames = {"graph path", "name", "short message"};


            // Create data for the table
            Object[][] data = new Object[forPrint.size()][columnNames.length];
            int i=0;
            int j=0;
            for (textgraphAndCommit forPrint_ : forPrint) {
                Object[] newRow = {
                        forPrint_.line,
                        forPrint_.commitname.getAuthorIdent().getName(),
                        forPrint_.commitname.getShortMessage()
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

            table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        int selectedRow = table.getSelectedRow();
                        int selectedColumn = table.getSelectedColumn();

                        // Handle the selection event
                        System.out.println("Selected: " + data[selectedRow][selectedColumn]);
                        JOptionPane.showMessageDialog(framed, "information: \n" +
                                "commit time : " + forPrint.get(selectedRow).commitname.getCommitTime());

                    }
                }
            });
             */

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }


    }

    private static void showInfoWindow(int index) {
        JFrame infoWindow = new JFrame("Image Info Window");
        infoWindow.setLayout(new FlowLayout());
        infoWindow.setSize(300, 200);

        JLabel infoLabel = new JLabel("Selected Image: " + (index + 1));
        infoWindow.add(infoLabel);

        infoWindow.setVisible(true);
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
        /*
        String tempStr = "";
        for(int i=0;i<lineNum;i++){
            tempStr += "|____";
        }
        tempStr += "*____";
        */

        forPrintList.add(insertindex, new textgraphAndCommit(lineNum ,node));//해당 줄
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
