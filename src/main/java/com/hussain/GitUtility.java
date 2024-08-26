package com.hussain;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

/**
 * GitNewUtility provides utilities for interacting with a Git repository using
 * the JGit library.
 * It supports repository initialization, file staging, committing, pushing
 * changes, conflict resolution,
 * and OAuth2 authentication for GitHub.
 */
public class GitUtility {

    private static final Logger logger = LoggerFactory.getLogger(GitUtility.class);

    public static Git git;

    /**
     * Opens an existing Git repository at the specified path.
     *
     * @param repositoryPath The file path to the local Git repository.
     * @return The Git instance if the repository is opened successfully, otherwise
     *         null.
     */
    public static Git openRepository(String repositoryPath) {
        try {
            return Git.open(new File(repositoryPath));
        } catch (Exception e) {
            logger.error("Error opening repository: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Initializes a new Git repository at the specified local path.
     *
     * @param localRepoPath The file path to the directory where the new Git
     *                      repository will be initialized.
     */
    public static void initializeRepository(String localRepoPath) {
        try {
            InitCommand initCommand = Git.init();
            initCommand.setDirectory(new File(localRepoPath));
            initCommand.call();
            logger.info("Initialized empty Git repository at {}", localRepoPath);
        } catch (Exception e) {
            logger.error("Error initializing repository: {}", e.getMessage());
        }
    }

    /**
     * Adds specified files to the Git index (staging area).
     *
     * @param git       The Git instance representing the repository.
     * @param filePaths A set of file paths to be added to the index.
     */
    public static void addFiles(Git git, Set<String> filePaths) {
        try {
            AddCommand add = git.add();
            for (String filePath : filePaths) {
                add.addFilepattern(filePath);
            }
            add.call();
            logger.info("Files added to the index successfully!");
        } catch (Exception e) {
            logger.error("Error adding files to the index: {}", e.getMessage());
        }
    }

    /**
     * Adds specified files to the index and commits the changes with a specified
     * commit message.
     *
     * @param git           The Git instance representing the repository.
     * @param filePaths     A set of file paths to be committed.
     * @param commitMessage The commit message to use for this commit.
     */
    public static void commitChanges(Git git, Set<String> filePaths, String commitMessage) {
        try {
            AddCommand add = git.add();
            for (String filePath : filePaths) {
                add.addFilepattern(filePath);
            }
            add.call();
            logger.info("Files added to the index successfully!");

            CommitCommand commit = git.commit();
            commit.setMessage(commitMessage);
            commit.call();
            logger.info("Changes committed successfully!");
        } catch (Exception e) {
            logger.error("Error adding files to the index or committing changes: {}", e.getMessage());
        }
    }

    /**
     * Pushes local changes to the remote Git repository using an authentication
     * token.
     *
     * @param git           The Git instance representing the repository.
     * @param remoteRepoUrl The URL of the remote repository.
     * @param token         The authentication token for GitHub.
     */
    public static void pushChanges(Git git, String remoteRepoUrl, String token) {
        try {
            URIish remoteURI = new URIish(remoteRepoUrl);
            PushCommand pushCommand = git.push();
            pushCommand.setRemote(remoteURI.toString())
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("", token)); // Use token for
                                                                                                 // authentication
            pushCommand.call();
            logger.info("Push successful");
        } catch (Exception e) {
            logger.error("Error pushing changes: {}", e.getMessage());
        }
    }

    /**
     * Retrieves a list of untracked files in the repository.
     *
     * @return A set of untracked file paths, or null if an error occurs.
     */
    public static Set<String> getUntrackedFiles(Git git) {
        Status status;
        try {
            status = git.status().call();
            Set<String> untrackedFiles = status.getUntracked();
            logger.info("Untracked files:");
            for (String untrackedFile : untrackedFiles) {
                System.out.println(untrackedFile);
            }
            return untrackedFiles;
        } catch (NoWorkTreeException | GitAPIException e) {
            logger.error("Error retrieving untracked files: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a list of tracked files that have been modified in the repository.
     *
     * @return A set of modified file paths, or null if an error occurs.
     */
    public static Set<String> getTrackedFiles(Git git) {
        Status status;
        try {
            status = git.status().call();
            System.out.println(git.status().call().getModified());
            Set<String> untrackedFiles = status.getModified();

            logger.info("Tracked files:");
            for (String untrackedFile : untrackedFiles) {
                logger.info(untrackedFile);
            }
            return untrackedFiles;
        } catch (NoWorkTreeException | GitAPIException e) {
            logger.error("Error retrieving tracked files: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Handles conflict resolution in the repository, allowing the user to choose
     * between
     * current changes, incoming changes, or both.
     *
     * @param git The Git instance representing the repository.
     */
    public static void resolveConflicts(Git git) {
        try {
            // Get the status of the repository
            Status status = git.status().call();

            // Check if there are any unmerged paths (indicating conflicts)
            Set<String> conflicts = status.getConflicting();

            if (!conflicts.isEmpty()) {
                logger.info("Conflicts detected.");
                logger.info("Conflicted files:");

                // Print the conflicted files
                for (String conflictedFile : conflicts) {
                    System.out.println(conflictedFile);
                }

                try (// Resolving conflicts based on user input
                        Scanner scanner = new Scanner(System.in)) {
                    logger.info("Choose conflict resolution option:");
                    logger.info("1. Accept current changes");
                    logger.info("2. Accept incoming changes");
                    logger.info("3. Accept both changes");
                    int choice = scanner.nextInt();

                    switch (choice) {
                        case 1:
                            acceptCurrentChanges(git, conflicts);
                            break;
                        case 2:
                            acceptIncomingChanges(git, conflicts);
                            break;
                        case 3:
                            acceptBothChanges(git, conflicts);
                            break;
                        default:
                            logger.info("Invalid choice. No action taken.");
                            break;
                    }
                }
                logger.info("Conflicts resolved successfully.");
            } else {
                logger.info("No conflicts detected.");
            }

        } catch (GitAPIException e) {
            logger.error("Error resolving conflicts: {}", e.getMessage());
        }
    }

    /**
     * Accepts current changes during conflict resolution.
     *
     * @param git   The Git instance representing the repository.
     * @param files A set of files for which current changes will be accepted.
     * @throws GitAPIException If an error occurs during the operation.
     */
    private static void acceptCurrentChanges(Git git, Set<String> files) throws GitAPIException {
        CheckoutCommand checkoutCommand = git.checkout();
        for (String file : files) {
            checkoutCommand.addPath(file).setStage(CheckoutCommand.Stage.OURS).call();
        }
    }

    /**
     * Accepts incoming changes during conflict resolution.
     *
     * @param git   The Git instance representing the repository.
     * @param files A set of files for which incoming changes will be accepted.
     * @throws GitAPIException If an error occurs during the operation.
     */
    private static void acceptIncomingChanges(Git git, Set<String> files) throws GitAPIException {
        CheckoutCommand checkoutCommand = git.checkout();
        for (String file : files) {
            checkoutCommand.addPath(file).setStage(CheckoutCommand.Stage.THEIRS).call();
        }
    }

    /**
     * Accepts both current and incoming changes during conflict resolution.
     *
     * @param git   The Git instance representing the repository.
     * @param files A set of files for which both changes will be accepted.
     * @throws GitAPIException If an error occurs during the operation.
     */
    private static void acceptBothChanges(Git git, Set<String> files) throws GitAPIException {
        acceptCurrentChanges(git, files);
        acceptIncomingChanges(git, files);
    }

    /**
     * Initiates an OAuth2 flow to obtain an access token from GitHub.
     *
     * @param clientId     The GitHub OAuth client ID.
     * @param clientSecret The GitHub OAuth client secret.
     * @return An access token string if successful, otherwise null.
     */
    public static String createOAuth2Token(String clientId, String clientSecret) {
        try {
            String redirectUri = "http://localhost:8000/callback";
            String authorizationUrl = "https://github.com/login/oauth/authorize?client_id=" + clientId +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") +
                    "&scope=repo";
            System.out.println("Open this URL in a browser to authorize the application:");
            System.out.println(authorizationUrl);

            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/callback", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String[] params = query.split("&");
                String code = null;
                for (String param : params) {
                    if (param.startsWith("code=")) {
                        code = param.split("=")[1];
                        break;
                    }
                }

                if (code != null) {
                    String token = exchangeCodeForToken(code, clientId, clientSecret, redirectUri);
                    String response = "OAuth2 token: " + token;
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    System.out.println("Token received: " + token);
                    server.stop(0);
                }
            });
            server.start();

            Desktop.getDesktop().browse(new URI(authorizationUrl));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Exchanges the authorization code for an OAuth2 access token.
     *
     * @param code         The authorization code obtained from GitHub.
     * @param clientId     The GitHub OAuth client ID.
     * @param clientSecret The GitHub OAuth client secret.
     * @param redirectUri  The redirect URI for the OAuth callback.
     * @return An access token string if successful, otherwise null.
     */
    public static String exchangeCodeForToken(String code, String clientId, String clientSecret, String redirectUri) {
        try {
            URL url = new URL("https://github.com/login/oauth/access_token");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            String data = "client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&code=" + code +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8");

            OutputStream os = connection.getOutputStream();
            os.write(data.getBytes());
            os.flush();
            os.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject jsonObject = new JSONObject(response.toString());
            return jsonObject.getString("access_token");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
