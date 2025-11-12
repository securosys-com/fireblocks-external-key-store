// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Service
@Slf4j
@RequiredArgsConstructor
public class InfoService {

    private static final String VENDOR = "Securosys SA";

    private static final String TITLE = "Fireblocks Securosys Custom Server";

    /**
     * Searches all MANIFEST.MF files on the classpath to find the one of our fireblocks-securosys-custom-server component.
     * Then retrieves the version number and returns it.
     *
     * @return a map containing the version number of the deployed package
     */
    public Map<String, String> getVersion() {

        Enumeration<URL> resources;

        try {
            resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
        }
        catch (IOException e) {
            throw new BusinessException("Error retrieving version number.", BusinessReason.ERROR_IMPLEMENTATION, e);
        }

        Map<String, String> versionInfo = new HashMap<>();
        while (resources.hasMoreElements()) { //NOSONAR
            URL url = resources.nextElement();
            try {
                Manifest manifest = new Manifest(url.openStream());

                Attributes currentAttributes = manifest.getMainAttributes();
                String vendor = currentAttributes.getValue("Implementation-Vendor");
                String version = currentAttributes.getValue("Implementation-Version");
                //LOGGER.debug("Vendor: {}, Title: {}, Version: {}", vendor, title, version);
                if ((null == vendor) || (null == version)) { // NOSONAR
                    // it's not our manifest
                    continue;
                }
                if (vendor.equals(VENDOR)) { // NOSONAR
                    versionInfo.put("API", TITLE);
                    versionInfo.put("Version", version);
                    versionInfo.put("Vendor", vendor);
                    break;
                }
            }
            catch (IOException e) {
                throw new BusinessException("Error retrieving version number.", BusinessReason.ERROR_IO, e);
            }
        }
        return versionInfo;
    }


    public List<String> fetchLogs() {
        File logFile = new File("/etc/app/output/service.log");
        if (!logFile.exists()) {
            log.warn("Log file not found: {}", logFile.getAbsolutePath());
            return List.of("Log file not found.");
        }

        List<String> logs = new ArrayList<>();
        int maxLines = 500;
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            ArrayList<String> allLines = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                allLines.add(line);
            }

            int fromIndex = Math.max(allLines.size() - maxLines, 0);
            logs.addAll(allLines.subList(fromIndex, allLines.size()));
        } catch (IOException e) {
            log.error("Error reading log file", e);
            logs.add("Error reading log file: " + e.getMessage());
            throw new BusinessException("Error retrieving logs.", BusinessReason.ERROR_IO, e);
        }
        return logs;
    }
}
