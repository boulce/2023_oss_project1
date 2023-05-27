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
    static int count_labelssSize=0;
    static int numOfDownClik=0;
    //int count_labelssSize=0;
    static JFrame refwindow =null;


    public static void showCommitHistory(Git git) throws GitAPIException {

        try {
            //현재 브랜치 찾기.
            Repository repository = git.getRepository();
            String branchName = repository.getBranch();
            //System.out.println("Current Branch: " + branchName);

            // 모든 브랜치 가져오기
            List<Ref> branches = git.branchList().call();

            // 현재 브랜치와 이름이 일치하는 커밋 가져오기
            int numOfCommits=0;
            List<branchAndCommit> commits = new ArrayList<>();
            for (Ref branch : branches) {
                Iterable<RevCommit> branchCommits = git.log().add(branch.getObjectId()).call();
                //같은 브랜치인지 비교
                if(branchName.equals(branch.getName().substring(branch.getName().lastIndexOf('/') + 1))){
                    //System.out.println(branch.getName());
                    for (RevCommit commit : branchCommits) {
                        numOfCommits++;
                        commits.add(new branchAndCommit(branch.getName(), commit));
                    }
                }
            }

            // git log 출력
            /*
            for (branchAndCommit commit : commits) {
                System.out.println(
                        "-------------------------------------------" +
                        "  Checksum: " + commit.commitname.getId().getName() +
                        "  Author: " + commit.commitname.getAuthorIdent().getName() +
                        "  message: " + commit.commitname.getShortMessage());
                //"Author: " + commit.getAuthorIdent().getName() + " <" + commit.getAuthorIdent().getEmailAddress() + ">"
            }
*/


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
                //System.out.println(forprint_.line + forprint_.commitname.getShortMessage());
            }

            JFrame framed = new JFrame("Image Click Example");
            framed.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);//EXIT_ON_CLOSE
            framed.setLayout(null);

            JButton[] textFields = new JButton[forPrint.size()];

            // 이미지 로드
            int countfortextFields =0;
            for(textgraphAndCommit forprint_ : forPrint){
                textFields[countfortextFields] = new JButton(forprint_.commitname.getAuthorIdent().getName() + " : " + forprint_.commitname.getShortMessage());
                countfortextFields++;
            }

            /*
            for(textgraphAndCommit forprint_ : forPrint){
                System.out.println(forprint_.line + forprint_.commitname.getShortMessage());
            }
*/
            // 이미지 레이블 생성 및 설정
            for (int i = 0; i < forPrint.size(); i++) {
                textFields[i].setBounds((forPrint.get(i).line+1)*100, (i+1)*70,150 * 3,50);
                //labels[i].setBounds(100, (i +                //labels[i] = new JLabel(textFields[i]); 1) * 30, images[i].getIconWidth(), images[i].getIconHeight());

                final int index = i; // MouseAdapter에서 참조하기 위해 인덱스를 final 변수로 설정
                textFields[i].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if(refwindow == null){
                            refwindow = showInfoWindow(index, forPrint.get(index));
                        }
                        else{
                            refwindow.dispose();
                            refwindow = showInfoWindow(index, forPrint.get(index));
                        }
                    }
                });
                framed.add(textFields[i]);
            }


            //JButton[] textFields = new JButton[forPrint.size()];
            /*
            JLabel[][] labelss = new JLabel[forPrint.size()][20];
            for(int i=0;i<forPrint.size();i++){
                for(int j=0;j<20;j++){
                    labelss[i][j] = new JLabel(new ImageIcon("image1.png"));
                }
            }
            */
            int tNumOfImages = 200;
            JLabel[] labelss = new JLabel[forPrint.size() * forPrint.size() * tNumOfImages];

            count_labelssSize=0;
            //선 긋기. //이미지로 구현
            for(int i=0;i<forPrint.size();i++){
                for(int j=i;j<forPrint.size();j++){
                    if(AisParentOfB(forPrint.get(j),forPrint.get(i))){
                        //System.out.println(j + "는 부모다 " + i + "의");
                        int startX = (forPrint.get(i).line+1)*100 + 75 ;
                        int startY = (i+1)*70 +25 ;
                        int endX = (forPrint.get(j).line+1)*100+ 75 ;
                        int endY = (j+1)*70+25 ;

                        int difx=endX-startX;
                        int dify=endY-startY;

                        //이미지로 선 긋자.
                        float a=startX;
                        float b=startY;
                        int c;
                        for(c=0;c< tNumOfImages;c++){
                            a = startX +  ((c * difx)/tNumOfImages);
                            b = startY +  ((c * dify)/tNumOfImages);

                            /*
                            JLabel label = new JLabel(new ImageIcon("image1.png"));
                            label.setBounds(a,b,3,3);
                            framed.add(label);
                            */
                            //ImageIcon img = new ImageIcon(GitCommitHistory.class.getClassLoader().getResource("image1.png"));
                            JLabel starlabel = new JLabel();
                            starlabel.setText("*");
                            starlabel.setBackground(Color.BLACK);
                            starlabel.setOpaque(true);
                            labelss[count_labelssSize] = starlabel;//new ImageIcon(imagePath));
                            labelss[count_labelssSize].setBounds((int)a,(int)b,3,3);
                            framed.add(labelss[count_labelssSize]);

                            count_labelssSize++;
                        }
                    }
                }
            }
            //framed.add(labelss);

            JButton upMoveButton = new JButton("up");
            upMoveButton.setBounds(10,10,50,50);
            upMoveButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    numOfDownClik--;
                    for (int i = 0; i < forPrint.size(); i++) {
                        //textFields[i].setLocation((forPrint.get(i).line+1)*100, (i+1)*70 - numOfDownClik*50);
                        textFields[i].setLocation(textFields[i].getX(), textFields[i].getY() + 50);
                    }
                    for(int i=0;i<count_labelssSize;i++){
                        labelss[i].setLocation(labelss[i].getX(), labelss[i].getY() + 50);
                    }
                }
            });
            framed.add(upMoveButton);

            JButton downMoveButton = new JButton("down");
            downMoveButton.setBounds(10,70,50,50);
            downMoveButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    numOfDownClik++;
                    for (int i = 0; i < forPrint.size(); i++) {
                        //textFields[i].setLocation((forPrint.get(i).line+1)*100, (i+1)*70 - numOfDownClik*50);
                        textFields[i].setLocation(textFields[i].getX(), textFields[i].getY() - 50);
                    }
                    for(int i=0;i<count_labelssSize;i++){
                        labelss[i].setLocation(labelss[i].getX(), labelss[i].getY() - 50);
                    }
                }
            });
            framed.add(downMoveButton);


            framed.setSize(1500, 800);
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

    private static boolean AisParentOfB(textgraphAndCommit A, textgraphAndCommit B){
        for(RevCommit parentOfB : B.commitname.getParents()){
            if(A.commitname == parentOfB) {
                return true;
            }
        }
        return false;
    }
    private static JFrame showInfoWindow(int index, textgraphAndCommit TGCcommit) {
        JFrame infoWindow = new JFrame(TGCcommit.commitname.getName() + TGCcommit.commitname.getShortMessage());
        infoWindow.setLayout(new FlowLayout());
        infoWindow.setSize(300, 200);

        int tNumOfLabel = 3;
        JLabel infoLabel[] = new JLabel[tNumOfLabel];
        infoLabel[0] = new JLabel("Name: " + TGCcommit.commitname.getAuthorIdent().getName());
        infoLabel[1] = new JLabel("EmailAddress: " + TGCcommit.commitname.getAuthorIdent().getEmailAddress());
        infoLabel[2] = new JLabel("CommitTime: " + TGCcommit.commitname.getCommitterIdent().getWhen());
//infoLabel[3] = new JLabel("getAuthorIdent(): " + TGCcommit.commitname.getAuthorIdent().toString());

        for(int i=0;i<tNumOfLabel;i++){
            //textFields[i].setBounds((forPrint.get(i).line+1)*100, (i+1)*70,150 * 3,50);
            infoLabel[i].setBounds(30, (i+1) *20, 150,150);
            infoWindow.add(infoLabel[i]);
        }
        infoWindow.setVisible(true);
        return infoWindow;
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

        forPrintList.add(insertindex, new textgraphAndCommit(lineNum ,node));//해당 줄

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
