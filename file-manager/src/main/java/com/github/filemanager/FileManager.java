/*
The MIT License

Copyright (c) 2015-2023 Valentyn Kolesnikov (https://github.com/javadev/file-manager)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.github.filemanager;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.*;
import javax.swing.tree.*;

import org.apache.commons.io.FileUtils;

import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;

import java.util.List;


import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import static com.github.filemanager.GitUtilsForTrack.*;


import static com.github.filemanager.BranchManagement.*;
import static com.github.filemanager.BranchMerge.*;
import static com.github.filemanager.GitClone.*;
import static com.github.filemanager.GitCommitHistory.*;



/**
 * A basic File Manager. Requires 1.6+ for the Desktop &amp; SwingWorker classes, amongst other
 * minor things.
 *
 * <p>Includes support classes FileTableModel &amp; FileTreeCellRenderer.
 *
 * <p>TODO Bugs
 *
 * <ul>
 *   <li>Still throws occasional AIOOBEs and NPEs, so some update on the EDT must have been missed.
 *   <li>Fix keyboard focus issues - especially when functions like rename/delete etc. are called
 *       that update nodes &amp; file lists.
 *   <li>Needs more testing in general.
 *       <p>TODO Functionality
 *   <li>Implement Read/Write/Execute checkboxes
 *   <li>Implement Copy
 *   <li>Extra prompt for directory delete (camickr suggestion)
 *   <li>Add File/Directory fields to FileTableModel
 *   <li>Double clicking a directory in the table, should update the tree
 *   <li>Move progress bar?
 *   <li>Add other file display modes (besides table) in CardLayout?
 *   <li>Menus + other cruft?
 *   <li>Implement history/back
 *   <li>Allow multiple selection
 *   <li>Add file search
 * </ul>
 */


public class FileManager {

    /**
     * Title of the application
     */
    public static final String APP_TITLE = "FileMan";//asdf
    /**
     * Used to open/edit/print files.
     */
    private Desktop desktop;

    /**
     * Provides nice icons and names for files.
     */
    private FileSystemView fileSystemView;

    /**
     * currently selected File.
     */
    private File currentFile;

    /**
     * Main GUI container
     */
    private JPanel gui;

    /**
     * File-system tree. Built Lazily
     */
    private JTree tree;

    private DefaultTreeModel treeModel;

    /**
     * Directory listing
     */
    private JTable table;

    private JProgressBar progressBar;
    /**
     * Table model for File[].
     */
    private FileTableModel fileTableModel;

    private ListSelectionListener listSelectionListener;

    private MouseAdapter mouseAdapter;//우클릭 처리위해 추가한 변수
    private boolean cellSizesSet = false;
    private int rowIconPadding = 30; //우상단 테이블에서 행의 높이 결정 변수

    /* File controls. */
    private JButton openFile;
    private JButton printFile;
    private JButton editFile;
    private JButton deleteFile;
    private JButton newFile;
    private JButton copyFile;
    /* File details. */

    //git init 추가, git commit  추가, refresh 추가
    private JButton gitinit;
    private JButton gitcommit;
    private JButton refresh;

    private JButton restore_missed;

    private JButton branch_Create_btn; //F1: 브랜치 생성
    private JButton branch_Delete_btn;
    private JButton branch_Rename_btn;
    private JButton branch_Checkout_btn;
    private JButton merge_btn; // merge 버튼

    private JButton git_clone; //테스트용 버튼 곽수정
    private JButton gitCommitHistory;


    private JLabel fileName;
    private JTextField path;
    private JLabel date;
    private JLabel size;

    private JLabel branchName;

    /* GUI options/containers for new File/Directory creation.  Created lazily. */
    private JPanel newFilePanel;
    private JRadioButton newTypeFile;
    private JTextField name;

    private Git git; // FileManager에서 사용될 Git 변수
    private File currentPath; // 현재 JTree에서 가리키고 있는 디렉토리 정보

    private String currentPopupPath; // 지훈이가 만든 팝업에서 git 내부구현을 위한 변수

    private int commitTableRow; //commit table에서 removed를 클릭하면 반환하는 row
    private BranchList branchList; // branch list 창

    private BranchManagement branchManagement; //branch Management (Feature1)

    public Container getGui() {
        if (gui == null) {
            gui = new JPanel(new BorderLayout(3, 3));
            gui.setBorder(new EmptyBorder(5, 5, 5, 5));

            fileSystemView = FileSystemView.getFileSystemView();
            desktop = Desktop.getDesktop();

            JPanel detailView = new JPanel(new BorderLayout(3, 3));


            table = new JTable(); //table: 우상단 파일 행렬의 swing 구현체
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.setShowVerticalLines(false);

            listSelectionListener =
                    new ListSelectionListener() {
                        @Override
                        public void valueChanged(ListSelectionEvent lse) { //우상단 테이블에서 행이 선택되면 호출된다. 파일 상세정보를 업데이트한다.
                            int row = table.getSelectionModel().getLeadSelectionIndex(); //table에서 선택된 행중 가장 상단의 행의 인덱스 반환
                            setFileDetails(((FileTableModel) table.getModel()).getFile(row)); //선택된 행의 File정보를 읽어 setFileDetails메소드를 실행한다.
                        }
                    };
            table.getSelectionModel().addListSelectionListener(listSelectionListener);


            JScrollPane tableScroll = new JScrollPane(table);
            Dimension d = tableScroll.getPreferredSize();
            tableScroll.setPreferredSize(
                    new Dimension((int) d.getWidth(), (int) d.getHeight())); //우상단 테이블 크기 조정
            detailView.add(tableScroll, BorderLayout.CENTER); //우상단 테이블에 스크롤바 생성


            //Untracked를 위한 popup menu
            JPopupMenu popupMenuUntracked = new JPopupMenu();
            JMenuItem gitadd_Untracked = new JMenuItem("git add");
            //gitadd_Untracked.addActionListener();
            //gitadd_Untracked.addActionListener(new MyActionListener());
            popupMenuUntracked.add(gitadd_Untracked);
            //table.setComponentPopupMenu(popupMenuUntracked);
            gitadd_Untracked.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // JOptionPane.showMessageDialog(null, "gitadd_Untracked clicked");

                    try {
                        String gitpath = git.getRepository().getDirectory().getParent();

                        String temp = currentPopupPath.replace(OsUtils.getAbsolutePathByOs(gitpath + "\\"), "").replace("\\", "/");


                        AddCommand a = git.add();
                        a.addFilepattern(temp).call();

                        Status status = git.status().call();
                        fileTableModel.setGit(status, currentPath);
                        table.repaint();
                    } catch (GitAPIException ex) {
                        throw new RuntimeException(ex);
                    }


                }
            });

            //Modified를 위한 popup menu
            JPopupMenu popupMenuModified = new JPopupMenu();
            JMenuItem gitadd_Modified = new JMenuItem("git add");
            JMenuItem gitresotre_Modified = new JMenuItem("git restore");
            popupMenuModified.add(gitadd_Modified);
            popupMenuModified.add(gitresotre_Modified);
            gitadd_Modified.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        String gitpath = git.getRepository().getDirectory().getParent();


                        String temp = currentPopupPath.replace(OsUtils.getAbsolutePathByOs(gitpath + "\\"), "").replace("\\", "/");

                        AddCommand a = git.add();
                        a.addFilepattern(temp).call();


                        Status status = git.status().call();
                        fileTableModel.setGit(status, currentPath);
                        table.repaint();
                    } catch (GitAPIException ex) {
                        throw new RuntimeException(ex);
                    }

                }
            });
            gitresotre_Modified.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "gitresotre_Modified clicked");


                    try {
                        String gitpath = git.getRepository().getDirectory().getParent();


                        String temp = currentPopupPath.replace(OsUtils.getAbsolutePathByOs(gitpath + "\\"), "").replace("\\", "/");

                        CheckoutCommand checkout = git.checkout();
                        checkout.addPath(temp);
                        checkout.call();


                        Status status = git.status().call();
                        fileTableModel.setGit(status, currentPath);
                        table.repaint();

                    } catch (GitAPIException ex) {
                        throw new RuntimeException(ex);
                    }


                }
            });

            //staged(==Added)를 위한 popup menu
            JPopupMenu popupMenuStaged = new JPopupMenu();
            JMenuItem gitresotre_Staged = new JMenuItem("git restore --staged");
            popupMenuStaged.add(gitresotre_Staged);
            gitresotre_Staged.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "gitresotre_Staged clicked");
                    try {
                        String gitpath = git.getRepository().getDirectory().getParent();


                        String temp = currentPopupPath.replace(OsUtils.getAbsolutePathByOs(gitpath + "\\"), "").replace("\\", "/");


                        ResetCommand reset = git.reset();
                        reset.setRef("HEAD");
                        reset.addPath(temp);
                        reset.call();


                        Status status = git.status().call();
                        fileTableModel.setGit(status, currentPath);
                        table.repaint();

                    } catch (GitAPIException ex) {
                        throw new RuntimeException(ex);
                    }


                }


            });


            //Commited를 위한 popup menu
            JPopupMenu popupMenuCommited = new JPopupMenu();
            JMenuItem gitrm_cached = new JMenuItem("git rm --cached");
            JMenuItem gitrm = new JMenuItem("git rm");
            JMenuItem gitmv = new JMenuItem("git mv");
            popupMenuCommited.add(gitrm_cached);
            popupMenuCommited.add(gitrm);
            popupMenuCommited.add(gitmv);
            gitrm_cached.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "git rm --cached clicked");

                    try {
                        String gitpath = git.getRepository().getDirectory().getParent();


                        String temp = currentPopupPath.replace(OsUtils.getAbsolutePathByOs(gitpath + "\\"), "").replace("\\", "/");

                        RmCommand rm = git.rm();
                        rm.setCached(true);
                        rm.addFilepattern(temp);
                        rm.call();

                        Status status = git.status().call();
                        fileTableModel.setGit(status, currentPath);
                        table.repaint();

                    } catch (GitAPIException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            gitrm.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "git rm clicked");
                    try {
                        String gitpath = git.getRepository().getDirectory().getParent();


                        String temp = currentPopupPath.replace(OsUtils.getAbsolutePathByOs(gitpath + "\\"), "").replace("\\", "/");

                        RmCommand rm = git.rm();

                        rm.addFilepattern(temp);
                        rm.call();

                        Status status = git.status().call();
                        fileTableModel.setGit(status, currentPath);
                        table.repaint();

                    } catch (GitAPIException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });


            gitmv.addActionListener(new ActionListener() { //git mv를 이름바꾸는 용도로만 사용
                @Override
                public void actionPerformed(ActionEvent e) {
                    String newFilePath = JOptionPane.showInputDialog(null, "insert new file name add extension");

                    if (newFilePath != null && !newFilePath.isEmpty()) {
                        try {
                            String gitpath = git.getRepository().getDirectory().getParent();
                            String temp = currentPopupPath.replace(OsUtils.getAbsolutePathByOs(gitpath + "\\"), "").replace("\\", "/");
                            String newFilePathTemp = newFilePath.replace("\\", "/");

                            File oldFile = new File(gitpath, temp);
                            File newFile = new File(gitpath, newFilePathTemp);
                            if (!oldFile.renameTo(newFile)) {
                                JOptionPane.showMessageDialog(null, "Could not rename file");
                                return;
                            }


                            git.add().addFilepattern(newFilePathTemp).call();
                            git.rm().addFilepattern(temp).call();


                            Status status = git.status().call();
                            //name.txt에서 name1.txt로 바꾼 경우 getRemoved에 name.txt가 남아 filemanager상에 남는다. 그래서 변경전 이름의 아이콘이 removed로 표시된다.
                            fileTableModel.setGit(status, currentPath); //바로위 만 빼면 git bash상에서도 git mv와 똑같이 구현된다. ui구현의 차이
                            table.repaint();

                        } catch (GitAPIException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }

            });





            //우클릭 처리 추가
            mouseAdapter =
                    new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            if (e.getButton() == MouseEvent.BUTTON3) {//if (e.isPopupTrigger()) {
                                int row = table.rowAtPoint(e.getPoint());
                                if (row >= 0 && row < table.getRowCount()) {
                                    table.setRowSelectionInterval(row, row);
                                } else {
                                    table.clearSelection();
                                }

                                // 선택된 행에 대한 작업 수행
                                String filepath = ((FileTableModel) table.getModel()).getFile(row).getPath();

                                if (isGitRepository(new File(filepath))) {
                                    File file = getGitRepository(new File(filepath));

                                    try (Repository repo = Git.open(file).getRepository()) {
                                        Git git = new Git(repo);

                                        // git status 명령 실행
                                        Status status = git.status().call();


                                        Set<String> getUntrackedset = status.getUntracked();
                                        Set<String> unTrackted = new HashSet<String>();
                                        Iterator<String> stringIter = getUntrackedset.iterator();
                                        String path;
                                        while (stringIter.hasNext()) {
                                            String str = stringIter.next();
                                            path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                                            unTrackted.add(path); // 1 2 3
                                        }

                                        Set<String> getModifiedset = status.getModified();
                                        Set<String> Modified = new HashSet<String>();
                                        stringIter = getModifiedset.iterator();
                                        while (stringIter.hasNext()) {
                                            String str = stringIter.next();
                                            path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                                            Modified.add(path); // 1 2 3
                                        }

                                        Set<String> getAddedset = status.getAdded(); //파일을 새로 만들고 add한것
                                        Set<String> Added = new HashSet<String>();
                                        stringIter = getAddedset.iterator();
                                        while (stringIter.hasNext()) {
                                            String str = stringIter.next();
                                            path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                                            Added.add(path); // 1 2 3
                                        }

                                        Set<String> getChangedset = status.getChanged(); //changed 있는 파일을 변경하고 git add한것.
                                        Set<String> Changed = new HashSet<String>();
                                        stringIter = getChangedset.iterator();
                                        while (stringIter.hasNext()) {
                                            String str = stringIter.next();
                                            path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                                            Changed.add(path); // 1 2 3
                                        }
                                        Set<String> getRemovedset = status.getRemoved(); //unmodified=commited를 git rm한 것
                                        Set<String> Removed = new HashSet<String>();
                                        stringIter = getRemovedset.iterator();
                                        while (stringIter.hasNext()) {
                                            String str = stringIter.next();
                                            path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                                            Removed.add(path); // 1 2 3
                                        }
                                        Set<String> getMissingset = status.getMissing(); //파일을 rm 한 것.
                                        Set<String> Missing = new HashSet<String>();
                                        stringIter = getMissingset.iterator();
                                        while (stringIter.hasNext()) {
                                            String str = stringIter.next();
                                            path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                                            Missing.add(path); // 1 2 3
                                        }

                                        Set<String> getUntrackedFolderset = status.getUntrackedFolders(); //untracked인 폴더
                                        Set<String> UntrackedFolders = new HashSet<String>();
                                        stringIter = getUntrackedFolderset.iterator();
                                        while (stringIter.hasNext()) {
                                            String str = stringIter.next();
                                            path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                                            UntrackedFolders.add(path);
                                        }

                                        currentPopupPath = filepath;
                                        if (unTrackted.contains(filepath) || UntrackedFolders.contains(filepath)) {// untracked 파일일 경우
                                            popupMenuUntracked.show(e.getComponent(), e.getX(), e.getY());
                                        } else if (Modified.contains(filepath)) {//Modified 파일인 경우
                                            popupMenuModified.show(e.getComponent(), e.getX(), e.getY());
                                        } else if (Added.contains(filepath) || Changed.contains(filepath)) {//added,changed 파일인 경우
                                            popupMenuStaged.show(e.getComponent(), e.getX(), e.getY());
                                        } else if (Removed.contains(filepath)) { //commited가 git rm 된 경우

                                        } else { // commited 파일일 경우

                                            popupMenuCommited.show(e.getComponent(), e.getX(), e.getY());
                                        }


                                    } catch (IOException ex) {
                                        JOptionPane.showMessageDialog(null, "오류1");

                                    } catch (GitAPIException ex) {
                                        JOptionPane.showMessageDialog(null, "오류2");
                                    }
                                } else {
                                    JOptionPane.showMessageDialog(null, "깃으로 관리 중이지 않습니다.");
                                }
                            }
                        }
                    };
            table.addMouseListener(mouseAdapter);


            ///////////////////////////////////////////////////////////////////////


            ///////////////////////////////////////////////////////////////////////
            // the File tree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            treeModel = new DefaultTreeModel(root);

            // 왼쪽 디렉토리 JTree에서 특정 디렉토리를 선택했을 때 발생하는 이벤트
            TreeSelectionListener treeSelectionListener =
                    new TreeSelectionListener() {
                        public void valueChanged(TreeSelectionEvent tse) {
                            DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) tse.getPath().getLastPathComponent();
                            setFileDetails((File) node.getUserObject());
                            currentPath = (File) node.getUserObject();
                            setBranchName();
                            // 특정 디렉토리를 누를 때마다 그 선택된 디렉토리 정보를 가져온다
                            //버튼 관리
                            File selected_file = (File) node.getUserObject();
                            if (GitUtilsForTrack.isGitRepository(selected_file)) { // 만약 git에 의해 관리되는 저장소라면
                                gitinit.setEnabled(false); // git init과
                                gitcommit.setEnabled(true); // git commit 버튼을 disable하고
                                refresh.setEnabled(true);
                                restore_missed.setEnabled(true);


                                branch_Create_btn.setEnabled(true);  //Branch create 버튼 활성화
                                branch_Delete_btn.setEnabled(true);  //Branch delete 버튼 활성화
                                branch_Rename_btn.setEnabled(true);  //Branch rename 버튼 활성화
                                branch_Checkout_btn.setEnabled(true);//Branch checkout 버튼 활성화
                                merge_btn.setEnabled(true); // branch_list_btn 활성화

                                branchName.setEnabled(true);

                                git_clone.setEnabled(true); //곽수정
                                gitCommitHistory.setEnabled(true);
                                try {
                                    git = Git.open(getGitRepository(selected_file)); // git 저장소를 연다.
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else { // git에 의해 관리되지 않는 저장소라면
                                gitinit.setEnabled(true); // git init 과
                                gitcommit.setEnabled(false); // git commit 버튼을 활성화한다.
                                refresh.setEnabled(false);
                                restore_missed.setEnabled(false);

                                branch_Create_btn.setEnabled(false); // Branch create 버튼 비활성화
                                branch_Delete_btn.setEnabled(false);  //Branch delete 버튼 비활성화
                                branch_Rename_btn.setEnabled(false);  //Branch rename 버튼 비활성화
                                branch_Checkout_btn.setEnabled(false);//Branch checkout 버튼 비활성화
                                merge_btn.setEnabled(false); // branch_list_btn 비활성화
                                branchName.setEnabled(false);
                                git_clone.setEnabled(true); //곽수정
                                gitCommitHistory.setEnabled(false);
                            }

                            // 디렉토리가 깃에의해 관리되고 있는지 판단
                            if (isGitRepository(currentPath)) {
                                Status status = null;
                                try {
                                    status = git.status().call();
                                } catch (GitAPIException e) {
                                    throw new RuntimeException(e);
                                }

                                fileTableModel.setGit(status, currentPath);
                            } else {
                                try {
                                    fileTableModel.setPath(currentPath);

                                } catch (Exception e) {

                                }
                            }
                            showChildren(node);
                        }
                    };

            // show the file system roots. 왼쪽 루트 구조
            File[] roots = fileSystemView.getRoots();
            for (File fileSystemRoot : roots) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
                root.add(node);

                File[] files = fileSystemView.getFiles(fileSystemRoot, true);
                for (File file : files) {
                    if (file.isDirectory()) {
                        node.add(new DefaultMutableTreeNode(file));
                    }
                }
            }


            tree = new JTree(treeModel);
            tree.setRootVisible(false);
            tree.addTreeSelectionListener(treeSelectionListener);
            tree.setCellRenderer(new FileTreeCellRenderer());
            tree.expandRow(0);
            JScrollPane treeScroll = new JScrollPane(tree);

            // as per trashgod tip
            tree.setVisibleRowCount(15);

            Dimension preferredSize = treeScroll.getPreferredSize();
            Dimension widePreferred = new Dimension(200, (int) preferredSize.getHeight());
            treeScroll.setPreferredSize(widePreferred);

            // details for a File
            JPanel fileMainDetails = new JPanel(new BorderLayout(4, 2)); //우하단 회색영역 관련
            fileMainDetails.setBorder(new EmptyBorder(0, 6, 0, 6));

            JPanel fileDetailsLabels = new JPanel(new GridLayout(0, 1, 2, 2));
            fileMainDetails.add(fileDetailsLabels, BorderLayout.WEST);

            JPanel fileDetailsValues = new JPanel(new GridLayout(0, 1, 2, 2));
            fileMainDetails.add(fileDetailsValues, BorderLayout.CENTER);

            fileDetailsLabels.add(new JLabel("File", JLabel.TRAILING)); //우하단 회색영역
            fileName = new JLabel();
            fileDetailsValues.add(fileName);
            fileDetailsLabels.add(new JLabel("Path/name", JLabel.TRAILING));
            path = new JTextField(5);
            path.setEditable(false);
            fileDetailsValues.add(path);
            fileDetailsLabels.add(new JLabel("Last Modified", JLabel.TRAILING));
            date = new JLabel();
            fileDetailsValues.add(date);
            fileDetailsLabels.add(new JLabel("File size", JLabel.TRAILING));
            size = new JLabel();
            fileDetailsValues.add(size);
            fileDetailsLabels.add(new JLabel("Type", JLabel.TRAILING));



            int count = fileDetailsLabels.getComponentCount();
            for (int ii = 0; ii < count; ii++) {
                fileDetailsLabels.getComponent(ii).setEnabled(false);
            }

            JToolBar toolBar = new JToolBar();
            JToolBar toolBarForBranch = new JToolBar(); // Branch 관련 JToolBar
            // mnemonics stop working in a floated toolbar
            toolBar.setFloatable(false);
            toolBarForBranch.setFloatable(false); // Branch 관련 JToolBar setFlotable 설정

            // 아래쪽 버튼 생성
            // toolBar에 위치한 버튼들
            // 1. Open
            openFile = new JButton("Open");
            openFile.setMnemonic('o');

            openFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                desktop.open(currentFile);
                            } catch (Throwable t) {
                                showThrowable(t);
                            }
                            gui.repaint();
                        }
                    });
            toolBar.add(openFile);

            //2. Edit
            editFile = new JButton("Edit");
            editFile.setMnemonic('e');
            editFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                desktop.edit(currentFile);
                            } catch (Throwable t) {
                                showThrowable(t);
                            }
                        }
                    });
            toolBar.add(editFile);

            //3. Print
            printFile = new JButton("Print");
            printFile.setMnemonic('p');
            printFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                desktop.print(currentFile);
                            } catch (Throwable t) {
                                showThrowable(t);
                            }
                        }
                    });
            toolBar.add(printFile);

            // Check the actions are supported on this platform!
            openFile.setEnabled(desktop.isSupported(Desktop.Action.OPEN));
            editFile.setEnabled(desktop.isSupported(Desktop.Action.EDIT));
            printFile.setEnabled(desktop.isSupported(Desktop.Action.PRINT));

            toolBar.addSeparator();

            //3. New
            newFile = new JButton("New");
            newFile.setMnemonic('n');
            newFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            newFile();
                        }
                    });
            toolBar.add(newFile);


            // 4. Rename
            JButton renameFile = new JButton("Rename");
            renameFile.setMnemonic('r');
            renameFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            renameFile();
                        }
                    });
            toolBar.add(renameFile);

            //6. Delete
            deleteFile = new JButton("Delete");
            deleteFile.setMnemonic('d');
            deleteFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            deleteFile();
                        }
                    });
            toolBar.add(deleteFile);

            toolBar.addSeparator();

            //1. git init 버튼
            gitinit = new JButton("Git init");
            gitinit.addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                gitInit();
                                gitinit.setEnabled(false); // git init과
                                gitcommit.setEnabled(true); // git commit 버튼을 disable하고
                                refresh.setEnabled(true);
                                restore_missed.setEnabled(true);
                                merge_btn.setEnabled(true);

                                gitCommitHistory.setEnabled(true);
                            } catch (GitAPIException ex) {
                                throw new RuntimeException(ex);
                            }

                            Status status = null;
                            try {
                                status = git.status().call();
                            } catch (GitAPIException ex) {
                                throw new RuntimeException(ex);
                            }
                            try {
                                fileTableModel.setGit(status, currentPath);
                            } catch (Exception er) {

                            }

                            table.repaint();


                        }
                    }
            );
            toolBar.add(gitinit);

            // 2. git commit 버튼
            gitcommit = new JButton("Git commit");
            gitcommit.addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                gitCommit();
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            } catch (GitAPIException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
            );
            toolBar.add(gitcommit);

            // 3. refesh 버튼, 누르면 Modified와 같이 반영이 안된 파일들을 테이블에 반영해준다
            refresh = new JButton("Refresh Status");
            refresh.addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                refresh();
                            } catch (GitAPIException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
            );
            toolBar.add(refresh);


            // 4. restore for deleted 버튼, 누르면 현재 missed = deleted:modifed인 파일을 git restore 해준다.
            restore_missed = new JButton("restore Deleted:modified");
            restore_missed.addActionListener(

                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                restoremissed();
                            } catch (GitAPIException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }

            );
            toolBar.add(restore_missed);


            // toolBarForBranch에 위치한 버튼

            //5. git clone기능, 테스트용도 곽수정


            git_clone = new JButton("git clone");
            git_clone.addActionListener(

                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {


                            try {
                                gitclone(String.valueOf(currentPath));
                            } catch (GitAPIException | IOException ex) {
                                JOptionPane.showMessageDialog(null, "에러 발생! : "+ex.getMessage());
                                // throw new RuntimeException(ex);
                            }

                        }
                    }

            );
            toolBar.add(git_clone);


///////////////////////////////////////git history 보여주는 부분
            gitCommitHistory = new JButton("Git Commit History");
            gitCommitHistory.addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                showCommitHistory(git);
                                //showCommitHistory_ver2();
                            } catch (GitAPIException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }

            );
            toolBar.add(gitCommitHistory);

            ///////////////////////////////////////


//6. Branch Create 버튼
            branch_Create_btn = new JButton("Branch Create");
            branch_Create_btn.addActionListener(

                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                String newBranchName = JOptionPane.showInputDialog("Enter the new branch name");
                                branchManagement.BranchCreate(git, newBranchName);
                            } catch (GitAPIException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
            );

            //7. Branch Delete 버튼
            branch_Delete_btn = new JButton("Branch Delete");
            branch_Delete_btn.addActionListener(

                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {

                            try {
                                String[] options = branchManagement.getBranchNamesList(git, branchName.getText(), false);
                                int selection = JOptionPane.showOptionDialog(null, "Select one you want to delete:", "Branch list",
                                        0, 3, null, options, options[0]);
                                if (selection != -1)
                                    branchManagement.BranchDelete(git, options[selection]);
                            }
                            catch (NotMergedException ex) {
                                    // Handle NotMergedException
                                    String errorMessage = "Branch deletion failed. Branch has not been merged yet.";
                                    JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                                }
                            catch (GitAPIException exy) {
                                throw new RuntimeException(exy);
                            }

                        }
                    }
            );

            //8. Branch Rename 버튼
            branch_Rename_btn = new JButton("Branch Rename");
            branch_Rename_btn.addActionListener(

                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                String[] options = branchManagement.getBranchNamesList(git, branchName.getText(), true);
                                int selection = JOptionPane.showOptionDialog(null, "Select one you want to rename:", "Branch list",
                                        0, 3, null, options, options[0]);
                                if (selection != -1) {
                                    String newName = JOptionPane.showInputDialog("Enter the new branch name");
                                    branchManagement.BranchRename(git, options[selection], newName);
                                }
                            } catch (RefAlreadyExistsException ex) {
                                // Handle RefAlreadyExistsException
                                String errorMessage = "Branch rename failed. Branch name already exists.";
                                JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                            } catch (GitAPIException exy) {
                                throw new RuntimeException(exy);
                            }
                        }
                    }
            );

            //9. Branch Checkout 버튼
            branch_Checkout_btn = new JButton("Branch Checkout");
            branch_Checkout_btn.addActionListener(

                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                String[] options = branchManagement.getBranchNamesList(git, branchName.getText(), false);
                                int selection = JOptionPane.showOptionDialog(null, "Select one you want to checkout:", "Branch list",
                                        0, 3, null, options, options[0]);
                                if (selection != -1) {
                                    branchManagement.BranchCheckout(git, options[selection]);
                                    branchName.setText(options[selection]);
                                    refresh(); // Checkout 버튼 누르면 현재 브랜치에 맞게 브라우저 refresh
                                }

                            } catch (GitAPIException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
            );

            FileManager myFileManager = this;

            // toolBarForBranch에 위치한 버튼들
            //10. merge 버튼
            merge_btn = new JButton("Merge");
            merge_btn.addActionListener(

                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if(branchList == null){
                                try {
                                    branchList = new BranchList(git, myFileManager);
                                } catch (GitAPIException ex) {
                                    throw new RuntimeException(ex);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                            else{
                                branchList.dispose();
                                try {
                                    branchList = new BranchList(git, myFileManager);
                                } catch (GitAPIException | IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        }
                    }
            );



            //11. Branch Name 텍스트 필드
            JLabel branchLabel = new JLabel(" Current Branch: ");
            branchName = new JLabel("");
            //여기에 브랜치 이름 받아오거는 함수 실행



            toolBarForBranch.add(branch_Create_btn);
            toolBarForBranch.add(branch_Delete_btn);
            toolBarForBranch.add(branch_Rename_btn);
            toolBarForBranch.add(branch_Checkout_btn);
            toolBarForBranch.addSeparator();
            //merge 버튼
            toolBarForBranch.add(merge_btn);

            //branch name 글자로 띄우기
            toolBarForBranch.add(branchLabel);
            toolBarForBranch.add(branchName);


            JPanel fileView = new JPanel(new BorderLayout(3, 3)); //fileView는 우하단 회색영역 전체를 말한다.


            fileView.add(toolBar, BorderLayout.NORTH);
            fileView.add(toolBarForBranch, BorderLayout.CENTER); // Branch 관련 JToolBar 창에 띄우기
            fileView.add(fileMainDetails, BorderLayout.SOUTH); // toolBarForBranch를 toolBar 바로 밑에 위치시키기 위해 BorderLayout.SOUTH로 변경함

            detailView.add(fileView, BorderLayout.SOUTH);

            JSplitPane splitPane =
                    new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailView); //왼쪽 루트와 오른쪽 테이블,회색영역을 구분하는 코드 Vertical로 하면 위아래로 나온다.
            gui.add(splitPane, BorderLayout.CENTER);

            JPanel simpleOutput = new JPanel(new BorderLayout(3, 3));
            progressBar = new JProgressBar();
            simpleOutput.add(progressBar, BorderLayout.EAST);
            progressBar.setVisible(false);

            gui.add(simpleOutput, BorderLayout.SOUTH);
        }
        return gui;
    }


    public void showRootFile() {
        // ensure the main files are displayed
        tree.setSelectionInterval(0, 0);
    }

    private TreePath findTreePath(File find) {
        for (int ii = 0; ii < tree.getRowCount(); ii++) {
            TreePath treePath = tree.getPathForRow(ii);
            Object object = treePath.getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
            File nodeFile = (File) node.getUserObject();

            if (nodeFile.equals(find)) {
                return treePath;
            }
        }
        // not found!
        return null;
    }

    private void renameFile() {
        if (currentFile == null) {
            showErrorMessage("No file selected to rename.", "Select File");
            return;
        }

        String renameTo = JOptionPane.showInputDialog(gui, "New Name");
        if (renameTo != null) {
            try {
                boolean directory = currentFile.isDirectory();
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode) parentPath.getLastPathComponent();

                boolean renamed =
                        currentFile.renameTo(new File(currentFile.getParentFile(), renameTo));
                if (renamed) {
                    if (directory) {
                        // rename the node..

                        // delete the current node..
                        TreePath currentPath = findTreePath(currentFile);
                        DefaultMutableTreeNode currentNode =
                                (DefaultMutableTreeNode) currentPath.getLastPathComponent();

                        treeModel.removeNodeFromParent(currentNode);

                        // add a new node..
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" + currentFile + "' could not be renamed.";
                    showErrorMessage(msg, "Rename Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }

    //1. Git Init 함수 구현
    private void gitInit() throws GitAPIException {
        //git init 실행하는 코드 작성
        git = Git.init()
                .setDirectory(currentPath)
                .call();

    }

    //2. Git commit 구현
    private void gitCommit() throws IOException, GitAPIException {
        if (git.status().call().getAdded().size() > 0 || git.status().call().getChanged().size() > 0 || git.status().call().getRemoved().size() > 0) { //added인 파일이 1개 이상 존재하는 경우, 그리고 Changed인 파일이 1개이상 존재하는 경우
            // 표에 나타내기
            JPanel panel = new JPanel(new BorderLayout());
            JTextField textField = new JTextField();
            JLabel messageLabel = new JLabel(" Please enter your commit message. : \n");
            JPanel bottomPanel = new JPanel(new BorderLayout());
            //빈 테이블에 staged 된 파일 불러오기
            JTable table_commit = new JTable(new DefaultTableModel(new Object[]{"Status", "File", "Path/name"}, 0) {
                public Class<?> getColumnClass(int column) {
                    if (column == 0) {
                        return ImageIcon.class;
                    } else return Object.class;
                }


            });

            table_commit.setRowHeight(10 + rowIconPadding);//commit 테이블의 행 높이 조정

            Status status = git.status().call();
            Set<String> stagedSet = new HashSet<>();
            stagedSet.addAll(status.getAdded());
            stagedSet.addAll(status.getChanged());
            stagedSet.addAll(status.getRemoved());

            String temp = (String) (OsUtils.getAbsolutePathByOs(git.getRepository().getDirectory().getParent()));


            // 각 테이블에 staged 파일 목록 불러와서 추가하는 부분
            DefaultTableModel model = (DefaultTableModel) table_commit.getModel();


            for (String stagedFile : stagedSet) {
                File stagedFILE = new File(stagedFile);
                if (status.getAdded().contains(stagedFile) || status.getChanged().contains(stagedFile)) { //added, changed는 모두 파일이 추가되거나, 변경된 상태에서 staged된것.

                    ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("staged.png"));
                    Image image = icon.getImage();
                    Image newimg = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);


                    Object[] rowData = {new ImageIcon(newimg), stagedFILE.getName(), OsUtils.getAbsolutePathByOs(temp + "\\" + stagedFILE.getName())}; // 첫 번재 값은 아이콘(영헌이가 추가해야함), 두 번째는 파일 이름, // 세 번째는 파일의 절대 경로 (최상단 부모 깃 절대경로 + 파일의 상대경로)
                    model.addRow(rowData);


                } else { //removed =deleted

                    ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("removed.png"));
                    Image image = icon.getImage();
                    Image newimg1 = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);

                    Object[] rowData = {new ImageIcon(newimg1), stagedFILE.getName(), OsUtils.getAbsolutePathByOs(temp + "\\" + stagedFILE.getName())}; // 첫 번재 값은 아이콘(영헌이가 추가해야함), 두 번째는 파일 이름, // 세 번째는 파일의 절대 경로 (최상단 부모 깃 절대경로 + 파일의 상대경로)
                    model.addRow(rowData);
                }


            }


            panel.add(new JScrollPane(table_commit), BorderLayout.CENTER);

            bottomPanel.add(messageLabel, BorderLayout.WEST);
            bottomPanel.add(textField, BorderLayout.CENTER);

            panel.add(bottomPanel, BorderLayout.SOUTH);

            //-------------------------이하 지훈이 코드부분 발췌----------------------------------------------------

            //Removed를 위한 popup menu removed는 commit상태에서 git rm을 실행한 결과이고, deleted가 staged 된 상태이다.
            JPopupMenu popupMenuRemoved_commit = new JPopupMenu();
            JMenuItem gitremoved_commit = new JMenuItem("git restore --staged for removed");


            popupMenuRemoved_commit.add(gitremoved_commit);

            gitremoved_commit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "git restore --staged execute");


                    Status status = null;
                    try {
                        String gitpath = git.getRepository().getDirectory().getParent();
                        String temp = currentPopupPath.replace(OsUtils.getAbsolutePathByOs(gitpath + "\\"), "").replace("\\", "/");

                        ResetCommand reset = git.reset();
                        reset.setRef("HEAD");
                        reset.addPath(temp); //temp에는 파일명이 들어간다.
                        reset.call();

                        status = git.status().call();

                    } catch (GitAPIException ex) {
                        throw new RuntimeException(ex);
                    }

                    fileTableModel.setGit(status, currentPath);


                    ;
                    ((DefaultTableModel) table_commit.getModel()).removeRow(commitTableRow);
                    table_commit.repaint();
                    table.repaint();
                }


            });

            mouseAdapter =
                    new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            if (e.getButton() == MouseEvent.BUTTON3) {//if (e.isPopupTrigger()) {
                                int row = table_commit.rowAtPoint(e.getPoint());
                                if (row >= 0 && row < table_commit.getRowCount()) {
                                    table_commit.setRowSelectionInterval(row, row);
                                } else {
                                    table_commit.clearSelection();
                                }

                                commitTableRow = row;
                                // 선택된 행에 대한 작업 수행
                                String filepath = new File((String) ((DefaultTableModel) table_commit.getModel()).getValueAt(row, 2)).getPath();

                                if (isGitRepository(new File(filepath))) {
                                    File file = getGitRepository(new File(filepath));


                                    try (Repository repo = Git.open(file).getRepository()) {

                                        Git git = new Git(repo);

                                        // git status 명령 실행
                                        Status status = git.status().call();


                                        Set<String> getRemovedset = status.getRemoved();
                                        Set<String> Removed = new HashSet<String>();
                                        Iterator<String> stringIter = getRemovedset.iterator();
                                        String path;
                                        while (stringIter.hasNext()) {
                                            String str = stringIter.next();
                                            path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                                            Removed.add(path);

                                        }
                                        currentPopupPath = filepath;

                                        if (Removed.contains(filepath)) {// removed 파일일 경우

                                            popupMenuRemoved_commit.show(e.getComponent(), e.getX(), e.getY());
                                        }


                                    } catch (IOException ex) {
                                        JOptionPane.showMessageDialog(null, "오류1");

                                    } catch (GitAPIException ex) {
                                        JOptionPane.showMessageDialog(null, "오류2");
                                    }
                                } else {
                                    JOptionPane.showMessageDialog(null, "깃으로 관리 중이지 않습니다.");
                                }
                            }
                        }
                    };
            table_commit.addMouseListener(mouseAdapter);


            //--------------------------------지훈이 코드발췌 끝------------------------------------------------

            int result = JOptionPane.showConfirmDialog(null, panel, "Commit message", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            // 확인 버튼을 눌렀을 때의 동작
            if (result == JOptionPane.OK_OPTION) {
                String message = textField.getText();

                // Confirm 팝업창 생성
                result = JOptionPane.showConfirmDialog(null, "Are you sure you want to commit this file?\n" + "The message is : " + message, "Yes", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (result == JOptionPane.YES_OPTION) {
                    // Yes를 눌렀을 때의 동작
                    JOptionPane.showMessageDialog(null, "Commited successfully!", "알림", JOptionPane.INFORMATION_MESSAGE);
                    // Git commit 구현하기
                    git.commit().setMessage(message).call();
                } else {
                    // No를 눌렀을 때의 동작
                    JOptionPane.showMessageDialog(null, "Commit cancelled", "알림", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } else {
            String msg = "There are no staged files";
            showErrorMessage(msg, "Commit Error");
        }
        Status status = git.status().call();
        fileTableModel.setGit(status, currentPath);
        table.repaint();
    }

    private void deleteFile() {
        if (currentFile == null) {
            showErrorMessage("No file selected for deletion.", "Select File");
            return;
        }

        int result =
                JOptionPane.showConfirmDialog(
                        gui,
                        "Are you sure you want to delete this file?",
                        "Delete File",
                        JOptionPane.ERROR_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                System.out.println("currentFile: " + currentFile);
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                System.out.println("parentPath: " + parentPath);
                DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                System.out.println("parentNode: " + parentNode);

                boolean directory = currentFile.isDirectory();
                if (FileUtils.deleteQuietly(currentFile)) {
                    if (directory) {
                        // delete the node..
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode =
                                (DefaultMutableTreeNode) currentPath.getLastPathComponent();

                        treeModel.removeNodeFromParent(currentNode);
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" + currentFile + "' could not be deleted.";
                    showErrorMessage(msg, "Delete Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }

    //3. refresh 함수 구현
    public void refresh() throws GitAPIException {

        try {
            boolean directory = currentFile.isDirectory();

            if (directory) {
                TreePath currentPath = findTreePath(currentFile);
                // System.out.println(currentPath);
                DefaultMutableTreeNode currentNode =
                        (DefaultMutableTreeNode) currentPath.getLastPathComponent();

                //treeModel.removeNodeFromParent(currentNode);
                showChildren(currentNode);
            }
        } catch (Throwable t) {
            showThrowable(t);
        }


        Status status = git.status().call();
        fileTableModel.setGit(status, currentPath);

        table.repaint();

    }


    //4. restore_missed 함수 구현

    private void restoremissed() throws GitAPIException {

        Status status_missed = git.status().call();
        Set<String> missingSet = status_missed.getMissing();
        if (!missingSet.isEmpty()) { //Modified : Deleted가 있어야 실행

            Set<String> removed = new HashSet<String>();
            Iterator<String> stringIter4 = missingSet.iterator();
            while (stringIter4.hasNext()) {
                String str = stringIter4.next();
                try {

                    CheckoutCommand checkout = git.checkout();
                    checkout.addPath(str);
                    checkout.call();
                    //System.out.println("here Missed, "+str);

                } catch (Exception e) {

                }

            } //removed의 while 끝
        }

        else {
            JOptionPane.showMessageDialog(null, "there are no Modified:deleted");
        }

        refresh();

    }

    //5. git clone 테스트용도 곽수정

    private void gitclone(String localPath) throws GitAPIException, IOException {



        new GitClone(localPath);
        git=Git.init().setDirectory(currentPath).call(); //refresh를 구현하기위해 FileManager의 attribute인 git을 초기화할 필요가 있음, 로직상 git clone은 non-git폴더를 왼쪽 트리에서 고르면서 시작되므로 git이 초기화되어있지 않음. 임시로 초기화  곽수정
        refresh();
    }




    private void newFile() {
        if (currentFile == null) {
            showErrorMessage("No location selected for new file.", "Select Location");
            return;
        }

        if (newFilePanel == null) {
            newFilePanel = new JPanel(new BorderLayout(3, 3));

            JPanel southRadio = new JPanel(new GridLayout(1, 0, 2, 2));
            newTypeFile = new JRadioButton("File", true);
            JRadioButton newTypeDirectory = new JRadioButton("Directory");
            ButtonGroup bg = new ButtonGroup();
            bg.add(newTypeFile);
            bg.add(newTypeDirectory);
            southRadio.add(newTypeFile);
            southRadio.add(newTypeDirectory);

            name = new JTextField(15);

            newFilePanel.add(new JLabel("Name"), BorderLayout.WEST);
            newFilePanel.add(name);
            newFilePanel.add(southRadio, BorderLayout.SOUTH);
        }

        int result =
                JOptionPane.showConfirmDialog(
                        gui, newFilePanel, "Create File", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                boolean created;
                File parentFile = currentFile;
                if (!parentFile.isDirectory()) {
                    parentFile = parentFile.getParentFile();
                }
                File file = new File(parentFile, name.getText());
                if (newTypeFile.isSelected()) {
                    created = file.createNewFile();
                } else {
                    created = file.mkdir();
                }
                if (created) {

                    TreePath parentPath = findTreePath(parentFile);
                    DefaultMutableTreeNode parentNode =
                            (DefaultMutableTreeNode) parentPath.getLastPathComponent();

                    if (file.isDirectory()) {
                        // add the new node..
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(file);

                        TreePath currentPath = findTreePath(currentFile);
                        DefaultMutableTreeNode currentNode =
                                (DefaultMutableTreeNode) currentPath.getLastPathComponent();

                        treeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" + file + "' could not be created.";
                    showErrorMessage(msg, "Create Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }

    private void showErrorMessage(String errorMessage, String errorTitle) {
        JOptionPane.showMessageDialog(gui, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
    }

    private void showThrowable(Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(gui, t.toString(), t.getMessage(), JOptionPane.ERROR_MESSAGE);
        gui.repaint();
    }

    /**
     * Update the table on the EDT
     */
    private void setTableData(final File[] files) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        if (fileTableModel == null) {
                            fileTableModel = new FileTableModel();
                            table.setModel(fileTableModel); //UI인 JTable을 내부구현하는 fileTableModel 지정
                        }
                        table.getSelectionModel()
                                .removeListSelectionListener(listSelectionListener);
                        fileTableModel.setFiles(files);
                        if (!cellSizesSet) {
                            Icon icon = fileSystemView.getSystemIcon(files[0]); //icon을 실제로 설정하기보다는 바로 아랫줄과 함께 file icon을 표현하기 적당한 크기설정에 사용됨.

                            // size adjustment to better account for icons
                            table.setRowHeight(icon.getIconHeight() + rowIconPadding);

                            setColumnWidth(0, 50);
                            setColumnWidth(3, 60);
                            table.getColumnModel().getColumn(3).setMaxWidth(120);
                            setColumnWidth(4, -1);
                            cellSizesSet = true;
                        }
                    }
                });
    }

    private void setColumnWidth(int column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        if (width < 0) {
            // use the preferred width of the header..
            JLabel label = new JLabel((String) tableColumn.getHeaderValue());
            Dimension preferred = label.getPreferredSize();
            // altered 10->14 as per camickr comment.
            width = (int) preferred.getWidth() + 14;
        }
        tableColumn.setPreferredWidth(width);
        tableColumn.setMaxWidth(width);
        tableColumn.setMinWidth(width);
    }

    /**
     * Add the files that are contained within the directory of this node. Thanks to Hovercraft Full
     * Of Eels.
     */
    private void showChildren(final DefaultMutableTreeNode node) {
        tree.setEnabled(false);
        progressBar.setVisible(false);
        progressBar.setIndeterminate(false);

        SwingWorker<Void, File> worker =
                new SwingWorker<Void, File>() {
                    @Override
                    public Void doInBackground() {
                        File file = (File) node.getUserObject();
                        if (file.isDirectory()) {
                            File[] files = fileSystemView.getFiles(file, true); // !!
                            if (node.isLeaf()) {
                                for (File child : files) {
                                    if (child.isDirectory()) {
                                        publish(child);
                                    }
                                }
                            }
                            setTableData(files); //table의 내부 값을 설정
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<File> chunks) {
                        for (File child : chunks) {
                            node.add(new DefaultMutableTreeNode(child));
                        }
                    }

                    @Override
                    protected void done() {
                        progressBar.setIndeterminate(false);
                        progressBar.setVisible(false);
                        tree.setEnabled(true);
                    }
                };
        worker.execute();
    }

    /**
     * Update the File details view with the details of this File.
     */
    private void setFileDetails(File file) { //우하단 회색영역의 설정
        currentFile = file;


        Icon icon = fileSystemView.getSystemIcon(file);
        fileName.setIcon(icon);
        fileName.setText(fileSystemView.getSystemDisplayName(file));

        path.setText(file.getPath());
        date.setText(new Date(file.lastModified()).toString());
        size.setText(file.length() + " bytes");


        JFrame f = (JFrame) gui.getTopLevelAncestor();
        if (f != null) {
            f.setTitle(APP_TITLE + " :: " + fileSystemView.getSystemDisplayName(file));
        }

        gui.repaint();
    }

    public static void main(String[] args) throws IOException, GitAPIException {

        String classpath = System.getProperty("java.class.path");


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

                        FileManager fileManager = new FileManager();
                        f.setContentPane(fileManager.getGui());

                        fileManager.setBranchName();

                        try {
                            URL urlBig = fileManager.getClass().getResource("fm-icon-32x32.png");
                            URL urlSmall = fileManager.getClass().getResource("fm-icon-16x16.png");
                            ArrayList<Image> images = new ArrayList<Image>();
                            images.add(ImageIO.read(urlBig));
                            images.add(ImageIO.read(urlSmall));
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
    }

    private void setBranchName() {
        if(isGitRepository(currentPath)){
            File file = getGitRepository(currentPath);
            Repository repo;
            try {

                repo = Git.open(file).getRepository();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Git git = new Git(repo);

            branchManagement = new BranchManagement();

            branchName.setText(branchManagement.BranchName(git, currentPath, repo));
        }
    }


    /**
     * A TableModel to hold File[].
     */
    class FileTableModel extends AbstractTableModel {

        private Status tablemodel_status;
        private File tablemodel_currentpath;
        private File[] files;
        private FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        private String[] columns = {
                "Icon", "File", "Path/name", "Size", "Last Modified"};


        FileTableModel() {
            this(new File[0]);
        }

        FileTableModel(File[] files) {
            this.files = files;
        }

        public Object getValueAt(int row, int column) {

            File file = files[row];
            switch (column) {
                case 0:
                    //ystem.out.println("!!!!!!!!");
                    if (isGitRepository(tablemodel_currentpath)) { //checkFileStatusThreading 에서 setgit으로 넘겨받은 status, currentpath를 바탕으로 현재 경로가 git 폴더인지 파악

                        String filepath = file.getAbsolutePath();


                        Set<String> untrackedSet = tablemodel_status.getUntracked();
                        Set<String> unTracked = new HashSet<String>();
                        Iterator<String> stringIter = untrackedSet.iterator();
                        String path = null;
                        while (stringIter.hasNext()) {
                            String str = stringIter.next();
                            try {
                                path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                            } catch (Exception e) {

                            }
                            unTracked.add(path); // 1 2 3
                        }

                        Set<String> modifiedSet = tablemodel_status.getModified();
                        Set<String> modified = new HashSet<String>();
                        Iterator<String> stringIter1 = modifiedSet.iterator();
                        while (stringIter1.hasNext()) {
                            String str = stringIter1.next();
                            try {
                                path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                            } catch (Exception e) {

                            }
                            modified.add(path); // 1 2 3
                        }

                        Set<String> stagedSet = tablemodel_status.getChanged();
                        Set<String> staged = new HashSet<String>();
                        Iterator<String> stringIter2 = stagedSet.iterator();
                        while (stringIter2.hasNext()) {
                            String str = stringIter2.next();
                            try {
                                path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                            } catch (Exception e) {

                            }
                            staged.add(path);
                        }
                        Set<String> addSet = tablemodel_status.getAdded();
                        Set<String> added = new HashSet<String>();
                        Iterator<String> stringIter3 = addSet.iterator();
                        while (stringIter3.hasNext()) {
                            String str = stringIter3.next();
                            try {
                                path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                            } catch (Exception e) {

                            }
                            added.add(path);
                        }
                        Set<String> removeSet = tablemodel_status.getRemoved();
                        Set<String> removed = new HashSet<String>();
                        Iterator<String> stringIter4 = removeSet.iterator();
                        while (stringIter4.hasNext()) {
                            String str = stringIter4.next();
                            try {
                                path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                            } catch (Exception e) {

                            }
                            removed.add(path);
                        }

                        Set<String> missingSet = tablemodel_status.getMissing();
                        Set<String> missing = new HashSet<String>();
                        Iterator<String> stringIter5 = missingSet.iterator();
                        while (stringIter5.hasNext()) {
                            String str = stringIter5.next();
                            try {
                                path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                            } catch (Exception e) {

                            }
                            missing.add(path);
                        }

                        Set<String> untrackedfolderSet = tablemodel_status.getUntrackedFolders();
                        Set<String> untrackedfolder = new HashSet<String>();
                        Iterator<String> stringIter6 = untrackedfolderSet.iterator();
                        while (stringIter6.hasNext()) {
                            String str = stringIter6.next();
                            try {
                                path = OsUtils.getAbsolutePathByOs(getGitRepository(file).getParentFile().getAbsolutePath() + "\\" + str.replace('/', '\\'));
                            } catch (Exception e) {

                            }
                            untrackedfolder.add(path);
                        }
/*
                        System.out.println("-----------------------------------------------------");
                        System.out.println("Added: " + tablemodel_status.getAdded());
                        System.out.println("Changed: " + tablemodel_status.getChanged());
                        System.out.println("Conflicting: " + tablemodel_status.getConflicting());
                        System.out.println("ConflictingStageState: " + tablemodel_status.getConflictingStageState());
                        System.out.println("IgnoredNotInIndex: " + tablemodel_status.getIgnoredNotInIndex());
                        System.out.println("Missing: " + tablemodel_status.getMissing());
                        System.out.println("Modified: " + tablemodel_status.getModified());
                        System.out.println("Removed: " + tablemodel_status.getRemoved());
                        System.out.println("Untracked: " + tablemodel_status.getUntracked());
                        System.out.println("UntrackedFolders: " + tablemodel_status.getUntrackedFolders());
*/

                        if (unTracked.contains(filepath) || untrackedfolder.contains(filepath)) {
                            Icon icon = fileSystemView.getSystemIcon(file);

                            BufferedImage image = new BufferedImage(
                                    icon.getIconWidth(),
                                    icon.getIconHeight(),
                                    BufferedImage.TYPE_INT_ARGB
                            );
                            Graphics g = image.createGraphics();

// paint the Icon to the BufferedImage
                            icon.paintIcon(null, g, 0, 0);
                            g.dispose();

// resize the image
                            Image scaledImage = image.getScaledInstance(50, 50, Image.SCALE_SMOOTH);

// create a new ImageIcon
                            ImageIcon imageIcon = new ImageIcon(scaledImage);

                            ImageIcon icon_git = new ImageIcon(getClass().getClassLoader().getResource("untracked.png")); //git 상태 아이콘 준비

                            return createOverlayedImageIcon(imageIcon, icon_git);


                        } else if (modified.contains(filepath)) {
                            Icon icon = fileSystemView.getSystemIcon(file);

                            BufferedImage image = new BufferedImage(
                                    icon.getIconWidth(),
                                    icon.getIconHeight(),
                                    BufferedImage.TYPE_INT_ARGB
                            );
                            Graphics g = image.createGraphics();

// paint the Icon to the BufferedImage
                            icon.paintIcon(null, g, 0, 0);
                            g.dispose();

// resize the image
                            Image scaledImage = image.getScaledInstance(50, 50, Image.SCALE_SMOOTH);

// create a new ImageIcon
                            ImageIcon imageIcon = new ImageIcon(scaledImage);

                            ImageIcon icon_git = new ImageIcon(getClass().getClassLoader().getResource("modified.png")); //git 상태 아이콘 준비

                            return createOverlayedImageIcon(imageIcon, icon_git);
                        } else if (staged.contains(filepath)) {
                            Icon icon = fileSystemView.getSystemIcon(file);
                            BufferedImage image = new BufferedImage(
                                    icon.getIconWidth(),
                                    icon.getIconHeight(),
                                    BufferedImage.TYPE_INT_ARGB
                            );
                            Graphics g = image.createGraphics();

// paint the Icon to the BufferedImage
                            icon.paintIcon(null, g, 0, 0);
                            g.dispose();

// resize the image
                            Image scaledImage = image.getScaledInstance(50, 50, Image.SCALE_SMOOTH);

// create a new ImageIcon
                            ImageIcon imageIcon = new ImageIcon(scaledImage);

                            ImageIcon icon_git = new ImageIcon(getClass().getClassLoader().getResource("staged.png")); //git 상태 아이콘 준비

                            return createOverlayedImageIcon(imageIcon, icon_git);

                        } else if (added.contains(filepath)) {

                            Icon icon = fileSystemView.getSystemIcon(file);
                            BufferedImage image = new BufferedImage(
                                    icon.getIconWidth(),
                                    icon.getIconHeight(),
                                    BufferedImage.TYPE_INT_ARGB
                            );
                            Graphics g = image.createGraphics();

// paint the Icon to the BufferedImage
                            icon.paintIcon(null, g, 0, 0);
                            g.dispose();

// resize the image
                            Image scaledImage = image.getScaledInstance(50, 50, Image.SCALE_SMOOTH);

// create a new ImageIcon
                            ImageIcon imageIcon = new ImageIcon(scaledImage);

                            ImageIcon icon_git = new ImageIcon(getClass().getClassLoader().getResource("staged.png")); //git 상태 아이콘 준비

                            return createOverlayedImageIcon(imageIcon, icon_git);


                        } else if (removed.contains(filepath)) {
                            Icon icon = fileSystemView.getSystemIcon(file);
                            BufferedImage image = new BufferedImage(
                                    icon.getIconWidth(),
                                    icon.getIconHeight(),
                                    BufferedImage.TYPE_INT_ARGB
                            );
                            Graphics g = image.createGraphics();

// paint the Icon to the BufferedImage
                            icon.paintIcon(null, g, 0, 0);
                            g.dispose();

// resize the image
                            Image scaledImage = image.getScaledInstance(50, 50, Image.SCALE_SMOOTH);

// create a new ImageIcon
                            ImageIcon imageIcon = new ImageIcon(scaledImage);

                            ImageIcon icon_git = new ImageIcon(getClass().getClassLoader().getResource("removed.png")); //git 상태 아이콘 준비

                            return createOverlayedImageIcon(imageIcon, icon_git);


                        } else if (missing.contains(filepath)) {
                            Icon icon = fileSystemView.getSystemIcon(file);
                            BufferedImage image = new BufferedImage(
                                    icon.getIconWidth(),
                                    icon.getIconHeight(),
                                    BufferedImage.TYPE_INT_ARGB
                            );
                            Graphics g = image.createGraphics();

// paint the Icon to the BufferedImage
                            icon.paintIcon(null, g, 0, 0);
                            g.dispose();

// resize the image
                            Image scaledImage = image.getScaledInstance(50, 50, Image.SCALE_SMOOTH);

// create a new ImageIcon
                            ImageIcon imageIcon = new ImageIcon(scaledImage);

                            ImageIcon icon_git = new ImageIcon(getClass().getClassLoader().getResource("missing.png")); //git 상태 아이콘 준비

                            return createOverlayedImageIcon(imageIcon, icon_git);

                        } else {

                            Icon icon = fileSystemView.getSystemIcon(file);
                            BufferedImage image = new BufferedImage(
                                    icon.getIconWidth(),
                                    icon.getIconHeight(),
                                    BufferedImage.TYPE_INT_ARGB
                            );
                            Graphics g = image.createGraphics();

// paint the Icon to the BufferedImage
                            icon.paintIcon(null, g, 0, 0);
                            g.dispose();

// resize the image
                            Image scaledImage = image.getScaledInstance(50, 50, Image.SCALE_SMOOTH);

// create a new ImageIcon
                            ImageIcon imageIcon = new ImageIcon(scaledImage);

                            ImageIcon icon_git = new ImageIcon(getClass().getClassLoader().getResource("unmodified,commited.png")); //git 상태 아이콘 준비

                            return createOverlayedImageIcon(imageIcon, icon_git);

                        }
                        //}
                    } else {

                        Icon icon = fileSystemView.getSystemIcon(file);
                        BufferedImage image = new BufferedImage(
                                icon.getIconWidth(),
                                icon.getIconHeight(),
                                BufferedImage.TYPE_INT_ARGB
                        );
                        Graphics g = image.createGraphics();

// paint the Icon to the BufferedImage
                        icon.paintIcon(null, g, 0, 0);
                        g.dispose();

// resize the image
                        Image scaledImage = image.getScaledInstance(50, 50, Image.SCALE_SMOOTH);

// create a new ImageIcon
                        ImageIcon imageIcon = new ImageIcon(scaledImage);

                        return imageIcon;


                    }


                case 1:
                    return fileSystemView.getSystemDisplayName(file);
                case 2:
                    return file.getPath();
                case 3:
                    return file.length();

                case 4:
                    return file.lastModified();
                case 5:
                    // return file.canRead(); //이하 4개는 우상단 테이블에서 RWEDF관련
                case 6:
                    //return file.canWrite();
                case 7:
                    // return file.canExecute();
                case 8:
                    // return file.isDirectory();
                case 9:
                    return file.isFile();
                default:
                    System.err.println("Logic Error");
            }


            return "";
        }


        public int getColumnCount() {
            return columns.length;
        }

        public Class<?> getColumnClass(int column) {
            switch (column) {
                case 0:
                    return ImageIcon.class;
                case 3:
                    return Long.class;
                case 4:
                    return Date.class;
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                    //return Boolean.class;
            }
            return String.class;
        }

        public String getColumnName(int column) {
            return columns[column];
        }

        public int getRowCount() {
            return files.length;
        }

        public File getFile(int row) {
            return files[row];
        }

        public void setFiles(File[] files) {
            this.files = files;
            fireTableDataChanged();
        }

        public void setGit(Status status, File currentPath) {

            tablemodel_status = status;
            tablemodel_currentpath = currentPath;
        }

        public void setPath(File currentPath) {
            tablemodel_currentpath = currentPath;
        }

        private ImageIcon createOverlayedImageIcon(ImageIcon baseIcon, ImageIcon gitIcon) {
            Image baseImage = baseIcon.getImage();
            Image overlayImage = gitIcon.getImage();

            int width = baseImage.getWidth(null);
            int height = baseImage.getHeight(null);

            int overlayWidth = overlayImage.getWidth(null) / 32;
            int overlayHeight = overlayImage.getHeight(null) / 32;

            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.drawImage(baseImage, 0, 0, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
            g2d.drawImage(overlayImage, width - overlayWidth, height - overlayHeight, overlayWidth, overlayHeight, null);
            g2d.dispose();

            return new ImageIcon(bufferedImage);
        }
    }

} //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


/**
 * A TreeCellRenderer for a File.
 */
class FileTreeCellRenderer extends DefaultTreeCellRenderer {

    private FileSystemView fileSystemView;

    private JLabel label;

    FileTreeCellRenderer() {
        label = new JLabel();
        label.setOpaque(true);
        fileSystemView = FileSystemView.getFileSystemView();
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {
        //시작 노드를 루트폴더로 설정
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        File file = (File) node.getUserObject();
        label.setIcon(fileSystemView.getSystemIcon(file));
        label.setText(fileSystemView.getSystemDisplayName(file));
        label.setToolTipText(file.getPath());

        if (selected) {
            label.setBackground(backgroundSelectionColor);
            label.setForeground(textSelectionColor);
        } else {
            label.setBackground(backgroundNonSelectionColor);
            label.setForeground(textNonSelectionColor);
        }

        return label;
    }
}

