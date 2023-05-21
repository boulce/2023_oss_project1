package com.github.filemanager;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.*;

//import org.eclipse.jgit.api.Git;
//import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static com.github.filemanager.GitUtilsForTrack.getGitRepository;
import static com.github.filemanager.GitUtilsForTrack.isGitRepository;


class branchAndCommit{
    public String branchname;
    public RevCommit commitname;
    public branchAndCommit(String name, RevCommit tempCommit){
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

            // 그래프 출력
            //RevWalk revWalk = new RevWalk(repository);
            int parentCount = 0;
            //int j=0;
            for (branchAndCommit commit : commits) {
                String commitId = commit.commitname.getName();
                String shortMessage = commit.commitname.getShortMessage();
                String graph = createGraph(parentCount);
                System.out.println(graph + "* " + commitId + " " + shortMessage);
                parentCount = commit.commitname.getParentCount();

            }
            /*
            // 그래프 출력
            System.out.println(
                    "start\n" +
                            "  Checksum: " + commits.get(0).commitname.getId().getName() +
                            "  Author: " + commits.get(0).commitname.getAuthorIdent().getName() +
                            "  message: " + commits.get(0).commitname.getShortMessage());
            printGraph(commits.get(0), "", git);
*/
            //git.close();
            /*
            JFrame window = new JFrame("제목 있는 윈도우");
            window.setTitle("제목이 변경된 윈도우");
            window.setBounds(800, 100, 400, 200);

            Container container = window.getContentPane();
            container.setBackground(Color.ORANGE);

            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setVisible(true);

             */
            ////////////////////////////////////////////////////////////////
            /*
            SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run() {
                            try {
                                // Significantly improves the look of the output in
                                // terms of the file names returned by FileSystemView!
                                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                            } catch (Exception weTried) {
                            }
                            JFrame f = new JFrame(APP_TITLE);
                            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                            //FileManager fileManager = new FileManager();
                            GitCommitHistory fileManager = new GitCommitHistory();
                            f.setContentPane(fileManager.getGui());

                            try {
                                //URL urlBig = fileManager.getClass().getResource("fm-icon-32x32.png");
                                //URL urlSmall = fileManager.getClass().getResource("fm-icon-16x16.png");
                                ArrayList<Image> images = new ArrayList<Image>();
                                //images.add(ImageIO.read(urlBig));
                                //images.add(ImageIO.read(urlSmall));
                                f.setIconImages(images);
                            } catch (Exception weTried) {
                            }

                            f.pack();
                            f.setLocationByPlatform(true);
                            f.setMinimumSize(f.getSize());
                            f.setVisible(true);

                            fileManager.showRootFile();
                        }
                    });
                    */

            /////////////////////////////////////////////////////////
            SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run() {
                            try {
                                JFrame frame = new JFrame("깃 커밋 히스토리");
                                //frame.setTitle("깃 커밋 히스토리");
                                //frame.setBounds(800, 100, 400, 200);
                                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                //Container container = frame.getContentPane();
                                //container.setBackground(Color.ORANGE);


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
                                frame.add(scrollPane);

                                // Set the size and visibility of the frame
                                frame.setSize(400, 300);
                                frame.setVisible(true);
                                //

                            } catch (Exception weTried) {
                            }
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
