package com.github.filemanager;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BranchList extends JFrame {
    public BranchList(Git git) throws GitAPIException {
        super("Branch List");
        this.setSize(500, 500);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setVisible(true);
        this.setResizable(false);
        JPanel panel = new JPanel(new FlowLayout()); // panel 생성
        JTable branch_list_table = new JTable(new DefaultTableModel(new Object[]{"Branch Name"}, 0) { // branch_list_table 생성
            public Class<?> getColumnClass(int column) {
                return Object.class;
            }
        });

//        table_commit.setRowHeight(10 + rowIconPadding);//commit 테이블의 행 높이 조정

       // DefaultTableModel model = (DefaultTableModel) branch_list_table.getModel();


//        for (String stagedFile : stagedSet) {
//            File stagedFILE = new File(stagedFile);
//            if (status.getAdded().contains(stagedFile) || status.getChanged().contains(stagedFile)) { //added, changed는 모두 파일이 추가되거나, 변경된 상태에서 staged된것.
//
//                ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("staged.png"));
//                Image image = icon.getImage();
//                Image newimg = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
//
//
//                Object[] rowData = {new ImageIcon(newimg), stagedFILE.getName(), OsUtils.getAbsolutePathByOs(temp + "\\" + stagedFILE.getName())}; // 첫 번재 값은 아이콘(영헌이가 추가해야함), 두 번째는 파일 이름, // 세 번째는 파일의 절대 경로 (최상단 부모 깃 절대경로 + 파일의 상대경로)
//                model.addRow(rowData);
//
//
//            } else { //removed =deleted
//
//                ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("removed.png"));
//                Image image = icon.getImage();
//                Image newimg1 = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
//
//                Object[] rowData = {new ImageIcon(newimg1), stagedFILE.getName(), OsUtils.getAbsolutePathByOs(temp + "\\" + stagedFILE.getName())}; // 첫 번재 값은 아이콘(영헌이가 추가해야함), 두 번째는 파일 이름, // 세 번째는 파일의 절대 경로 (최상단 부모 깃 절대경로 + 파일의 상대경로)
//                model.addRow(rowData);
//            }
//
//
//        }

        JScrollPane scrollpane = new JScrollPane(branch_list_table); // branch_list_table의 JScrollPane 생성
        //this.add(scrollpane , BorderLayout.CENTER);
        panel.add(scrollpane);

        panel.setPreferredSize(new Dimension(100, 100)); // panel 크기 조절
        this.add(panel);

        System.out.println("Listing local branches:");
        List<Ref> call = git.branchList().call();
        for (Ref ref : call) {
            System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
        }
    }
}
