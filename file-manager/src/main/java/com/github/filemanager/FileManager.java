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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.*;
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
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.LineIterator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import static com.github.filemanager.GitUtilsForTrack.*;


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

    /** Title of the application */
    public static final String APP_TITLE = "FileMan";
    /** Used to open/edit/print files. */
    private Desktop desktop;

    /** Provides nice icons and names for files. */
    private FileSystemView fileSystemView;

    /** currently selected File. */
    private File currentFile;

    /** Main GUI container */
    private JPanel gui;

    /** File-system tree. Built Lazily */
    private JTree tree;

    private DefaultTreeModel treeModel;

    /** Directory listing */
    private JTable table;

    private JProgressBar progressBar;
    /** Table model for File[]. */
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

    // copyfile 기능 없앰
    // private JButton copyFile;

    //git init 추가, git commit  추가
   private JButton gitinit;
   private JButton gitcommit;

    private JLabel fileName;
    private JTextField path;
    private JLabel date;
    private JLabel size;

    /* 체크박스 기능 주석 처리함
    private JCheckBox readable;
    private JCheckBox writable;
    private JCheckBox executable;
    private JRadioButton isDirectory;
    private JRadioButton isFile;
    */

    /* GUI options/containers for new File/Directory creation.  Created lazily. */
    private JPanel newFilePanel;
    private JRadioButton newTypeFile;
    private JTextField name;

    private Git git; // FileManager에서 사용될 Git 변수
    private File currentPath; // 현재 JTree에서 가리키고 있는 디렉토리 정보

    public Container getGui() {
        if (gui == null) {
            gui = new JPanel(new BorderLayout(3, 3));
            gui.setBorder(new EmptyBorder(5, 5, 5, 5));

            fileSystemView = FileSystemView.getFileSystemView();
            desktop = Desktop.getDesktop();

            JPanel detailView = new JPanel(new BorderLayout(3, 3));
            // fileTableModel = new FileTableModel();

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
            table.setComponentPopupMenu(popupMenuUntracked);
            gitadd_Untracked.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "gitadd_Untracked clicked");
                }
            });

            //Modified를 위한 popup menu
            JPopupMenu popupMenuModified = new JPopupMenu();
            JMenuItem gitadd_Modified = new JMenuItem("git add");
            JMenuItem gitresotre_Modified = new JMenuItem("git restore");
            popupMenuModified.add(gitadd_Modified);
            popupMenuModified.add(gitresotre_Modified);
            table.setComponentPopupMenu(popupMenuModified);
            gitadd_Modified.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "gitadd_Modified clicked");
                }
            });
            gitresotre_Modified.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "gitresotre_Modified clicked");
                }
            });

            //staged(==Added)를 위한 popup menu
            JPopupMenu popupMenuStaged = new JPopupMenu();
            JMenuItem gitresotre_Staged = new JMenuItem("git restore --staged");
                //JMenuItem gitcommit_Staged = new JMenuItem("git commit -m"); 해솔&하빈이 버튼으로 구현
            popupMenuStaged.add(gitresotre_Staged);
                //popupMenuStaged.add(gitcommit_Staged);
            table.setComponentPopupMenu(popupMenuStaged);
            gitresotre_Staged.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "gitresotre_Staged clicked");
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
            table.setComponentPopupMenu(popupMenuCommited);
            gitrm_cached.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "git rm --cached clicked");
                }
            });

            gitrm.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "git rm clicked");
                }
            });
            gitmv.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "git mv clicked");
                }
            });

            //우클릭 처리 추가
            mouseAdapter=
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
                                //JOptionPane.showMessageDialog(null, row + "번째 행" + filepath);

                                if(isGitRepository(new File(filepath))){
                                    //JOptionPane.showMessageDialog(null, "깃으로 관리중입니다.");
                                    File file = getGitRepository(new File(filepath));
                                    //JOptionPane.showMessageDialog(null, file.getPath());

                                    try (Repository repo = Git.open(file).getRepository()) {
                                        //JOptionPane.showMessageDialog(null, "실행");
                                        Git git = new Git(repo);

                                        // git status 명령 실행
                                        Status status = git.status().call();

                                        //JOptionPane.showMessageDialog(null, "untracked" + status.getUntracked());
                                        //JOptionPane.showMessageDialog(null, "Modified" + status.getModified());
                                        //JOptionPane.showMessageDialog(null, "Added" + status.getAdded());
                                        //JOptionPane.showMessageDialog(null, "uncommit " + status.hasUncommittedChanges());
                                        //JOptionPane.showMessageDialog(null, status.getChanged());
                                        //JOptionPane.showMessageDialog(null, status.getMissing());
                                        //JOptionPane.showMessageDialog(null, status.getRemoved());

                                        Set<String> getUntrackedset = status.getUntracked();
                                        Set<String> unTrackted = new HashSet<String>();
                                        Iterator<String> stringIter = getUntrackedset.iterator();
                                        while(stringIter.hasNext()){
                                            String str = stringIter.next();
                                            unTrackted.add(getAbsolutePath(file.getParentFile() + "\\" + str.replace('/','\\'))); // 1 2 3
                                            //JOptionPane.showMessageDialog(null, file.getParentFile() + "\\"+ str.replace('/','\\'));
                                        }

                                        Set<String> getModifiedset = status.getModified();
                                        Set<String> Modified = new HashSet<String>();
                                        stringIter = getModifiedset.iterator();
                                        while(stringIter.hasNext()){
                                            String str = stringIter.next();
                                            Modified.add(getAbsolutePath(file.getParentFile() + "\\" + str.replace('/','\\'))); // 1 2 3
                                            //JOptionPane.showMessageDialog(null, file.getParentFile() + "\\"+ str.replace('/','\\'));
                                        }

                                        Set<String> getAddedset = status.getAdded();
                                        Set<String> Added = new HashSet<String>();
                                        stringIter = getAddedset.iterator();
                                        while(stringIter.hasNext()){
                                            String str = stringIter.next();
                                            Added.add(getAbsolutePath(file.getParentFile() + "\\" + str.replace('/','\\'))); // 1 2 3
                                            //JOptionPane.showMessageDialog(null, file.getParentFile() + "\\"+ str.replace('/','\\'));
                                        }
                                        //JOptionPane.showMessageDialog(null, filepath);
                                        if(unTrackted.contains(filepath)){// untracked 파일일 경우
                                            //JOptionPane.showMessageDialog(null, "untracked");
                                            popupMenuUntracked.show(e.getComponent(), e.getX(), e.getY());
                                        }
                                        else if(Modified.contains(filepath)){//Modified 파일인 경우
                                            //JOptionPane.showMessageDialog(null, "Modified");
                                            popupMenuModified.show(e.getComponent(), e.getX(), e.getY());
                                        }
                                        else if(Added.contains(filepath)){//Staged 파일인 경우
                                            //JOptionPane.showMessageDialog(null, "Staged");
                                            popupMenuStaged.show(e.getComponent(), e.getX(), e.getY());
                                        }
                                        else{ // commited 파일일 경우 == untracked modified added 모두 아닐 경우
                                            //JOptionPane.showMessageDialog(null, "Commited");
                                            popupMenuCommited.show(e.getComponent(), e.getX(), e.getY());
                                        }



                                    } catch (IOException ex) {
                                        JOptionPane.showMessageDialog(null, "오류1");

                                    } catch (GitAPIException ex) {
                                        JOptionPane.showMessageDialog(null, "오류2");                                }
                                    }
                                else{
                                    JOptionPane.showMessageDialog(null, "깃으로 관리 중이지 않습니다.");
                                }
                            }
                        }
                    };
            table.addMouseListener(mouseAdapter);

            // the File tree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            treeModel = new DefaultTreeModel(root);

            // 왼쪽 디렉토리 JTree에서 특정 디렉토리를 선택했을 때 발생하는 이벤트
            TreeSelectionListener treeSelectionListener =
                    new TreeSelectionListener() {
                        public void valueChanged(TreeSelectionEvent tse) {
                            DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) tse.getPath().getLastPathComponent();
                            showChildren(node);
                            setFileDetails((File) node.getUserObject());

                            currentPath = (File) node.getUserObject();

                            // 특정 디렉토리를 누를 때마다 그 선택된 디렉토리 정보를 가져온다
                            File selected_file = (File) node.getUserObject();
                            if(GitUtilsForTrack.isGitRepository(selected_file)){ // 만약 git에 의해 관리되는 저장소라면
                                gitinit.setEnabled(false); // git init과
                                gitcommit.setEnabled(true); // git commit 버튼을 disable하고
                                try {
                                    git = Git.open(getGitRepository(selected_file)); // git 저장소를 연다.
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            else{ // git에 의해 관리되지 않는 저장소라면
                                gitinit.setEnabled(true); // git init 과
                                gitcommit.setEnabled(false); // git commit 버튼을 활성화한다.
                            }
                        }
                    };

            // show the file system roots. 왼쪽 루트 구조
            File[] roots = fileSystemView.getRoots();
            for (File fileSystemRoot : roots) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
                root.add(node);
                // showChildren(node);
                //
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

            /* 아래쪽 버튼 옆에 있던 체크박스 삭제
            JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
            isDirectory = new JRadioButton("Directory");
            isDirectory.setEnabled(false);
            flags.add(isDirectory);
            isFile = new JRadioButton("File");
            isFile.setEnabled(false);
            flags.add(isFile);
            fileDetailsValues.add(flags);
            */

            int count = fileDetailsLabels.getComponentCount();
            for (int ii = 0; ii < count; ii++) {
                fileDetailsLabels.getComponent(ii).setEnabled(false);
            }

            JToolBar toolBar = new JToolBar();
            // mnemonics stop working in a floated toolbar
            toolBar.setFloatable(false);

            // 아래쪽 버튼 생성
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

            //2. Edit
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


            /* Copy 버튼 삭제
            copyFile = new JButton("Copy");
            copyFile.setMnemonic('c');
            copyFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            showErrorMessage("'Copy' not implemented.", "Not implemented.");
                        }
                    });
            toolBar.add(copyFile);

             */

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
                           /* File directory = new File(".");
                            if(isGitRepository(directory)){//(1) 현재 폴더가 git으로 관리되는 경우
                                //버튼 누르면 = gitInit();
                            }
                            else{//(2) 현재 폴더가 git으로 관리되고 있지 않은 경우
                                //버튼 안보임
                            }


                            */
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


            /* 버튼 옆에 있던 체크 박스 삭제
            readable = new JCheckBox("Read  ");
            readable.setMnemonic('a');
            // readable.setEnabled(false);
            toolBar.add(readable);

            writable = new JCheckBox("Write  ");
            writable.setMnemonic('w');
            // writable.setEnabled(false);
            toolBar.add(writable);

            executable = new JCheckBox("Execute");
            executable.setMnemonic('x');
            // executable.setEnabled(false);
            toolBar.add(executable);
             */

            JPanel fileView = new JPanel(new BorderLayout(3, 3)); //fileView는 우하단 회색영역 전체를 말한다.

            fileView.add(toolBar, BorderLayout.NORTH);
            fileView.add(fileMainDetails, BorderLayout.CENTER);

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
                        System.out.println(currentPath);
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
    private void gitInit() {
        //git init 실행하는 코드 작성

    }

    //2. Git commit 구현
    private void gitCommit() throws IOException, GitAPIException {
        if(git.status().call().getAdded().size() > 0) { //staged인 파일이 1개 이상 존재하는 경우
            //표에 나타내기
            JPanel panel = new JPanel(new BorderLayout());
            JTextField textField = new JTextField();
            JLabel messageLabel = new JLabel(" Please enter your commit message. : \n");
            JPanel bottomPanel = new JPanel(new BorderLayout());
            //빈 테이블에 staged 된 파일 불러오기
            JTable table = new JTable(new DefaultTableModel(new Object[]{"Icon", "File", "Path/name"}, 0));

            Status status = git.status().call();
            Set<String> stagedSet = status.getAdded();

            // 각 테이블에 staged 파일 목록 불러와서 추가하는 부분
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            for(String stagedFile : stagedSet){
                File stagedFILE = new File(stagedFile);

                Object[] rowData = {null, stagedFILE.getName(), stagedFILE.getAbsolutePath()}; // 첫 번재 값은 아이콘(영헌이가 추가해야함), 두 번째는 파일 이름, // 세 번째는 파일의 절대 경로 (최상단 부모 깃 절대경로 + 파일의 상대경로)
                model.addRow(rowData);
            }

            panel.add(new JScrollPane(table), BorderLayout.CENTER);

            bottomPanel.add(messageLabel, BorderLayout.WEST);
            bottomPanel.add(textField, BorderLayout.CENTER);

            panel.add(bottomPanel, BorderLayout.SOUTH);
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
        }
        else{
            String msg = "There are no staged files";
            showErrorMessage(msg, "Commit Error");
        }
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

    /** Update the table on the EDT */
    private void setTableData(final File[] files) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        if (fileTableModel == null) {
                            fileTableModel = new FileTableModel();
                            table.setModel(fileTableModel);
                        }
                        table.getSelectionModel()
                                .removeListSelectionListener(listSelectionListener);
                        fileTableModel.setFiles(files);
                        table.getSelectionModel().addListSelectionListener(listSelectionListener);
                        if (!cellSizesSet) {
                            Icon icon = fileSystemView.getSystemIcon(files[0]);

                            // size adjustment to better account for icons
                            table.setRowHeight(icon.getIconHeight() + rowIconPadding);

                            setColumnWidth(0, -1);
                            setColumnWidth(3, 60);
                            table.getColumnModel().getColumn(3).setMaxWidth(120);
                            setColumnWidth(4, -1);
                            //setColumnWidth(5, -1); //우상단 테이블의 RWEDF관련 열 너비 설정
                           // setColumnWidth(6, -1);
                           // setColumnWidth(7, -1);
                          //  setColumnWidth(8, -1);
                           // setColumnWidth(9, -1);

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
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

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
                            setTableData(files);
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

    /** Update the File details view with the details of this File. */
    private void setFileDetails(File file) {
        currentFile = file;
        Icon icon = fileSystemView.getSystemIcon(file);
        fileName.setIcon(icon);
        fileName.setText(fileSystemView.getSystemDisplayName(file));
        path.setText(file.getPath());
        date.setText(new Date(file.lastModified()).toString());
        size.setText(file.length() + " bytes");
         /* 파일 디테일 부분 체크박스 삭제
        readable.setSelected(file.canRead());
        writable.setSelected(file.canWrite());
        executable.setSelected(file.canExecute());
        isDirectory.setSelected(file.isDirectory());
        isFile.setSelected(file.isFile());
        */


        JFrame f = (JFrame) gui.getTopLevelAncestor();
        if (f != null) {
            f.setTitle(APP_TITLE + " :: " + fileSystemView.getSystemDisplayName(file));
        }

        gui.repaint();
    }


    /*
    Copy 기능 삭제
    public static boolean copyFile(File from, File to) throws IOException {
        boolean created = to.createNewFile();
        if (created) {
            FileChannel fromChannel = null;
            FileChannel toChannel = null;
            try {
                fromChannel = new FileInputStream(from).getChannel();
                toChannel = new FileOutputStream(to).getChannel();
                toChannel.transferFrom(fromChannel, 0, fromChannel.size());
                // set the flags of the to the same as the from
                to.setReadable(from.canRead());
                to.setWritable(from.canWrite());
                to.setExecutable(from.canExecute());
            } finally {
                if (fromChannel != null) {
                    fromChannel.close();
                }
                if (toChannel != null) {
                    toChannel.close();
                }
                return false;
            }
        }
        return created;
    }
    */

    // 파일 상태 지속적으로 확인해주는 SwingWorker
    private void checkFileStatusThreading() {
        SwingWorker<Void, File> worker =
                new SwingWorker<Void, File>() {
                    @Override
                    public Void doInBackground() throws InterruptedException, GitAPIException {
                        while(true){
                            if(isGitRepository(currentPath)){
                                Status status = git.status().call();
                                Set<String> untrackedSet = status.getUntracked();
                                Set<String> modifiedSet = status.getModified();
                                Set<String> stagedSet = status.getAdded();

                                for ( int i = 0; i < table.getRowCount(); i++){
                                    String filename = (String) table.getValueAt(i, 1); // 테이블을 순회하면서 파일 이름들을 살펴본다
                                    if(untrackedSet.contains(filename)){
                                        // 각 상태에 맞는 파일 아이콘으로 변경해주면 됨. 영헌이 부분
                                    }
                                    else if(modifiedSet.contains(filename)){
                                        // 각 상태에 맞는 파일 아이콘으로 변경해주면 됨. 영헌이 부분
                                    }
                                    else if(stagedSet.contains(filename)){
                                        // 각 상태에 맞는 파일 아이콘으로 변경해주면 됨. 영헌이 부분
                                    }
                                }
                                System.out.println(currentPath.getAbsoluteFile());
                                System.out.println("Untracked:" + untrackedSet);
                                System.out.println("Modified:" + modifiedSet);
                                System.out.println("Staged:" + stagedSet);
                                table.repaint();
                            }
                            // 외부 브라우저에서 파일들 변할때 즉각적 반영 필요하면 사용
//                            try {
//                                boolean directory = currentFile.isDirectory();
//
//                                if (directory) {
//                                    TreePath currentPath = findTreePath(currentFile);
//                                    System.out.println(currentPath);
//                                    DefaultMutableTreeNode currentNode =
//                                                (DefaultMutableTreeNode) currentPath.getLastPathComponent();
//
//                                    //treeModel.removeNodeFromParent(currentNode);
//                                    showChildren(currentNode);
//                                }
//                            } catch (Throwable t) {
//                                showThrowable(t);
//                            }
                            Thread.sleep(1000);
                        }
                    }
                };
        worker.execute();
    }
    public static void main(String[] args) throws IOException, GitAPIException {
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
                        fileManager.checkFileStatusThreading(); // 파일 상태 실시간 체크 SwingWorker
                    }
                });
    }
}

/** A TableModel to hold File[]. */
class FileTableModel extends AbstractTableModel {

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
                return fileSystemView.getSystemIcon(file);
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
                return Boolean.class;
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
}

/** A TreeCellRenderer for a File. */
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

