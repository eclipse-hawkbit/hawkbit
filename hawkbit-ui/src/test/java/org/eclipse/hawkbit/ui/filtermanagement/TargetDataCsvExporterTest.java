/**
 * Copyright (c) 2020 devolo AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Feature("Unit Tests - Management UI")
@Story("Target Data CSV Exporter")
@RunWith(MockitoJUnitRunner.class)
public class TargetDataCsvExporterTest {

	@Test
	public void verifyTargetDataCsvExporter() {
		TargetManagement targetManagement = Mockito.mock(TargetManagement.class);

		// Create targets
		JpaTarget targetInPage1 = new JpaTarget("0123", null);

		targetInPage1.setName("Target1");
		targetInPage1.setDescription("Target1: Plug and Play");
		targetInPage1.setAddress("192.168.1.1");
		targetInPage1.setUpdateStatus(TargetUpdateStatus.UNKNOWN);
		targetInPage1.setCreatedBy("John Doe");

		JpaTarget targetInPage2 = new JpaTarget("4567", null);

		targetInPage2.setName("Target2");
		targetInPage2.setDescription("Target2: Plug and Play");
		targetInPage2.setAddress("192.168.1.2");
		targetInPage2.setUpdateStatus(TargetUpdateStatus.IN_SYNC);
		targetInPage2.setCreatedBy("Jane Doe");

		// Have targetManagement return these targets as pages
		when(targetManagement.findByRsql(any(), anyString()))
				.thenReturn(new PageImpl<>(Collections.singletonList(targetInPage1), PageRequest.of(0, 1), 2))
				.thenReturn(new PageImpl<>(Collections.singletonList(targetInPage2), PageRequest.of(1, 1), 2));

		// Get output as ByteArray
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		String dateFormatPattern = "E MMM dd HH:mm:ss z yyyy";

		TargetDataCsvExporter exporter = new TargetDataCsvExporter(targetManagement, ";", dateFormatPattern);

		final int nEntriesPerRow = exporter.getNumberOfColumns();
		exporter.writeTargetsByFilterQueryString("name==target*", outputStream);

		String exporterOutput = outputStream.toString();

		String lineSeparator = System.getProperty("line.separator");
		String[] outputStrings = exporterOutput.replace("\"", "").split(";|" + lineSeparator);

		assertThat(outputStrings[nEntriesPerRow]).isEqualTo(targetInPage1.getControllerId());
		assertThat(outputStrings[nEntriesPerRow * 2]).isEqualTo(targetInPage2.getControllerId());

		assertThat(outputStrings[nEntriesPerRow + 2]).isEqualTo(targetInPage1.getUpdateStatus().toString());

		SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
		assertThat(outputStrings[nEntriesPerRow + 4])
				.isEqualTo(dateFormat.format(new Date(targetInPage1.getCreatedAt())));

		assertThat(outputStrings[nEntriesPerRow * 2 + 3]).isEqualTo(targetInPage2.getCreatedBy());
	}
}
