package com.github.filemanager;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CommitHistoryGraph {
    public static void showCommitHistory_ver2() {
        try (Repository repository = getGitRepository()) {
            Iterable<RevCommit> commits = getCommits(repository);

            for (RevCommit commit : commits) {
                String commitId = commit.getId().abbreviate(7).name();
                String commitMessage = commit.getShortMessage();
                int parentCount = commit.getParentCount();

                StringBuilder graph = new StringBuilder();
                for (int i = 0; i < parentCount; i++) {
                    graph.append("│    ");
                }
                if (parentCount > 0) {
                    graph.append("├── ");
                }
                graph.append(commitId).append(" ").append(commitMessage);

                System.out.println(graph.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    private static Repository getGitRepository() throws IOException {
        File gitDir = new File("C:/Users/cango/Documents/gittesttest/.git");
        return Git.open(gitDir).getRepository();
    }

    private static Iterable<RevCommit> getCommits(Repository repository) throws IOException, GitAPIException {
        Git git = new Git(repository);
        LogCommand logCommand = git.log();
        logCommand.all();
        return logCommand.call();
    }
}