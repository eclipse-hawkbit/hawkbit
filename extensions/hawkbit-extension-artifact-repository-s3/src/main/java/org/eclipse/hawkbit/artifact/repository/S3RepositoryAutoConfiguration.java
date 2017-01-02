/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * The Spring auto-configuration to register the necessary beans for the S3
 * artifact repository implementation.
 */
@Configuration
@ConditionalOnProperty(prefix = "org.eclipse.hawkbit.artifact.repository.s3", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(S3RepositoryProperties.class)
public class S3RepositoryAutoConfiguration {

    /**
     * The {@link DefaultAWSCredentialsProviderChain} looks for credentials in
     * this order:
     * 
     * <pre>
     * 1. Environment Variables (AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY)
     * 2. Java System Properties (aws.accessKeyId and aws.secretKey)
     * 3. The default credential profiles file (~/.aws/credentials)
     * 4. Amazon ECS container credentials
     * 5. Instance profile credentials
     * </pre>
     * 
     * @return the {@link DefaultAWSCredentialsProviderChain} if no other
     *         {@link AWSCredentialsProvider} bean is registered.
     */
    @Bean
    @ConditionalOnMissingBean
    public AWSCredentialsProvider awsCredentialsProvider() {
        return new DefaultAWSCredentialsProviderChain();
    }

    /**
     * The default AmazonS3 client configuration, which declares the
     * configuration for managing connection behavior to s3.
     * 
     * @return the default {@link ClientConfiguration} bean with the default
     *         client configuration
     */
    @Bean
    @ConditionalOnMissingBean
    public ClientConfiguration awsClientConfiguration() {
        return new ClientConfiguration();
    }

    /**
     * @return the {@link AmazonS3Client} if no other {@link AmazonS3} bean is
     *         registered.
     */
    @Bean
    @ConditionalOnMissingBean
    public AmazonS3 amazonS3() {
        return new AmazonS3Client(awsCredentialsProvider(), awsClientConfiguration());
    }

    /**
     * @return AWS S3 repository {@link ArtifactRepository} implementation.
     */
    @Bean
    public ArtifactRepository artifactRepository(final S3RepositoryProperties s3Properties) {
        return new S3Repository(amazonS3(), s3Properties);
    }
}
