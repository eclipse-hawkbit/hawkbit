/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.artifact.model.ArtifactStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Sweep of the streaming buffer size used by FileStreamingUtil.copyStreams.
 *
 * After the seek fix, skip() is O(1) and no longer dominates. The remaining hot loop is
 *
 *     while (toRead > 0) { read(buf, 0, BUFFER_SIZE); to.write(...); }
 *
 * For 800MB/100MB artifacts each read/write is a syscall, so buffer size scales how many
 * syscalls (and TCP segments) we issue per request. Tomcat default for response.setBufferSize
 * is 8KB; we sweep up to 1MB.
 *
 *   mvn -pl hawkbit-rest/hawkbit-rest-core test -Dtest=BufferSizeBenchmark -Dperf=true
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 3)
@Measurement(iterations = 3, time = 5)
@Fork(1)
public class BufferSizeBenchmark {

    private static final long FILE_SIZE = 800L * 1024 * 1024;

    @Param({ "8192", "65536", "262144", "1048576" }) // 8KB, 64KB, 256KB, 1MB
    private int bufferSize;

    // Payload sizes representative of a small range (1MB) and a full-file read (100MB chunk)
    @Param({ "1048576", "104857600" })
    private long payloadSize;

    private File file;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        final Path dir = Paths.get(System.getProperty("perf.dir", "target/perf-artifacts"));
        Files.createDirectories(dir);
        file = Files.createTempFile(dir, "buf-artifact-", ".bin").toFile();
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            final byte[] chunk = new byte[1 << 20];
            for (int i = 0; i < chunk.length; i++) {
                chunk[i] = (byte) i;
            }
            long written = 0;
            while (written < FILE_SIZE) {
                raf.write(chunk);
                written += chunk.length;
            }
            raf.getFD().sync();
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (file != null) {
            file.delete();
        }
    }

    @Benchmark
    @Threads(1)
    public long stream_single() throws IOException {
        return streamPayload();
    }

    @Benchmark
    @Threads(8)
    public long stream_parallel() throws IOException {
        return streamPayload();
    }

    /**
     * Mirrors the FileStreamingUtil.copyStreams hot loop: read from artifact, write to a sink.
     * The sink is a counting OutputStream so we don't measure JVM nullification — every byte
     * is touched, simulating Tomcat's OutputBuffer copying into kernel send buffer.
     */
    private long streamPayload() throws IOException {
        long total = 0;
        try (ArtifactStream stream = new ArtifactStream(
                new BufferedInputStream(new FileInputStream(file)), FILE_SIZE, "sha1-bench");
             CountingSink sink = new CountingSink()) {
            final byte[] buf = new byte[bufferSize];
            long toRead = payloadSize;
            while (toRead > 0) {
                final int r = stream.read(buf, 0, (int) Math.min(bufferSize, toRead));
                if (r < 0) {
                    break;
                }
                sink.write(buf, 0, r);
                toRead -= r;
                total += r;
            }
        }
        return total;
    }

    private static final class CountingSink extends OutputStream {

        long count;

        @Override
        public void write(final int b) {
            count++;
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            // Touch every byte so the JIT cannot dead-code the read.
            int sum = 0;
            for (int i = off; i < off + len; i++) {
                sum += b[i];
            }
            count += len + (sum & 0); // sum kept live without affecting count
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "perf", matches = "true")
    void runBenchmark() throws Exception {
        final Options opt = new OptionsBuilder()
                .include(BufferSizeBenchmark.class.getSimpleName())
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.TEXT)
                .result(System.getProperty("perf.out", "target/jmh-bufsize.txt"))
                .build();
        new Runner(opt).run();
    }
}
