/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.ref.Cleaner;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.Feign;
import feign.FeignException;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.hc5.ApacheHttp5Client;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Builder
public class HawkbitClient {

    private static final String AUTHORIZATION = "Authorization";
    // @formatter:off
    public static final BiFunction<Tenant, Controller, RequestInterceptor> DEFAULT_REQUEST_INTERCEPTOR_FN =
            (tenant, controller) -> controller == null
                    ? template ->
                        template.header(
                                AUTHORIZATION,
                                "Basic " + Base64.getEncoder().encodeToString(
                                        (Objects.requireNonNull(tenant.getUsername(), "User is null!") +
                                                ":" +
                                                Objects.requireNonNull(tenant.getPassword(), "Password is not available!"))
                                        .getBytes(StandardCharsets.ISO_8859_1)))
                    : template -> {
                        if (!ObjectUtils.isEmpty(tenant.getGatewayToken())) {
                            template.header(AUTHORIZATION, "GatewayToken " + tenant.getGatewayToken());
                        } else if (!ObjectUtils.isEmpty(controller.getSecurityToken())) {
                            template.header(AUTHORIZATION, "TargetToken " + controller.getSecurityToken());
                        } // else do not send authentication, no authentication or certificate based
                    };
    // @formatter:on
    private static final ErrorDecoder DEFAULT_ERROR_DECODER_0 = new ErrorDecoder.Default();
    public static final ErrorDecoder DEFAULT_ERROR_DECODER = (methodKey, response) -> {
        final Exception e = DEFAULT_ERROR_DECODER_0.decode(methodKey, response);
        log.trace("REST API call failed!", e);
        return e;
    };

    private static final HttpRequestRetryStrategy DEFAULT_HTTP_REQUEST_RETRY_STRATEGY =
            new DefaultHttpRequestRetryStrategy(
                    Integer.getInteger("hawkbit.sdk.http.maxRetry", 3),
                    TimeValue.ofSeconds(Integer.getInteger("hawkbit.sdk.http.defaultRetryIntervalSec", 10)));
    private static final int BUFFER_SIZE = 8096;

    private final HawkbitServer hawkBitServer;

    private final Encoder encoder;
    private final Decoder decoder;
    private final Contract contract;

    private final ErrorDecoder errorDecoder;
    private final BiFunction<Tenant, Controller, RequestInterceptor> requestInterceptorFn;

    private final HttpRequestRetryStrategy httpRequestRetryStrategy;

    public HawkbitClient(final HawkbitServer hawkBitServer, final Encoder encoder, final Decoder decoder, final Contract contract) {
        this(hawkBitServer, encoder, decoder, contract, null, null);
    }

    public HawkbitClient(
            final HawkbitServer hawkBitServer, final Encoder encoder, final Decoder decoder, final Contract contract,
            final ErrorDecoder errorDecoder, final BiFunction<Tenant, Controller, RequestInterceptor> requestInterceptorFn) {
        this(hawkBitServer, encoder, decoder, contract, errorDecoder, requestInterceptorFn, null);
    }

    public HawkbitClient(
            final HawkbitServer hawkBitServer, final Encoder encoder, final Decoder decoder, final Contract contract,
            final ErrorDecoder errorDecoder, final BiFunction<Tenant, Controller, RequestInterceptor> requestInterceptorFn,
            final HttpRequestRetryStrategy httpRequestRetryStrategy) {
        this.hawkBitServer = hawkBitServer;
        this.encoder = encoder;
        this.decoder = decoder;
        this.contract = contract;

        this.errorDecoder = errorDecoder == null ? DEFAULT_ERROR_DECODER : errorDecoder;
        this.requestInterceptorFn = requestInterceptorFn == null ? DEFAULT_REQUEST_INTERCEPTOR_FN : requestInterceptorFn;

        this.httpRequestRetryStrategy = httpRequestRetryStrategy == null ? DEFAULT_HTTP_REQUEST_RETRY_STRATEGY : httpRequestRetryStrategy;
    }

    public <T> T mgmtService(final Class<T> serviceType, final Tenant tenantProperties) {
        return service(serviceType, tenantProperties, null);
    }

    public <T> T ddiService(final Class<T> serviceType, final Tenant tenantProperties, final Controller controller) {
        return service(serviceType, tenantProperties, controller);
    }

    /**
     * Downloads a link. After the handler is called, the steam and all resources are closed.
     */
    @SuppressWarnings("unchecked")
    public static <T, R> R getLink(
            final Link link, final Class<T> linkType, final Tenant tenant, final Controller controller,
            final Function<T, R> handler) throws IOException {
        final String url = link.getHref();
        final HttpClientKey key = new HttpClientKey(
                url.startsWith("https://"), controller == null ? null : controller.getCertificate(), tenant.getTenantCA());
        final HttpClient httpClient = httpClient(key);
        try {
            final HttpGet request = new HttpGet(url);
            final String gatewayToken = tenant.getGatewayToken();
            if (StringUtils.hasLength(gatewayToken)) {
                request.addHeader(HttpHeaders.AUTHORIZATION, "GatewayToken " + gatewayToken);
            } else {
                final String targetToken = controller == null ? null : controller.getSecurityToken();
                if (StringUtils.hasLength(targetToken)) {
                    request.addHeader(HttpHeaders.AUTHORIZATION, "TargetToken " + targetToken);
                }
            } // else not authenticated or certificate based

            return httpClient.execute(request, response -> {
                if (response.getCode() != HttpStatus.OK.value()) {
                    throw new IllegalStateException("Unexpected status code: " + response.getCode());
                }

                final T result;
                if (linkType.isAssignableFrom(ClassicHttpResponse.class)) {
                    result = (T) response;
                } else if (linkType == InputStream.class) {
                    result = (T) response.getEntity().getContent();
                } else {
                    result = new ObjectMapper().readValue(response.getEntity().getContent(), linkType);
                }

                return handler.apply(result);
            });
        } finally {
            key.release();
        }
    }

    private <T> T service(final Class<T> serviceType, final Tenant tenant, final Controller controller) {
        final T service = service0(serviceType, tenant, controller);
        if (serviceType.isInterface() // proxy only interfaces
                && Stream.of(serviceType.getDeclaredMethods()) // and has MultipartFile argument
                .anyMatch(method -> method.getAnnotation(PostMapping.class) != null
                        && List.of(method.getParameterTypes()).contains(MultipartFile.class))) {
            // doesn't use feign client since it doesn't (?) support streaming - loading all in memory which could lead to OOM
            // https://github.com/OpenFeign/feign-form/issues/121 (?)
            return proxy(serviceType, service, tenant, controller);
        } else { // default
            return service;
        }
    }

    private static final Cleaner CLEANER = Cleaner.create();

    private <T> T service0(final Class<T> serviceType, final Tenant tenant, final Controller controller) {
        final String url = controller == null ? hawkBitServer.getMgmtUrl() : hawkBitServer.getDdiUrl();
        final HttpClientKey key = new HttpClientKey(
                url.startsWith("https://"), controller == null ? null : controller.getCertificate(), tenant.getTenantCA());
        final HttpClient httpClient = httpClient(key);
        final T service = Feign.builder()
                .client(new ApacheHttp5Client(httpClient))
                .encoder(encoder)
                .decoder(decoder)
                .errorDecoder(errorDecoder)
                .contract(contract)
                .requestInterceptor(requestInterceptorFn.apply(tenant, controller))
                .target(serviceType, url);
        CLEANER.register(service, key::release);
        return service;
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(final Class<T> serviceType, final T service, final Tenant tenant, final Controller controller) {
        final ObjectMapper objectMapper = new ObjectMapper();
        return (T) Proxy.newProxyInstance(service.getClass().getClassLoader(), new Class<?>[] { serviceType }, (proxy, method, args) -> {
            try {
                final Class<?>[] parameterTypes = method.getParameterTypes();
                if (method.getDeclaringClass() == serviceType && List.of(parameterTypes).contains(MultipartFile.class)) {
                    return callMultipartFormDataRequest(method, args, tenant, controller, parameterTypes, objectMapper);
                } else {
                    return method.invoke(service, args);
                }
            } catch (final InvocationTargetException e) {
                throw e.getTargetException() == null ? e : e.getTargetException();
            }
        });
    }

    private Object callMultipartFormDataRequest(
            final Method method, final Object[] args,
            final Tenant tenant, final Controller controller,
            final Class<?>[] parameterTypes, final ObjectMapper objectMapper) throws URISyntaxException, IOException {
        final PostMapping postMapping = method.getAnnotation(PostMapping.class);
        final Annotation[][] parametersAnnotations = method.getParameterAnnotations();
        // build path - replace @PathVariables
        String path = postMapping.value()[0];
        for (int i = 0; i < args.length; i++) {
            final PathVariable pathVariable = getAnnotation(PathVariable.class, parametersAnnotations[i]);
            if (pathVariable != null) {
                path = path.replace("{" + pathVariable.value() + "}", args[i].toString());
            }
        }

        final HttpURLConnection conn = (HttpURLConnection) new URI(
                (controller == null ? hawkBitServer.getMgmtUrl() : hawkBitServer.getDdiUrl()) + path).toURL().openConnection();
        conn.setRequestMethod("POST");

        // deal with authentication - only from headers1
        final RequestTemplate requestTemplate = new RequestTemplate();
        requestInterceptorFn.apply(tenant, controller).apply(requestTemplate);
        requestTemplate.headers().forEach((k, v) -> v.forEach(e -> conn.setRequestProperty(k, e)));

        final String boundary = UUID.randomUUID().toString().replace("-", "");
        conn.setRequestProperty("content-type", "multipart/form-data;boundary=" + boundary);
        // consumes what the method produces
        final String[] consumes = postMapping.produces();
        if (!ObjectUtils.isEmpty(consumes)) {
            conn.setRequestProperty("accept", String.join(",", consumes));
        }

        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setChunkedStreamingMode(BUFFER_SIZE);

        try (final OutputStream out = new BufferedOutputStream(conn.getOutputStream())) {
            for (int i = 0; i < args.length; i++) {
                final Class<?> type = parameterTypes[i];
                if (MultipartFile.class.isAssignableFrom(type)) {
                    final MultipartFile multipartFile = (MultipartFile) args[i];
                    if (multipartFile != null) {
                        writeMultipartFile(multipartFile, out, boundary, parametersAnnotations[i]);
                    }
                } else {
                    writeSimpleFormData(args[i], out, boundary, parametersAnnotations[i]);
                }
            }
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }

        final int responseCode = conn.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw toFeignException(responseCode, conn, Request.create(
                    Request.HttpMethod.POST, conn.getURL().toString(),
                    conn.getHeaderFields().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                    (Request.Body) null, null));
        }

        return method.getReturnType() == ResponseEntity.class
                ? new ResponseEntity<>(
                    deserialize(
                            conn.getInputStream(),
                            (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0],
                            objectMapper),
                    HttpStatusCode.valueOf(responseCode))
                : deserialize(conn.getInputStream(), method.getReturnType(), objectMapper);
    }

    private static FeignException toFeignException(final int responseCode, final HttpURLConnection conn, final Request request)
            throws IOException {
        if (responseCode >= 500 && responseCode < 600) {
            // server error
            return switch (responseCode) {
                case 500 -> new FeignException.InternalServerError(conn.getResponseMessage(), request, null, null);
                case 501 -> new FeignException.NotImplemented(conn.getResponseMessage(), request, null, null);
                case 502 -> new FeignException.BadGateway(conn.getResponseMessage(), request, null, null);
                case 503 -> new FeignException.ServiceUnavailable(conn.getResponseMessage(), request, null, null);
                case 504 -> new FeignException.GatewayTimeout(conn.getResponseMessage(), request, null, null);
                default -> new FeignException.FeignServerException(responseCode, conn.getResponseMessage(), request, null, null);
            };
        } else {
            return switch (responseCode) {
                case 400 -> new FeignException.BadRequest(conn.getResponseMessage(), request, null, null);
                case 401 -> new FeignException.Unauthorized(conn.getResponseMessage(), request, null, null);
                case 403 -> new FeignException.Forbidden(conn.getResponseMessage(), request, null, null);
                case 404 -> new FeignException.NotFound(conn.getResponseMessage(), request, null, null);
                case 405 -> new FeignException.MethodNotAllowed(conn.getResponseMessage(), request, null, null);
                case 406 -> new FeignException.NotAcceptable(conn.getResponseMessage(), request, null, null);
                case 409 -> new FeignException.Conflict(conn.getResponseMessage(), request, null, null);
                case 410 -> new FeignException.Gone(conn.getResponseMessage(), request, null, null);
                case 415 -> new FeignException.UnsupportedMediaType(conn.getResponseMessage(), request, null, null);
                case 429 -> new FeignException.TooManyRequests(conn.getResponseMessage(), request, null, null);
                case 422 -> new FeignException.UnprocessableEntity(conn.getResponseMessage(), request, null, null);
                default -> new FeignException.FeignClientException(responseCode, conn.getResponseMessage(), request, null, null);
            };
        }
    }

    private static Object deserialize(final InputStream is, final Class<?> type, final ObjectMapper objectMapper) throws IOException {
        return type == void.class || type == Void.class ? null : objectMapper.readValue(is, type);
    }

    private static final String CRLF = "\r\n";

    private void writeMultipartFile(
            final MultipartFile multipartFile, final OutputStream out, final String boundary, final Annotation[] parametersAnnotations)
            throws IOException {
        final String name = Objects.requireNonNull(
                        getAnnotation(RequestPart.class, parametersAnnotations), "MultipartFile shall have RequestPart annotation")
                .value();
        try (final InputStream in = multipartFile.getInputStream()) {
            out.write(("--" + boundary + CRLF +
                    "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + multipartFile.getName() + "\"\r\n" +
                    "Content-Type: " + multipartFile.getContentType() + "\r\n\r\n"
            ).getBytes(StandardCharsets.UTF_8));
            final byte[] buff = new byte[BUFFER_SIZE];
            for (int read; (read = in.read(buff)) != -1; ) {
                out.write(buff, 0, read);
            }
            out.write(CRLF.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeSimpleFormData(
            final Object arg, final OutputStream out, final String boundary, final Annotation[] parameterAnnotations) throws IOException {
        if (arg != null) {
            final RequestParam requestParam = getAnnotation(RequestParam.class, parameterAnnotations);
            if (requestParam != null) {
                out.write(("--" + boundary + CRLF +
                        "Content-Disposition: form-data; name=\"" + requestParam.value() + "\"\r\n\r\n" +
                        arg + CRLF
                ).getBytes(StandardCharsets.UTF_8));
            }
        } // otherwise default
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation> T getAnnotation(final Class<T> annotationClass, final Annotation[] annotations) {
        for (final Annotation annotation : annotations) {
            if (annotation.annotationType().equals(annotationClass)) {
                return (T) annotation;
            }
        }
        return null;
    }

    private static final Map<HttpClientKey, HttpClientWrapper> HTTP_CLIENTS = new HashMap<>();

    private static HttpClient httpClient(final HttpClientKey key) {
        synchronized (HTTP_CLIENTS) {
            final HttpClientWrapper httpClientWrapper = HTTP_CLIENTS.get(key);
            HttpClient client = httpClientWrapper == null ? null : httpClientWrapper.get();
            if (client == null) { // create
                final HttpClientBuilder builder = HttpClients.custom().setRetryStrategy(DEFAULT_HTTP_REQUEST_RETRY_STRATEGY);

                if (key.isHttps()) {
                    // mTLS could be used / setup
                    try {
                        builder.setConnectionManager(
                                PoolingHttpClientConnectionManagerBuilder.create()
                                        .setTlsSocketStrategy(getTlsSocketStrategy(key.getClientCertificate(), key.getServerCertificates()))
                                        .build());
                    } catch (final RuntimeException e) {
                        throw e;
                    } catch (final Exception e) {
                        throw new IllegalStateException("Failed to create mTLS client", e);
                    }
                }

                final CloseableHttpClient newClient = builder.build();
                HTTP_CLIENTS.put(key, new HttpClientWrapper(key, newClient));
                return newClient;
            } else {
                return client; // reuse
            }
        }
    }

    private static final Random SECURE_RND = new SecureRandom();

    private static TlsSocketStrategy getTlsSocketStrategy(final Certificate clientCertificate, final X509Certificate[] serverCertificates)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException, CertificateException,
            IOException {
        final SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
        if (clientCertificate != null) {
            final byte[] bytes = new byte[16];
            SECURE_RND.nextBytes(bytes);
            final String keystorePassword = Base64.getEncoder().encodeToString(bytes);
            sslContextBuilder.loadKeyMaterial(clientCertificate.toKeyStore(keystorePassword), keystorePassword.toCharArray());
        }
        if (serverCertificates == null) {
            // trust all
            sslContextBuilder.loadTrustMaterial(null, new TrustAllStrategy());
        } else {
            final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            for (int i = 0; i < serverCertificates.length; i++) {
                trustStore.setEntry("alias" + i, new KeyStore.TrustedCertificateEntry(serverCertificates[i]), null);
            }
            sslContextBuilder.loadTrustMaterial(trustStore, null);
        }
        return new DefaultClientTlsStrategy(sslContextBuilder.build());
    }

    @AllArgsConstructor
    @Data
    private static class HttpClientKey {

        private final boolean https;
        private final Certificate clientCertificate;
        private final X509Certificate[] serverCertificates;

        private void release() {
            synchronized (HTTP_CLIENTS) {
                HTTP_CLIENTS.get(this).release();
            }
        }
    }

    private static class HttpClientWrapper {

        private final HttpClientKey key;
        private final CloseableHttpClient closeableHttpClient;
        private final AtomicLong pointers = new AtomicLong(1); // one use at create

        private HttpClientWrapper(final HttpClientKey key, final CloseableHttpClient closeableHttpClient) {
            this.key = key;
            this.closeableHttpClient = closeableHttpClient;
        }

        private HttpClient get() {
            pointers.incrementAndGet();
            return closeableHttpClient;
        }

        private void release() {
            if (pointers.decrementAndGet() <= 0) {
                synchronized (HTTP_CLIENTS) {
                    HTTP_CLIENTS.remove(key);
                }
                try {
                    closeableHttpClient.close();
                } catch (final IOException e) {
                    log.error("Failed to close http client", e);
                }
            }
        }
    }
}