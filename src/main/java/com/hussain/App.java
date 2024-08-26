package com.hussain;

import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.slf4j.*;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {

        String repositoryPath = ".";
        Git git = GitUtility.openRepository(repositoryPath);

        if (args.length < 1) {
            logger.error("No command provided. Usage: java App <command> [options]");
            return;
        }

        String command = args[0];

        switch (command) {
            case "init":
                if (args.length < 2) {
                    logger.error("Repository path required for init command.");
                    return;
                }
                String initPath = args[1];
                GitUtility.initializeRepository(initPath);
                break;

            case "add":
                if (args.length < 2) {
                    logger.error("File paths required for add command.");
                    return;
                }
                Set<String> filesToAdd = Set.of(args[1].split(","));
                GitUtility.addFiles(git, filesToAdd);
                break;

            case "commit":
                if (args.length < 3) {
                    logger.error("File paths and commit message required for commit command.");
                    return;
                }
                Set<String> filesToCommit = Set.of(args[1].split(","));
                String commitMessage = args[2];
                GitUtility.commitChanges(git, filesToCommit, commitMessage);
                break;

            case "push":
                if (args.length < 3) {
                    logger.error("Remote repo URL and token required for push command.");
                    return;
                }
                String remoteRepoUrl = args[1];
                String token = args[2];
                GitUtility.pushChanges(git, remoteRepoUrl, token);
                break;

            case "status":
                Set<String> untrackedFiles = GitUtility.getUntrackedFiles(git);
                if (untrackedFiles != null && !untrackedFiles.isEmpty()) {
                    // logger.info("Untracked files: {}", untrackedFiles);
                    System.out.println(untrackedFiles);
                }
                Set<String> trackedFiles = GitUtility.getTrackedFiles(git);
                if (trackedFiles != null && !trackedFiles.isEmpty()) {
                    // logger.info("Tracked files: {}", trackedFiles);
                    System.out.println(untrackedFiles);
                }
                break;

            case "resolve":
                GitUtility.resolveConflicts(git);
                break;

            default:
                logger.error("Unknown command: {}", command);
                logger.error("Available commands: init, add, commit, push, status, resolve");
                break;
        }
    }
}