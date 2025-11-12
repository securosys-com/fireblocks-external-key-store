// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.util;

import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class ConfigUtil {

	private final ResourceLoader resourceLoader;

	public ConfigUtil(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public File readFile(String location) {
		try {
			Resource resource = resourceLoader.getResource(location);
			if(resource.exists()) {
				return resource.getFile();
			}
			else {
				String msg = String.format("Could not load file from location '%s' as it does not exist", location);
				throw new BusinessException(msg, BusinessReason.ERROR_INVALID_CONFIG_INPUT);
			}

		}
		catch (IOException e) {
			String msg = String.format("Could not load file from location '%s'", location);
			throw new BusinessException(msg, BusinessReason.ERROR_INVALID_CONFIG_INPUT, e);
		}
	}

}
