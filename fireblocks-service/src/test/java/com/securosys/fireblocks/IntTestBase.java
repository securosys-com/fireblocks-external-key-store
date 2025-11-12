// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks;


import com.securosys.fireblocks.configuration.CustomServerProperties;
import com.securosys.fireblocks.datamodel.configuration.DataSourceConfiguration;
import io.restassured.RestAssured;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ComponentScan
@SpringBootTest(classes = IntTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DataSourceConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles(value = "test", inheritProfiles = false)
public abstract class IntTestBase {

	@LocalServerPort
	protected int port;

    @Mock
    protected HttpServletRequest request;

    @Autowired
    public CustomServerProperties properties;

	@BeforeAll
	void configureRestAssured() {
        MockitoAnnotations.openMocks(this);
        RestAssured.baseURI = "http://127.0.0.1:" + port + "/v1";
		RestAssured.port = port;

        RestAssured.requestSpecification = RestAssured.given()
                .header("Authorization", properties.getFireblocksAgentConfiguration().getApiAuthorization());
	}

}
