/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package net.grinder;

import net.grinder.engine.communication.UpdateAgentGrinderMessage;
import net.grinder.util.NetworkUtil;
import net.grinder.util.VersionNumber;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.ngrinder.common.util.CompressionUtil;
import org.ngrinder.infra.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Agent Update Message Handler.
 *
 * @author JunHo Yoon
 * @since 3.1
 */
public class AgentUpdateHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(AgentUpdateHandler.class);

	private final AgentConfig agentConfig;

	/**
	 * Agent Update handler.
	 *
	 * @param agentConfig agentConfig
	 */
	public AgentUpdateHandler(AgentConfig agentConfig) {
		LOGGER.info("AgentUpdateHandler is initialing !");
		this.agentConfig = agentConfig;
	}

	boolean isNewer(String newVersion, String installedVersion) {
		installedVersion = installedVersion.replaceAll("\\(.*\\)", "").trim();
		newVersion = newVersion.replaceAll("\\(.*\\)", "").trim();
		return new VersionNumber(newVersion).compareTo(new VersionNumber(installedVersion)) > 0;
	}

	/**
	 * Update agent based on the current message.
	 *
	 * @param message message to be sent
	 */
	public void updateAgent(UpdateAgentGrinderMessage message) {
		if (!isNewer(message.getVersion(), agentConfig.getInternalProperty("ngrinder.version", "UNKNOWN"))) {
			LOGGER.info("Update request was sent. But the old version was sent");
			return;
		}

		File tempFolder = agentConfig.getHome().getTempDirectory();
		File dest = new File(tempFolder, message.getFileName() + ".tar.gz");

		File interDir = new File(agentConfig.getCurrentDirectory(), "update_package_unzip");
		File updatePackageDir = new File(agentConfig.getCurrentDirectory(), "update_package");
		try {
			NetworkUtil.downloadFile(message.getDownloadUrl(), dest);
			decompress(dest, interDir, updatePackageDir);
			System.exit(10);
		} catch (Exception e) {
			LOGGER.error("Update request was sent. But download was failed {} ", e.getMessage());
			LOGGER.info("Details : ", e);
		}
	}

	void decompress(File from, File interDir, File toDir) {
		interDir.mkdirs();
		toDir.mkdirs();

		if (FilenameUtils.isExtension(from.getName(), "gz")) {
			File outFile = new File(toDir, "ngrinder-agent.tar");
			CompressionUtil.ungzip(from, outFile);
			CompressionUtil.untar(outFile, interDir);
			FileUtils.deleteQuietly(outFile);
		} else {
			LOGGER.error("{} is not allowed to be unzipped.", from.getName());
		}

		try {
			FileUtils.deleteQuietly(toDir);
			final File[] files = interDir.listFiles();
			FileUtils.moveDirectory(files[0], toDir);
		} catch (IOException e) {
			LOGGER.error("Error while moving a file ", e);
		}

		FileUtils.deleteQuietly(from);
		FileUtils.deleteQuietly(interDir);

	}
}
