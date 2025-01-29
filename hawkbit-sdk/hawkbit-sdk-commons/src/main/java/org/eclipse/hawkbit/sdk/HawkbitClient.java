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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Builder
public class HawkbitClient {

    private static final String AUTHORIZATION = "Authorization";
    public static final BiFunction<Tenant, Controller, RequestInterceptor> DEFAULT_REQUEST_INTERCEPTOR_FN =
            (tenant, controller) ->
                    controller == null
                            ? template ->
                                    template.header(
                                            AUTHORIZATION,

                                            "Basic " +
                                                    Base64.getEncoder()
                                                            .encodeToString(
                                                                    (Objects.requireNonNull(tenant.getUsername(), "User is null!") +
                                                                            ":" +
                                                                            Objects.requireNonNull(tenant.getPassword(),
                                                                                    "Password is not available!"))
                                                                            .getBytes(StandardCharsets.ISO_8859_1)))
                            :
                                    template -> {
                                        if (ObjectUtils.isEmpty(tenant.getGatewayToken())) {
                                            if (!ObjectUtils.isEmpty(controller.getSecurityToken())) {
                                                template.header(AUTHORIZATION, "TargetToken " + controller.getSecurityToken());
                                            } // else do not sent authentication
                                        } else {
                                            template.header(AUTHORIZATION, "GatewayToken " + tenant.getGatewayToken());
                                        }
                                    };
    private static final ErrorDecoder DEFAULT_ERROR_DECODER_0 = new ErrorDecoder.Default();
    public static final ErrorDecoder DEFAULT_ERROR_DECODER = (methodKey, response) -> {
        final Exception e = DEFAULT_ERROR_DECODER_0.decode(methodKey, response);
        log.trace("REST API call failed!", e);
        return e;
    };
    private final HawkbitServer hawkBitServer;

    private final Client client;
    private final Encoder encoder;
    private final Decoder decoder;
    private final Contract contract;

    private final ErrorDecoder errorDecoder;
    private final BiFunction<Tenant, Controller, RequestInterceptor> requestInterceptorFn;

    public HawkbitClient(
            final HawkbitServer hawkBitServer,
            final Client client, final Encoder encoder, final Decoder decoder, final Contract contract) {
        this(hawkBitServer, client, encoder, decoder, contract, null, null);
    }

    /**
     * Customizers gets default ones and could
     */
    public HawkbitClient(
            final HawkbitServer hawkBitServer,
            final Client client, final Encoder encoder, final Decoder decoder, final Contract contract,
            final ErrorDecoder errorDecoder,
            final BiFunction<Tenant, Controller, RequestInterceptor> requestInterceptorFn) {
        this.hawkBitServer = hawkBitServer;
        this.client = client;
        this.encoder = encoder;
        this.decoder = decoder;
        this.contract = contract;

        this.errorDecoder = errorDecoder == null ? DEFAULT_ERROR_DECODER : errorDecoder;
        this.requestInterceptorFn = requestInterceptorFn == null ? DEFAULT_REQUEST_INTERCEPTOR_FN : requestInterceptorFn;
    }

    public <T> T mgmtService(final Class<T> serviceType, final Tenant tenantProperties) {
        return service(serviceType, tenantProperties, null);
    }

    public <T> T ddiService(final Class<T> serviceType, final Tenant tenantProperties, final Controller controller) {
        return service(serviceType, tenantProperties, controller);
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

    @SuppressWarnings("unchecked")
    private <T> T proxy(final Class<T> serviceType, final T service, final Tenant tenant, final Controller controller) {
        final ObjectMapper objectMapper = new ObjectMapper();
        return (T) Proxy.newProxyInstance(service.getClass().getClassLoader(), new Class<?>[] { serviceType }, (proxy, method, args) -> {
            try {
                final Class<?>[] parameterTypes = method.getParameterTypes();
                if (method.getDeclaringClass() == serviceType && List.of(parameterTypes).contains(MultipartFile.class)) {
                    return processMultipartFormData(method, args, tenant, controller, parameterTypes, objectMapper);
                } else {
                    return method.invoke(service, args);
                }
            } catch (final InvocationTargetException e) {
                throw e.getTargetException() == null ? e : e.getTargetException();
            }
        });
    }

    private static final String CRLF = "\r\n";

    private Object processMultipartFormData(
            final Method method, final Object[] args,
            final Tenant tenant, final Controller controller,
            final Class<?>[] parameterTypes, final ObjectMapper objectMapper) throws IOException {
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

        final HttpURLConnection conn = (HttpURLConnection) new URL(
                (controller == null ? hawkBitServer.getMgmtUrl() : hawkBitServer.getDdiUrl()) + path).openConnection();
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
        return method.getReturnType() == ResponseEntity.class
                ? new ResponseEntity<Object>(
                    objectMapper.readValue(
                            conn.getInputStream(),
                            (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0]),
                    HttpStatusCode.valueOf(conn.getResponseCode()))
                : objectMapper.readValue(conn.getInputStream(), method.getReturnType());
    }

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
            final byte[] buff = new byte[8096];
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
    private <T extends Annotation> T getAnnotation(final Class<T> annotationClass, final Annotation[] annotations) {
        for (final Annotation annotation : annotations) {
            if (annotation.annotationType().equals(annotationClass)) {
                return (T) annotation;
            }
        }
        return null;
    }

    private <T> T service0(final Class<T> serviceType, final Tenant tenant, final Controller controller) {
        return Feign.builder().client(client)
                .encoder(encoder)
                .decoder(decoder)
                .errorDecoder(errorDecoder)
                .contract(contract)
                .requestInterceptor(requestInterceptorFn.apply(tenant, controller))
                .target(serviceType, controller == null ? hawkBitServer.getMgmtUrl() : hawkBitServer.getDdiUrl());
    }
}