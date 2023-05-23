package com.github.filemanager;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BranchList extends JFrame {
    public BranchList(Git git, int which_btn) throws GitAPIException {
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

        JButton select_btn = new JButton("select");
        panel.add(select_btn);

        select_btn.addActionListener( // select btn 눌렀을 시에 이벤트
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int row = branch_list_table.getSelectedRow();
                        int col = branch_list_table.getSelectedColumn();
                        Object value = branch_list_table.getValueAt(row, col);

                        System.out.println("selected branch:" + value); // 버튼 누르면 선택한 값 출력

                        // 위 값을 각자의 클래스에 전달해서 잘 처리해주는 식으로 구현하기
                        // ex) 해솔이 같은 경우에는 선택한 값 BranchManagement로 가져가서 사용
                        // ex) 하빈이 같은 경우에는 선택한 값 BranchMerge로 가져가서 사용


                        if(which_btn == 0){ // Delete

                        }
                        else if(which_btn == 1){ // Rename

                        }
                        else if(which_btn == 2){ // Checkout

                        }
                        else if(which_btn == 3){ // Merge
                            boolean is_successful_merge;
                            try {
                                is_successful_merge = BranchMerge.branchMerge(git, (String) value);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            } catch (GitAPIException ex) {
                                throw new RuntimeException(ex);
                            }
                            if(is_successful_merge){ //merge가 정상적으로 수행되면 창 종료
                                dispose(); // 창 종료
                            }
                        }


                    }
                }
        );

        this.add(panel);

        System.out.println("Listing local branches:");
    }
}