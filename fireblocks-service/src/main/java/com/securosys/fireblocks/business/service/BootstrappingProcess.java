// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public abstract class BootstrappingProcess implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(BootstrappingProcess.class);

	protected void bootstrapCommon() {
		LOGGER.info("Executing application bootstrapping");
	}

}
