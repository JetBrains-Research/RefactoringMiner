package org.refactoringminer.rm1;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.GitServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHistoryRefactoringMinerImpl implements GitHistoryRefactoringMiner {

	Logger logger = LoggerFactory.getLogger(GitHistoryRefactoringMinerImpl.class);
	private boolean analyzeMethodInvocations;
	private Set<RefactoringType> refactoringTypesToConsider = null;
	
	private static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {
		@Override
		public void write(int b) throws IOException {
		}
	};
	
	public GitHistoryRefactoringMinerImpl() {
		this(false);
	}

	private GitHistoryRefactoringMinerImpl(boolean analyzeMethodInvocations) {
		this.analyzeMethodInvocations = analyzeMethodInvocations;
		this.setRefactoringTypesToConsider(
			RefactoringType.RENAME_CLASS,
			RefactoringType.MOVE_CLASS,
			RefactoringType.MOVE_SOURCE_FOLDER,
			RefactoringType.RENAME_METHOD,
			RefactoringType.EXTRACT_OPERATION,
			RefactoringType.INLINE_OPERATION,
			RefactoringType.MOVE_OPERATION,
			RefactoringType.PULL_UP_OPERATION,
			RefactoringType.PUSH_DOWN_OPERATION,
			RefactoringType.MOVE_ATTRIBUTE,
			RefactoringType.PULL_UP_ATTRIBUTE,
			RefactoringType.PUSH_DOWN_ATTRIBUTE,
			RefactoringType.EXTRACT_INTERFACE,
			RefactoringType.EXTRACT_SUPERCLASS,
			RefactoringType.EXTRACT_AND_MOVE_OPERATION,
			RefactoringType.RENAME_PACKAGE
		);
	}

	public void setRefactoringTypesToConsider(RefactoringType ... types) {
		this.refactoringTypesToConsider = new HashSet<RefactoringType>();
		for (RefactoringType type : types) {
			this.refactoringTypesToConsider.add(type);
		}
	}
	
	private void detect(GitService gitService, Repository repository, final RefactoringHandler handler, Iterator<RevCommit> i) {
		int commitsCount = 0;
		int errorCommitsCount = 0;
		int refactoringsCount = 0;

		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		String projectName = projectFolder.getName();
		
		long time = System.currentTimeMillis();
		while (i.hasNext()) {
			RevCommit currentCommit = i.next();
			try {
				List<Refactoring> refactoringsAtRevision = detectRefactorings(gitService, repository, handler, projectFolder, currentCommit);
				refactoringsCount += refactoringsAtRevision.size();
				
			} catch (Exception e) {
				logger.warn(String.format("Ignored revision %s due to error", currentCommit.getId().getName()), e);
				errorCommitsCount++;
			}

			commitsCount++;
			long time2 = System.currentTimeMillis();
			if ((time2 - time) > 20000) {
				time = time2;
				logger.info(String.format("Processing %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
			}
		}

		handler.onFinish(refactoringsCount, commitsCount, errorCommitsCount);
		logger.info(String.format("Analyzed %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
	}

	protected List<Refactoring> detectRefactorings(GitService gitService, Repository repository, final RefactoringHandler handler, File projectFolder, RevCommit currentCommit) throws Exception {
		List<Refactoring> refactoringsAtRevision;
		String commitId = currentCommit.getId().getName();
		List<String> filesBefore = new ArrayList<String>();
		List<String> filesCurrent = new ArrayList<String>();
		Map<String, String> renamedFilesHint = new HashMap<String, String>();
		gitService.fileTreeDiff(repository, currentCommit, filesBefore, filesCurrent, renamedFilesHint, true);
		// If no java files changed, there is no refactoring. Also, if there are
		// only ADD's or only REMOVE's there is no refactoring
		if (!filesBefore.isEmpty() && !filesCurrent.isEmpty()) {
			// Checkout and build model for current commit
			gitService.checkout(repository, commitId);
			UMLModel currentUMLModel = createModel(projectFolder, filesCurrent);
			
			// Checkout and build model for parent commit
			String parentCommit = currentCommit.getParent(0).getName();
			gitService.checkout(repository, parentCommit);
			UMLModel parentUMLModel = createModel(projectFolder, filesBefore);
			
			// Diff between currentModel e parentModel
			refactoringsAtRevision = parentUMLModel.diff(currentUMLModel, renamedFilesHint).getRefactorings();
			refactoringsAtRevision = filter(refactoringsAtRevision);
			
		} else {
			//logger.info(String.format("Ignored revision %s with no changes in java files", commitId));
			refactoringsAtRevision = Collections.emptyList();
		}
		handler.handle(commitId, refactoringsAtRevision);
		return refactoringsAtRevision;
	}

	protected List<Refactoring> detectRefactorings(final RefactoringHandler handler, File projectFolder, String cloneURL, String currentCommitId) {
		List<Refactoring> refactoringsAtRevision = Collections.emptyList();
		try {
			RepositoryService repositoryService = null;
			try {
				Properties prop = new Properties();
				InputStream input = new FileInputStream("github-credentials.properties");
				prop.load(input);
				String username = prop.getProperty("username");
				String password = prop.getProperty("password");
				GitHubClient client = new GitHubClient();
				client.setCredentials(username, password);
				repositoryService = new RepositoryService(client);
			}
			catch(FileNotFoundException fnfe) {
				repositoryService = new RepositoryService();
			}
			RepositoryId repoId = RepositoryId.createFromUrl(cloneURL);
			String repoName = repoId.getName().endsWith(".git") ? repoId.getName().substring(0, repoId.getName().length()-4) : repoId.getName();
			IRepositoryIdProvider repository = repositoryService.getRepository(repoId.getOwner(), repoName);
			CommitService commitService = new CommitService();
			RepositoryCommit commit = commitService.getCommit(repository, currentCommitId);
			Commit parentCommit = commit.getParents().get(0);
			String parentCommitId = parentCommit.getSha();
			List<CommitFile> commitFiles = commit.getFiles();
			List<String> filesBefore = new ArrayList<String>();
			List<String> filesCurrent = new ArrayList<String>();
			Map<String, String> renamedFilesHint = new HashMap<String, String>();
			for (CommitFile commitFile : commitFiles) {
				if (commitFile.getFilename().endsWith(".java")) {
					if (commitFile.getStatus().equals("modified")) {
						filesBefore.add(commitFile.getFilename());
						filesCurrent.add(commitFile.getFilename());
					}
					else if (commitFile.getStatus().equals("added")) {
						filesCurrent.add(commitFile.getFilename());
					}
					else if (commitFile.getStatus().equals("removed")) {
						filesBefore.add(commitFile.getFilename());
					}
					else if (commitFile.getStatus().equals("renamed")) {
						//TODO
					}
				}
			}
			File currentFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + currentCommitId);
			File parentFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + parentCommitId);
			if (currentFolder.exists() && parentFolder.exists()) {
				UMLModel currentUMLModel = createModel(currentFolder, filesCurrent);
				UMLModel parentUMLModel = createModel(parentFolder, filesBefore);
				// Diff between currentModel e parentModel
				refactoringsAtRevision = parentUMLModel.diff(currentUMLModel, renamedFilesHint).getRefactorings();
				refactoringsAtRevision = filter(refactoringsAtRevision);
			}
			else {
				logger.warn(String.format("Folder %s not found", currentFolder.getPath()));
			}
		} catch (IOException e) {
			logger.warn(String.format("Ignored revision %s due to error", currentCommitId), e);
			handler.handleException(currentCommitId, e);
		} catch (Exception e) {
			
		}
		handler.handle(currentCommitId, refactoringsAtRevision);
		return refactoringsAtRevision;
	}

	protected List<Refactoring> filter(List<Refactoring> refactoringsAtRevision) {
		if (this.refactoringTypesToConsider == null) {
			return refactoringsAtRevision;
		}
		List<Refactoring> filteredList = new ArrayList<Refactoring>();
		for (Refactoring ref : refactoringsAtRevision) {
			if (this.refactoringTypesToConsider.contains(ref.getRefactoringType())) {
				filteredList.add(ref);
			}
		}
		return filteredList;
	}
	
	@Override
	public void detectAll(Repository repository, String branch, final RefactoringHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};
		RevWalk walk = gitService.createAllRevsWalk(repository, branch);
		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

	@Override
	public void fetchAndDetectNew(Repository repository, final RefactoringHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};
		RevWalk walk = gitService.fetchAndCreateNewRevsWalk(repository);
		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

	protected UMLModel createModel(File projectFolder, List<String> files) throws Exception {
		return new UMLModelASTReader(projectFolder, files).getUmlModel();
	}

	@Override
	public void detectAtCommit(Repository repository, String cloneURL, String commitId, RefactoringHandler handler) {
		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		GitService gitService = new GitServiceImpl();
		RevWalk walk = new RevWalk(repository);
		try {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			walk.parseCommit(commit.getParent(0));
			this.detectRefactorings(gitService, repository, handler, projectFolder, commit);
		} catch (MissingObjectException moe) {
			this.detectRefactorings(handler, projectFolder, cloneURL, commitId);
		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", commitId), e);
			handler.handleException(commitId, e);
		} finally {
			walk.dispose();
		}
	}

	@Override
	public String getConfigId() {
	    return "RM1";
	}
}
