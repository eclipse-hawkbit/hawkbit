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
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
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
 * JMH benchmark for the artifact range-request streaming hot path.
 *
 * Reproduces the exact sequence used by FileStreamingUtil.copyStreams when handling a single-range request:
 *   1. Open new InputStream from local FS artifact (BufferedInputStream(FileInputStream)) wrapped in ArtifactStream
 *   2. IOUtils.skipFully(stream, offset)
 *   3. Read partLen bytes through the same 8KB buffer used in production
 *
 * Run only when -Dperf=true is set on the JVM. By default the JUnit launcher is skipped so regular
 * test runs stay fast.
 *
 *   mvn -pl hawkbit-rest/hawkbit-rest-core test -Dtest=FileStreamingBenchmark -Dperf=true
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 3)
@Measurement(iterations = 3, time = 5)
@Fork(1)
public class FileStreamingBenchmark {

    private static final long FILE_SIZE = 800L * 1024 * 1024; // 800MB - matches production artifact size
    private static final int RANGE_LEN = 1024 * 1024; // 1MB payload per range request
    private static final int BUFFER_SIZE = 0x2000; // matches FileStreamingUtil.BUFFER_SIZE (8KB)

    // Offsets the device might request from inside an 800MB artifact.
    @Param({ "0", "104857600", "419430400", "629145600" }) // 0, 100MB, 400MB, 600MB
    private long offset;

    private File file;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        // Write to project target/ (real disk) instead of /tmp which is tmpfs/RAM on many Linux distros
        // and would let the kernel satisfy reads from page cache, hiding the read-and-discard cost.
        final Path dir = Paths.get(System.getProperty("perf.dir", "target/perf-artifacts"));
        Files.createDirectories(dir);
        file = Files.createTempFile(dir, "artifact-", ".bin").toFile();
        // Write real bytes (not sparse) so reads actually traverse data — sparse files would let the kernel
        // satisfy reads from the zero page, hiding the read-and-discard cost we want to measure.
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            final byte[] chunk = new byte[1 << 20]; // 1MB
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
    public long legacy_skipFully_single() throws IOException {
        return readRangeLegacy();
    }

    @Benchmark
    @Threads(8)
    public long legacy_skipFully_parallel() throws IOException {
        return readRangeLegacy();
    }

    @Benchmark
    @Threads(1)
    public long fixed_skipNBytes_single() throws IOException {
        return readRangeFixed();
    }

    @Benchmark
    @Threads(8)
    public long fixed_skipNBytes_parallel() throws IOException {
        return readRangeFixed();
    }

    /** Pre-fix path — IOUtils.skipFully reads and discards bytes through a 2KB scratch buffer. */
    private long readRangeLegacy() throws IOException {
        try (ArtifactStream stream = new ArtifactStream(
                new BufferedInputStream(new FileInputStream(file)), FILE_SIZE, "sha1-bench")) {
            IOUtils.skipFully(stream, offset);
            return readPayload(stream);
        }
    }

    /** Post-fix path — InputStream.skipNBytes uses skip() which lseeks on FileInputStream. */
    private long readRangeFixed() throws IOException {
        try (ArtifactStream stream = new ArtifactStream(
                new BufferedInputStream(new FileInputStream(file)), FILE_SIZE, "sha1-bench")) {
            stream.skipNBytes(offset);
            return readPayload(stream);
        }
    }

    private long readPayload(final ArtifactStream stream) throws IOException {
        final byte[] buf = new byte[BUFFER_SIZE];
        long total = 0;
        long toRead = RANGE_LEN;
        while (toRead > 0) {
            final int r = stream.read(buf, 0, (int) Math.min(BUFFER_SIZE, toRead));
            if (r < 0) {
                break;
            }
            toRead -= r;
            total += r;
        }
        return total;
    }

    @Test
    @EnabledIfSystemProperty(named = "perf", matches = "true")
    void runBenchmark() throws Exception {
        final Options opt = new OptionsBuilder()
                .include(FileStreamingBenchmark.class.getSimpleName())
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.TEXT)
                .result(System.getProperty("perf.out", "target/jmh-streaming.txt"))
                .build();
        new Runner(opt).run();
    }
}
