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

            @Override
            public boolean isCellEditable(int row, int column) { // 테이블 내용 수정 못하게 한다.
                return false;
            }
        });
        DefaultTableModel model = (DefaultTableModel) branch_list_table.getModel();

        List<Ref> call = git.branchList().call();
        for (Ref branch_info : call) {
                System.out.println("Branch: " + branch_info + " " + branch_info.getName() + " " + branch_info.getObjectId().getName());
                Object[] rowData = {branch_info.getName()}; // 첫 번재 값은 아이콘(영헌이가 추가해야함), 두 번째는 파일 이름, // 세 번째는 파일의 절대 경로 (최상단 부모 깃 절대경로 + 파일의 상대경로)
                model.addRow(rowData);
        }

        //branch_list_table.setEnabled(false);
        JScrollPane scrollpane = new JScrollPane(branch_list_table); // branch_list_table의 JScrollPane 생성
        //this.add(scrollpane , BorderLayout.CENTER);
        panel.add(scrollpane);

        panel.setPreferredSize(new Dimension(100, 100)); // panel 크기 조절
        this.add(panel);

        System.out.println("Listing local branches:");
    }
}
